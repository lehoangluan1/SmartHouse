import time
import math
import random
import json
import gc
import requests

# =========================================================
# VIRTUAL SMART HOUSE SIMULATOR + PIR + YOLO HUMAN MOTION
# THROUGH SECURE GATEWAY
# =========================================================

GATEWAY_HOST = "10.128.6.109"
GATEWAY_PORT = 9000
GATEWAY_BASE = f"http://{GATEWAY_HOST}:{GATEWAY_PORT}"

DEVICE_TOKEN = "ohstem-demo-token"
COMMON_HEADERS = {
    "X-Device-Token": DEVICE_TOKEN,
    "Content-Type": "application/json",
}

HOME_ID = 1
DEVICE_NAME = "Virtual OhStem Living Room"

HTTP_TIMEOUT = 5
YOLO_TIMEOUT = 2

# ===== CONFIG =====
CFG = {
    "Thigh": 30.0,
    "Tlow": 27.0,
    "Lhigh": 55,
    "Llow": 35,
    "Tsleep_high": 32.0,
    "Tsleep_low": 26.0,
    "Taway_high": 33.0,
    "Tcritical": 35.0,
    "N_minutes": 2,
    "M_minutes": 2,
    "Thold_minutes": 5,
    "auto_fan_speed": 70,
    "sleep_fan_speed": 30,
    "away_fan_speed": 60,
}

# ===== STATE =====
SYS = {
    "mode": "away",
    "prev_mode": None,
    "hold_until": None,
    "fan_status": "off",
    "fan_speed": 0,
    "light_status": "off",
    "sensor_error": False,
    "state_error": False,
    "config_error": False,
    "telemetry_error": False,
    "command_error": False,
    "registry_error": False,
    "alert_active": False,
    "security_alert_active": False,
    "nhiet_do": 0.0,
    "do_am": 0.0,
    "shine": 0,
    "someone": False,
    "pir_motion": False,
    "camera_human_detected": False,
    "camera_human_count": 0,
    "camera_confidence": 0.0,
    "camera_motion_detected": False,
    "camera_motion_score": 0.0,
    "yolo_error": False,
    "door_locked": True,
    "door_open": False,
    "door_open_until": None,
    "failed_attempts": 0,
    "last_security_alert_ms": 0,
    "manual_override": False,
    "last_gc": 0,
    "last_wrong_password_alert_ms": 0,
    "boot_ms": 0,
    "last_human_seen_ms": 0,
    "last_fan_hw": None,
    "last_light_hw": None,
    "last_door_hw": None,
}

# ===== DEVICE REGISTRY =====
DEV = {
    "runtime": None,
    "fan": None,
    "light": None,
    "temp": None,
    "humidity": None,
    "light_sensor": None,
    "motion": None,
}

KEYS = {
    "runtime_id": None,
    "runtime_key": "yolobit-01",
    "fan_id": None,
    "fan_key": "ohstem-fan-ctrl-01",
    "light_id": None,
    "light_key": "ohstem-light-ctrl-01",
    "temp_key": "ohstem-temp-01",
    "humidity_key": "ohstem-humidity-01",
    "light_sensor_key": "ohstem-light-01",
    "motion_key": "ohstem-motion-01",
}

DEVICE_RULES = {
    "runtime": {
        "exact": ["yolobit-01"],
        "type": ["SENSOR_NODE", "HUB", "OTHER"],
        "key": ["yolobit", "controller", "hub"],
        "name": ["controller", "hub", "trung tam"],
    },
    "fan": {
        "exact": ["ohstem-fan-ctrl-01"],
        "type": ["FAN"],
        "key": ["fan", "quat"],
        "name": ["fan", "quạt"],
    },
    "light": {
        "exact": ["ohstem-light-ctrl-01"],
        "type": ["LIGHT"],
        "key": ["light", "den"],
        "name": ["light", "đèn"],
    },
    "temp": {
        "exact": ["ohstem-temp-01"],
        "key": ["temp", "temperature"],
        "name": ["temp", "temperature"],
    },
    "humidity": {
        "exact": ["ohstem-humidity-01"],
        "key": ["humidity", "do-am"],
        "name": ["humidity", "độ ẩm"],
    },
    "light_sensor": {
        "exact": ["ohstem-light-01"],
    },
    "motion": {
        "exact": ["ohstem-motion-01"],
        "key": ["motion", "pir"],
        "name": ["motion", "chuyển động"],
    },
}


