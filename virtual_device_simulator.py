import time
import math
import random
import gc
import requests

GATEWAY_HOST = "192.168.1.83"
GATEWAY_PORT = 9000
GATEWAY_BASE = f"http://{GATEWAY_HOST}:{GATEWAY_PORT}"

DEVICE_TOKEN = "ohstem-demo-token"
COMMON_HEADERS = {
    "X-Device-Token": DEVICE_TOKEN,
    "Content-Type": "application/json",
}

HOME_ID = 1
HTTP_TIMEOUT = 5

SYS = {
    "mode": "manual",
    "prev_mode": None,

    "fan_status": "off",
    "fan_speed": 0,
    "light_status": "off",

    "nhiet_do": 0.0,
    "do_am": 0.0,
    "shine": 0,
    "someone": False,

    "state_error": False,
    "config_error": False,
    "telemetry_error": False,
    "registry_error": False,

    "boot_ms": 0,
    "last_gc": 0,
    "last_fan_hw": None,
    "last_light_hw": None,
}

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

LAST = {
    "mode": None,
    "status": None,
    "fan_state": None,
    "light_state": None,
}


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
        return None
    except:
        return None


def gateway_post(path, payload, timeout=HTTP_TIMEOUT):
    try:
        resp = requests.post(
            GATEWAY_BASE + path,
            json=payload,
            headers=COMMON_HEADERS,
            timeout=timeout,
        )
        return 200 <= resp.status_code < 300, resp.text
    except:
        return False, None


def batquat(on, speed):
    current = ("on" if on else "off", speed if on else 0)
    if current != SYS["last_fan_hw"]:
        SYS["last_fan_hw"] = current


def batden(on):
    current = "on" if on else "off"
    if current != SYS["last_light_hw"]:
        SYS["last_light_hw"] = current


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
    return True


def read_sensor_virtual():
    elapsed = (now_ms() - SYS["boot_ms"]) / 1000.0

    # high temperature: khoảng 32 -> 39 độ
    temp = 35 + 3.5 * math.sin(elapsed / 8.0) + random.uniform(-0.6, 0.6)

    # độ ẩm trung bình
    hum = 60 + 10 * math.sin(elapsed / 11.0 + 1.2) + random.uniform(-1.0, 1.0)

    # low light: khoảng 0 -> 20
    light = 10 + 6 * math.sin(elapsed / 7.0 + 0.4) + random.uniform(-2.0, 2.0)

    pir_motion = (int(elapsed) % 12) in [2, 3, 8, 9]

    SYS["nhiet_do"] = round(clamp(temp, 32, 39), 1)
    SYS["do_am"] = round(clamp(hum, 40, 80), 1)
    SYS["shine"] = int(clamp(light, 0, 20))
    SYS["someone"] = bool(pir_motion)


def sensor_is_valid():
    t, h, l = SYS["nhiet_do"], SYS["do_am"], SYS["shine"]
    return not (
        t is None or h is None or l is None or
        t < -10 or t > 80 or
        h < 0 or h > 100 or
        l < 0 or l > 100
    )


def fetch_state_by_device_id(device_id):
    return None if device_id is None else gateway_get(f"/gw/devices/{device_id}/state")


def fetch_device_state():
    ok_all = True

    if KEYS["runtime_id"] is not None:
        s = fetch_state_by_device_id(KEYS["runtime_id"])
        if isinstance(s, dict):
            new_mode = str(s.get("mode", SYS["mode"])).lower()
            if new_mode != SYS["mode"]:
                SYS["prev_mode"] = SYS["mode"]
                SYS["mode"] = new_mode
        else:
            ok_all = False

    if KEYS["fan_id"] is not None:
        s = fetch_state_by_device_id(KEYS["fan_id"])
        if isinstance(s, dict):
            if s.get("fanStatus") is not None:
                SYS["fan_status"] = str(s.get("fanStatus")).lower()
            if s.get("fanSpeed") is not None:
                SYS["fan_speed"] = clamp(to_int(s.get("fanSpeed"), 0), 0, 100)
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
    normalize_actuator_state()
    return ok_all