# =========================================================
# UTILS
# =========================================================
def now_ms():
    return int(time.time() * 1000)


def clamp(v, a, b):
    return a if v < a else b if v > b else v


def to_int(v, d=0):
    try:
        return int(v)
    except:
        try:
            return int(float(v))
        except:
            return d


def to_float(v, d=0.0):
    try:
        return float(v)
    except:
        return d


def norm(v):
    return "" if v is None else str(v).strip().lower()


def norm_type(v):
    return "" if v is None else str(v).strip().upper()


def contains(text, keys):
    t = norm(text)
    for k in keys:
        if norm(k) in t:
            return True
    return False


def has_server_error():
    return (
        SYS["state_error"]
        or SYS["config_error"]
        or SYS["telemetry_error"]
        or SYS["command_error"]
        or SYS["registry_error"]
    )


def unwrap(x):
    return x.get("data") if isinstance(x, dict) and "data" in x else x


def gateway_get(path, timeout=HTTP_TIMEOUT):
    try:
        resp = requests.get(
            GATEWAY_BASE + path,
            headers=COMMON_HEADERS,
            timeout=timeout,
        )
        if 200 <= resp.status_code < 300:
            try:
                return unwrap(resp.json())
            except:
                return None
        print(f"[GATEWAY GET FAIL] {path}: {resp.status_code} {resp.text}")
        return None
    except Exception as e:
        print(f"[GATEWAY GET FAIL] {path}: {e}")
        return None


def gateway_post(path, payload, timeout=HTTP_TIMEOUT):
    try:
        resp = requests.post(
            GATEWAY_BASE + path,
            json=payload,
            headers=COMMON_HEADERS,
            timeout=timeout,
        )
        ok = 200 <= resp.status_code < 300
        text = resp.text
        if not ok:
            print(f"[GATEWAY POST FAIL] {path}: {resp.status_code} {text}")
        return ok, text
    except Exception as e:
        print(f"[GATEWAY POST FAIL] {path}: {e}")
        return False, None


# =========================================================
# VIRTUAL HARDWARE
# =========================================================
def batquat(on, speed):
    current = ("on" if on else "off", speed if on else 0)
    if current != SYS.get("last_fan_hw"):
        print(f"[VIRTUAL FAN] {'ON' if on else 'OFF'} speed={speed if on else 0}")
        SYS["last_fan_hw"] = current


def batden(on):
    current = "on" if on else "off"
    if current != SYS.get("last_light_hw"):
        print(f"[VIRTUAL LIGHT] {'ON' if on else 'OFF'}")
        SYS["last_light_hw"] = current


def set_door_relay(is_open):
    current = bool(is_open)
    if current != SYS.get("last_door_hw"):
        print(f"[VIRTUAL DOOR RELAY] {'OPEN' if is_open else 'CLOSE'}")
        SYS["last_door_hw"] = current


def open_door():
    SYS["door_open"] = True
    SYS["door_locked"] = False
    SYS["door_open_until"] = now_ms() + 5000
    print("[VIRTUAL DOOR] OPEN")


def close_door():
    SYS["door_open"] = False
    SYS["door_locked"] = True
    SYS["door_open_until"] = None
    print("[VIRTUAL DOOR] CLOSE")


def update_door_auto_close():
    if SYS["door_open"] and SYS["door_open_until"] is not None:
        if now_ms() >= SYS["door_open_until"]:
            close_door()


# =========================================================
# REGISTRY
# =========================================================
def device_match(d, rule):
    if d is None or rule is None:
        return False
    if "type" in rule and norm_type(d.get("type")) in rule["type"]:
        return True
    if "key" in rule and contains(d.get("deviceKey"), rule["key"]):
        return True
    if "name" in rule and contains(d.get("name"), rule["name"]):
        return True
    return False


def find_exact(devices, keys):
    keys = [norm(k) for k in keys]
    for d in devices:
        if norm(d.get("deviceKey")) in keys:
            return d
    return None


def find_by_rule(devices, name):
    rule = DEVICE_RULES.get(name, {})
    d = find_exact(devices, rule.get("exact", []))
    if d is not None:
        return d
    for x in devices:
        if device_match(x, rule):
            if name == "light_sensor" and norm(x.get("deviceKey")) == "ohstem-light-01":
                return x
            if name != "light_sensor":
                return x
    return devices[0] if name == "runtime" and devices else None


def load_device_registry():
    data = gateway_get(f"/gw/devices/home/{HOME_ID}")
    if not isinstance(data, list) or not data:
        print("[REGISTRY] Không load được từ gateway/server, dùng key local")
        SYS["registry_error"] = True
        return False

    DEV["runtime"] = find_by_rule(data, "runtime")
    DEV["fan"] = find_by_rule(data, "fan")
    DEV["light"] = find_by_rule(data, "light")
    DEV["temp"] = find_by_rule(data, "temp")
    DEV["humidity"] = find_by_rule(data, "humidity")
    DEV["light_sensor"] = find_by_rule(data, "light_sensor")
    DEV["motion"] = find_by_rule(data, "motion")

    if DEV["runtime"]:
        KEYS["runtime_id"] = DEV["runtime"].get("id")
        KEYS["runtime_key"] = DEV["runtime"].get("deviceKey") or KEYS["runtime_key"]
    if DEV["fan"]:
        KEYS["fan_id"] = DEV["fan"].get("id")
        KEYS["fan_key"] = DEV["fan"].get("deviceKey") or KEYS["fan_key"]
    if DEV["light"]:
        KEYS["light_id"] = DEV["light"].get("id")
        KEYS["light_key"] = DEV["light"].get("deviceKey") or KEYS["light_key"]
    if DEV["temp"]:
        KEYS["temp_key"] = DEV["temp"].get("deviceKey") or KEYS["temp_key"]
    if DEV["humidity"]:
        KEYS["humidity_key"] = DEV["humidity"].get("deviceKey") or KEYS["humidity_key"]
    if DEV["light_sensor"]:
        KEYS["light_sensor_key"] = DEV["light_sensor"].get("deviceKey") or KEYS["light_sensor_key"]
    if DEV["motion"]:
        KEYS["motion_key"] = DEV["motion"].get("deviceKey") or KEYS["motion_key"]

    SYS["registry_error"] = False
    print("[REGISTRY] Loaded")
    print(json.dumps(KEYS, indent=2, ensure_ascii=False))
    return True


# =========================================================
# ALERT
# =========================================================
def send_security_alert(reason, detail):
    payload = {
        "deviceId": KEYS["runtime_id"],
        "sensorId": None,
        "type": reason,
        "message": detail,
    }
    success, _ = gateway_post(f"/gw/homes/{HOME_ID}/alerts", payload)
    print(f"[ALERT] {reason} | {detail} | success={success}")
    return success


def trigger_wrong_password_alert_periodic():
    current = now_ms()
    if current - SYS["last_wrong_password_alert_ms"] >= 5000:
        SYS["last_wrong_password_alert_ms"] = current
        SYS["failed_attempts"] += 1
        SYS["security_alert_active"] = True
        SYS["last_security_alert_ms"] = current
        send_security_alert(
            "WRONG_PASSWORD",
            f"[VIRTUAL TEST] Nhập sai mật khẩu mô phỏng lần {SYS['failed_attempts']}",
        )


def clear_security_alert_if_needed():
    if SYS["security_alert_active"] and (now_ms() - SYS["last_security_alert_ms"] >= 15000):
        SYS["security_alert_active"] = False


def send_temperature_alert_if_needed():
    if SYS["nhiet_do"] is not None and SYS["nhiet_do"] > CFG["Tcritical"]:
        send_security_alert(
            "HIGH_TEMPERATURE",
            f"[VIRTUAL TEST] Nhiệt độ cao: {round(SYS['nhiet_do'], 1)}°C",
        )