def normalize_actuator_state():
    SYS["fan_status"] = "on" if SYS["fan_status"] == "on" else "off"
    SYS["light_status"] = "on" if SYS["light_status"] == "on" else "off"
    SYS["fan_speed"] = clamp(to_int(SYS["fan_speed"], 0), 0, 100)
    if SYS["fan_status"] == "off":
        SYS["fan_speed"] = 0


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

        if not send_one_telemetry(device_key, sensor_type, value):
            ok_all = False

    SYS["telemetry_error"] = not ok_all
    return ok_all


def apply_state_to_hardware_and_log():
    prev_fan = LAST["fan_state"]
    prev_light = LAST["light_state"]

    cur_fan = (SYS["fan_status"], SYS["fan_speed"])
    cur_light = SYS["light_status"]

    batquat(SYS["fan_status"] == "on", SYS["fan_speed"])
    batden(SYS["light_status"] == "on")

    if prev_fan is not None and prev_fan != cur_fan:
        if SYS["mode"] != "manual":
            print(
                f"[AUTO-EVENT] mode={SYS['mode']} "
                f"fan {prev_fan[0]}:{prev_fan[1]} -> {cur_fan[0]}:{cur_fan[1]}"
            )
        else:
            print(
                f"[STATE] mode=manual "
                f"fan {prev_fan[0]}:{prev_fan[1]} -> {cur_fan[0]}:{cur_fan[1]}"
            )

    if prev_light is not None and prev_light != cur_light:
        if SYS["mode"] != "manual":
            print(
                f"[AUTO-EVENT] mode={SYS['mode']} "
                f"light {prev_light} -> {cur_light}"
            )
        else:
            print(
                f"[STATE] mode=manual "
                f"light {prev_light} -> {cur_light}"
            )

    LAST["fan_state"] = cur_fan
    LAST["light_state"] = cur_light


def log_mode_if_changed():
    if SYS["mode"] != LAST["mode"]:
        LAST["mode"] = SYS["mode"]
        print(f"[MODE] {SYS['mode']}")


def print_status():
    current = (
        SYS["mode"],
        SYS["nhiet_do"],
        SYS["do_am"],
        SYS["shine"],
        SYS["someone"],
        SYS["fan_status"],
        SYS["fan_speed"],
        SYS["light_status"],
    )

    if current != LAST["status"]:
        LAST["status"] = current
        print(
            f"[STAT] "
            f"mode={SYS['mode']} "
            f"temp={SYS['nhiet_do']}C "
            f"hum={SYS['do_am']}% "
            f"light={SYS['shine']} "
            f"motion={int(bool(SYS['someone']))} "
            f"fan={SYS['fan_status']}:{SYS['fan_speed']} "
            f"lamp={SYS['light_status']}"
        )


def boot():
    SYS["boot_ms"] = now_ms()
    print("[BOOT] device receive-state simulator")

    load_device_registry()
    fetch_device_state()
    apply_state_to_hardware_and_log()
    print_status()


def main():
    boot()

    last = {
        "state": 0,
        "telemetry": 0,
        "debug": 0,
        "registry": 0,
    }

    intervals = {
        "state": 3000,
        "telemetry": 5000,
        "debug": 1000,
        "registry": 600000,
    }

    while True:
        now = now_ms()

        if now - SYS["last_gc"] > 10000:
            gc.collect()
            SYS["last_gc"] = now

        read_sensor_virtual()

        if now - last["state"] >= intervals["state"]:
            fetch_device_state()
            log_mode_if_changed()
            apply_state_to_hardware_and_log()
            last["state"] = now

        if now - last["telemetry"] >= intervals["telemetry"]:
            if sensor_is_valid():
                send_all_telemetry()
            else:
                SYS["telemetry_error"] = True
            last["telemetry"] = now

        if now - last["registry"] >= intervals["registry"]:
            load_device_registry()
            last["registry"] = now

        if now - last["debug"] >= intervals["debug"]:
            print_status()
            last["debug"] = now

        time.sleep(0.3)


if __name__ == "__main__":
    main()