# =========================================================
# SENSOR SIMULATION
# =========================================================
def read_sensor_virtual():
    elapsed = (now_ms() - SYS["boot_ms"]) / 1000.0

    temp = 31 + 5 * math.sin(elapsed / 8.0) + random.uniform(-0.5, 0.5)
    hum = 65 + 12 * math.sin(elapsed / 11.0 + 1.2) + random.uniform(-1.0, 1.0)
    light = 50 + 40 * math.sin(elapsed / 6.0 + 0.4)

    pir_motion = (int(elapsed) % 12) in [2, 3, 8, 9]

    SYS["nhiet_do"] = round(clamp(temp, 20, 45), 1)
    SYS["do_am"] = round(clamp(hum, 30, 95), 1)
    SYS["shine"] = int(clamp(light, 0, 100))
    SYS["pir_motion"] = bool(pir_motion)


def update_camera_from_yolo():
    data = gateway_get("/gw/yolo/check_human", timeout=YOLO_TIMEOUT)

    if not isinstance(data, dict) or data.get("status") != "success":
        SYS["yolo_error"] = True
        SYS["camera_human_detected"] = False
        SYS["camera_human_count"] = 0
        SYS["camera_confidence"] = 0.0
        SYS["camera_motion_detected"] = False
        SYS["camera_motion_score"] = 0.0
        return

    SYS["yolo_error"] = False
    SYS["camera_human_detected"] = bool(data.get("human_detected", False))
    SYS["camera_human_count"] = to_int(data.get("human_count", 0), 0)
    SYS["camera_confidence"] = to_float(data.get("max_confidence", 0.0), 0.0)
    SYS["camera_motion_detected"] = bool(data.get("motion_detected", False))
    SYS["camera_motion_score"] = to_float(data.get("movement_score", 0.0), 0.0)


def combine_motion_sources():
    pir_motion = bool(SYS.get("pir_motion", False))
    human_detected = bool(SYS.get("camera_human_detected", False))
    camera_motion = bool(SYS.get("camera_motion_detected", False))
    confidence = to_float(SYS.get("camera_confidence", 0.0), 0.0)

    active = camera_motion or (pir_motion and human_detected and confidence >= 0.5)

    if active:
        SYS["someone"] = True
        SYS["last_human_seen_ms"] = now_ms()
    else:
        if now_ms() - SYS.get("last_human_seen_ms", 0) > 3000:
            SYS["someone"] = False


def sensor_is_valid():
    t, h, l = SYS["nhiet_do"], SYS["do_am"], SYS["shine"]
    return not (
        t is None or h is None or l is None or
        t < -10 or t > 80 or
        h < 0 or h > 100 or
        l < 0 or l > 100
    )


# =========================================================
# STATE / CONFIG
# =========================================================
def fetch_state_by_device_id(device_id):
    return None if device_id is None else gateway_get(f"/gw/devices/{device_id}/state")


def fetch_device_state():
    if SYS.get("manual_override"):
        return

    ok_all = True

    if KEYS["runtime_id"] is not None:
        s = fetch_state_by_device_id(KEYS["runtime_id"])
        if isinstance(s, dict):
            if s.get("mode") is not None:
                SYS["mode"] = str(s.get("mode")).lower()
            SYS["hold_until"] = s.get("holdUntil")
            SYS["prev_mode"] = s.get("prevMode")
        else:
            ok_all = False

    if KEYS["fan_id"] is not None:
        s = fetch_state_by_device_id(KEYS["fan_id"])
        if isinstance(s, dict):
            if s.get("fanStatus") is not None:
                SYS["fan_status"] = str(s.get("fanStatus")).lower()
            if s.get("fanSpeed") is not None:
                SYS["fan_speed"] = clamp(to_int(s.get("fanSpeed"), SYS["fan_speed"]), 0, 100)
        else:
            ok_all = False

    if KEYS["light_id"] is not None:
        s = fetch_state_by_device_id(KEYS["light_id"])
        if isinstance(s, dict):
            if s.get("lightStatus") is not None:
                SYS["light_status"] = str(s.get("lightStatus")).lower()
        else:
            ok_all = False

    SYS["state_error"] = not ok_all


def fetch_config():
    data = gateway_get(f"/gw/homes/{HOME_ID}/configs")
    if not isinstance(data, dict):
        SYS["config_error"] = True
        return

    map_cfg = {
        "thigh": ("Thigh", to_float),
        "tlow": ("Tlow", to_float),
        "lhigh": ("Lhigh", to_int),
        "llow": ("Llow", to_int),
        "tsleepHigh": ("Tsleep_high", to_float),
        "tsleepLow": ("Tsleep_low", to_float),
        "tawayHigh": ("Taway_high", to_float),
        "tcritical": ("Tcritical", to_float),
        "nMinutes": ("N_minutes", to_int),
        "mMinutes": ("M_minutes", to_int),
        "tholdMinutes": ("Thold_minutes", to_int),
        "autoFanSpeed": ("auto_fan_speed", to_int),
        "sleepFanSpeed": ("sleep_fan_speed", to_int),
        "awayFanSpeed": ("away_fan_speed", to_int),
    }

    for k, rule in map_cfg.items():
        name, caster = rule
        if k in data and data[k] is not None:
            v = caster(data[k], CFG[name])
            CFG[name] = clamp(v, 0, 100) if "Speed" in name else v

    SYS["config_error"] = False
    print("[CONFIG] Updated")


# =========================================================
# MODE LOGIC
# =========================================================
def apply_mode_logic():
    if not sensor_is_valid():
        SYS["sensor_error"] = True
        SYS["alert_active"] = False
        SYS["fan_status"] = "off"
        SYS["fan_speed"] = 0
        SYS["light_status"] = "off"
        return

    SYS["sensor_error"] = False

    if SYS["fan_status"] not in ["on", "off"]:
        SYS["fan_status"] = "off"
    if SYS["light_status"] not in ["on", "off"]:
        SYS["light_status"] = "off"

    SYS["fan_speed"] = clamp(to_int(SYS["fan_speed"], 0), 0, 100)

    if SYS["mode"] != "manual":
        fan_status, fan_speed, light_status = "off", 0, "off"
        t = SYS["nhiet_do"]
        l = SYS["shine"]

        if SYS["mode"] == "sleep":
            if t is not None and t >= CFG["Tsleep_high"]:
                fan_status, fan_speed = "on", CFG["sleep_fan_speed"]
            elif t is not None and t <= CFG["Tsleep_low"]:
                fan_status, fan_speed = "off", 0

        elif SYS["mode"] == "away":
            if t is not None and t >= CFG["Taway_high"]:
                fan_status, fan_speed = "on", CFG["away_fan_speed"]

        else:
            if t is not None and t >= CFG["Thigh"]:
                fan_status, fan_speed = "on", CFG["auto_fan_speed"]
            elif t is not None and t <= CFG["Tlow"]:
                fan_status, fan_speed = "off", 0

            if l is not None and l <= CFG["Llow"]:
                light_status = "on"
            elif l is not None and l >= CFG["Lhigh"]:
                light_status = "off"

        SYS["fan_status"] = fan_status
        SYS["fan_speed"] = fan_speed
        SYS["light_status"] = light_status

    if SYS["fan_status"] == "on" and SYS["fan_speed"] <= 0:
        SYS["fan_speed"] = (
            CFG["sleep_fan_speed"] if SYS["mode"] == "sleep"
            else CFG["away_fan_speed"] if SYS["mode"] == "away"
            else CFG["auto_fan_speed"]
        )

    batquat(SYS["fan_status"] == "on", SYS["fan_speed"] if SYS["fan_status"] == "on" else 0)
    batden(SYS["light_status"] == "on")

    temp_critical = SYS["nhiet_do"] is not None and SYS["nhiet_do"] > CFG["Tcritical"]
    away_motion = SYS["mode"] == "away" and SYS["someone"]
    SYS["alert_active"] = temp_critical or away_motion or SYS["security_alert_active"]


# =========================================================
# TELEMETRY
# =========================================================
TELEMETRY_ITEMS = [
    ("temp_key", "temperature", "nhiet_do"),
    ("humidity_key", "humidity", "do_am"),
    ("light_sensor_key", "light", "shine"),
    ("motion_key", "motion", "someone"),
]


def send_one_telemetry(device_key, sensor_type, value):
    payload = {
        "deviceKey": device_key,
        "sensorType": sensor_type,
        "value": value,
    }
    success, _ = gateway_post("/gw/device-telemetry", payload)
    print(f"[TELEMETRY] {sensor_type}={value} key={device_key} success={success}")
    return success


def send_all_telemetry():
    ok_all = True

    for key_name, sensor_type, sys_key in TELEMETRY_ITEMS:
        device_key = KEYS.get(key_name)
        value = SYS.get(sys_key)

        if sys_key == "someone":
            value = bool(value)

        if device_key is None or value is None:
            ok_all = False
            continue

        success = send_one_telemetry(device_key, sensor_type, value)
        if not success:
            ok_all = False

    SYS["telemetry_error"] = not ok_all
    return ok_all


# =========================================================
# COMMAND
# =========================================================
def normalize_target(t):
    t = norm(t).replace("-", "_")
    return {
        "fanstatus": "fan",
        "fanspeed": "fan_speed",
        "lightstatus": "light",
        "lightlevel": "light_level",
    }.get(t, t)


def fetch_next_command(device_key):
    if device_key is None:
        return None
    data = gateway_get(f"/gw/device/{device_key}/commands/next")
    return data if isinstance(data, dict) and data.get("id") is not None else None


def ack_command(device_key, command_id):
    if device_key is None or command_id is None:
        return False
    success, _ = gateway_post(f"/gw/device/{device_key}/commands/ack", {"id": command_id})
    return success


def process_runtime_command(cmd):
    cid = cmd.get("id")
    target = normalize_target(cmd.get("target"))
    value = cmd.get("value")

    if cid is None:
        return False

    if target == "mode" and value is not None:
        SYS["prev_mode"] = SYS["mode"]
        SYS["mode"] = str(value).lower()
        if SYS["mode"] != "manual":
            SYS["manual_override"] = False

    print(f"[CMD RUNTIME] {cmd}")
    return ack_command(KEYS["runtime_key"], cid)


def process_fan_command(cmd):
    cid = cmd.get("id")
    target = normalize_target(cmd.get("target"))
    value = cmd.get("value")

    if cid is None:
        return False

    SYS["prev_mode"] = SYS["mode"]
    SYS["mode"] = "manual"
    SYS["manual_override"] = True

    if target == "fan":
        if value is not None:
            SYS["fan_status"] = str(value).lower()
            SYS["fan_speed"] = 0 if SYS["fan_status"] == "off" else (50 if SYS["fan_speed"] <= 0 else SYS["fan_speed"])
    elif target == "fan_speed":
        SYS["fan_speed"] = clamp(to_int(value, SYS["fan_speed"]), 0, 100)
        SYS["fan_status"] = "on" if SYS["fan_speed"] > 0 else "off"

    print(f"[CMD FAN] {cmd}")
    return ack_command(KEYS["fan_key"], cid)


def process_light_command(cmd):
    cid = cmd.get("id")
    target = normalize_target(cmd.get("target"))
    value = cmd.get("value")

    if cid is None:
        return False

    SYS["prev_mode"] = SYS["mode"]
    SYS["mode"] = "manual"
    SYS["manual_override"] = True

    if target == "light" and value is not None:
        SYS["light_status"] = str(value).lower()

    print(f"[CMD LIGHT] {cmd}")
    return ack_command(KEYS["light_key"], cid)


def fetch_all_commands():
    ok_all = True

    command_specs = [
        (KEYS["runtime_key"], process_runtime_command),
        (KEYS["fan_key"], process_fan_command),
        (KEYS["light_key"], process_light_command),
    ]

    for key, fn in command_specs:
        if key is None:
            ok_all = False
            continue

        try:
            data = fetch_next_command(key)
            if data is not None:
                fn(data)
        except Exception as e:
            ok_all = False
            print("[COMMAND ERROR]", key, e)

    SYS["command_error"] = not ok_all
    return ok_all


# =========================================================
# DEBUG
# =========================================================
def print_status():
    print(
        "[STATUS]",
        f"mode={SYS['mode']}",
        f"temp={SYS['nhiet_do']}",
        f"hum={SYS['do_am']}",
        f"light={SYS['shine']}",
        f"pir_motion={SYS['pir_motion']}",
        f"cam_human={SYS['camera_human_detected']}",
        f"cam_count={SYS['camera_human_count']}",
        f"cam_conf={round(SYS['camera_confidence'], 2)}",
        f"cam_motion={SYS['camera_motion_detected']}",
        f"cam_score={round(SYS['camera_motion_score'], 2)}",
        f"someone={SYS['someone']}",
        f"fan={SYS['fan_status']}:{SYS['fan_speed']}",
        f"lamp={SYS['light_status']}",
        f"door_open={SYS['door_open']}",
        f"alert={SYS['alert_active']}",
        f"security={SYS['security_alert_active']}",
        f"yolo_error={SYS['yolo_error']}",
    )


# =========================================================
# BOOT
# =========================================================
def boot():
    print("===================================================")
    print(" VIRTUAL SMART HOUSE SIMULATOR + PIR + YOLO START ")
    print("===================================================")
    SYS["boot_ms"] = now_ms()

    close_door()
    load_device_registry()
    fetch_config()
    fetch_device_state()

    health = gateway_get("/gw/yolo/health", timeout=YOLO_TIMEOUT)
    print("[YOLO HEALTH]", health)

    print("[BOOT] Done")


# =========================================================
# MAIN LOOP
# =========================================================
def main():
    boot()

    last = {
        "state": 0,
        "config": 0,
        "telemetry": 0,
        "command": 0,
        "registry": 0,
        "debug": 0,
        "yolo": 0,
        "temp_alert": 0,
    }

    intervals = {
        "registry": 600000,
        "state": 15000,
        "config": 120000,
        "telemetry": 5000,
        "command": 3000,
        "debug": 2000,
        "yolo": 1200,
        "temp_alert": 15000,
    }

    while True:
        now = now_ms()

        if now - SYS.get("last_gc", 0) > 10000:
            gc.collect()
            SYS["last_gc"] = now

        # ===== LOCAL SIMULATION =====
        read_sensor_virtual()

        if now - last["yolo"] >= intervals["yolo"]:
            update_camera_from_yolo()
            last["yolo"] = now

        combine_motion_sources()
        update_door_auto_close()
        trigger_wrong_password_alert_periodic()
        clear_security_alert_if_needed()
        apply_mode_logic()

        if SYS["nhiet_do"] > CFG["Tcritical"] and now - last["temp_alert"] >= intervals["temp_alert"]:
            send_temperature_alert_if_needed()
            last["temp_alert"] = now

        set_door_relay(SYS["door_open"])

        # ===== NETWORK TASKS =====
        did_network = False

        if now - last["command"] >= intervals["command"]:
            try:
                fetch_all_commands()
            except Exception as e:
                SYS["command_error"] = True
                print("[COMMAND ERROR]", e)
            last["command"] = now
            did_network = True

        if now - last["state"] >= intervals["state"]:
            try:
                fetch_device_state()
                SYS["state_error"] = False
            except Exception as e:
                SYS["state_error"] = True
                print("[STATE ERROR]", e)
            last["state"] = now
            did_network = True

        if now - last["telemetry"] >= intervals["telemetry"]:
            try:
                if sensor_is_valid():
                    send_all_telemetry()
                else:
                    SYS["telemetry_error"] = True
            except Exception as e:
                SYS["telemetry_error"] = True
                print("[TELEMETRY ERROR]", e)
            last["telemetry"] = now
            did_network = True

        if now - last["config"] >= intervals["config"]:
            try:
                fetch_config()
                SYS["config_error"] = False
            except Exception as e:
                SYS["config_error"] = True
                print("[CONFIG ERROR]", e)
            last["config"] = now
            did_network = True

        if now - last["registry"] >= intervals["registry"]:
            try:
                load_device_registry()
                SYS["registry_error"] = False
            except Exception as e:
                SYS["registry_error"] = True
                print("[REGISTRY ERROR]", e)
            last["registry"] = now
            did_network = True

        if now - last["debug"] >= intervals["debug"]:
            print_status()
            last["debug"] = now

        time.sleep(0.2 if did_network else 0.5)


if __name__ == "__main__":
    main()