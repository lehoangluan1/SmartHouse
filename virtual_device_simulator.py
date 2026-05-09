import math
import os
import random
import threading
import time
from datetime import datetime
from typing import Any, Dict, Optional

import requests

def load_env_file(path: str):
    if not os.path.exists(path):
        return
    with open(path, "r", encoding="utf-8") as fh:
        for raw_line in fh:
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


load_env_file(".env.local")
load_env_file(".env")

try:
    from aiot_local_config import GATEWAY_HOST, GATEWAY_PORT, HOME_ID as AIOT_HOME_ID, DEVICE_TOKEN
except Exception:
    GATEWAY_HOST = "127.0.0.1"
    GATEWAY_PORT = 9000
    AIOT_HOME_ID = 1
    DEVICE_TOKEN = "ohstem-demo-token"

BASE_URL = os.getenv("GATEWAY_BASE_URL") or f"http://{os.getenv('GATEWAY_CLIENT_HOST', GATEWAY_HOST)}:{os.getenv('GATEWAY_PORT', str(GATEWAY_PORT))}"
BASE_URL = BASE_URL.rstrip("/")
HOME_ID = int(os.getenv("HOME_ID", str(AIOT_HOME_ID)))
HEADERS = {
    "X-Device-Token": os.getenv("GATEWAY_DEVICE_TOKEN", DEVICE_TOKEN),
    "Content-Type": "application/json"
}

# key giống device thật
KEY = {
    "runtime_key": "yolobit-01",
    "fan_key": "ohstem-fan-ctrl-01",
    "light_key": "ohstem-light-ctrl-01",
    "temp_key": "ohstem-temp-01",
    "humidity_key": "ohstem-humidity-01",
    "light_sensor_key": "ohstem-light-01",
    "motion_key": "ohstem-motion-01",
}

# config giống bản OhStem
CFG = {
    "Thigh": 30.0,
    "Tlow": 27.0,
    "Lhigh": 55,
    "Llow": 35,
    "Tsleep_high": 32.0,
    "Tsleep_low": 26.0,
    "Taway_high": 33.0,
    "Tcritical": 35.0,
    "auto_fan_speed": 70,
    "sleep_fan_speed": 30,
    "away_fan_speed": 60,
}

RUN_SECONDS = 300
HTTP_TIMEOUT = 3
TELEMETRY_INTERVAL = 0.25
STATE_POLL_INTERVAL = 0.6
CONFIG_POLL_INTERVAL = 5.0
SPIKE_BURST_INTERVAL = 18.0

session = requests.Session()
lock = threading.Lock()
started_at = time.time()

STATE: Dict[str, Any] = {
    "runtime_id": None,
    "fan_id": None,
    "light_id": None,

    # sensor mô phỏng
    "temperature": None,
    "humidity": None,
    "light": None,
    "motion": 0,

    # state từ server
    "server_mode": "away",
    "server_prev_mode": None,
    "server_hold_until": None,
    "server_fan_status": "off",
    "server_fan_speed": 0,
    "server_light_status": "off",

    # expected tính local theo rule
    "expected_fan_status": "off",
    "expected_fan_speed": 0,
    "expected_light_status": "off",

    "last_state_sync_ok": False,
    "last_config_sync_ok": False,
}

STATS = {
    "telemetry_ok": 0,
    "telemetry_fail": 0,
    "state_ok": 0,
    "state_fail": 0,
    "config_ok": 0,
    "config_fail": 0,
    "mismatch_count": 0,
    "exceptions": 0,
}


def now_str() -> str:
    return datetime.now().strftime("%H:%M:%S.%f")[:-3]


def elapsed() -> float:
    return time.time() - started_at


def clamp(v, a, b):
    return max(a, min(b, v))


def safe_int(v, default=0):
    try:
        return int(v)
    except Exception:
        try:
            return int(float(v))
        except Exception:
            return default


def safe_float(v, default=0.0):
    try:
        return float(v)
    except Exception:
        return default


def norm(v) -> str:
    return "" if v is None else str(v).strip().lower()


def unwrap(payload):
    if isinstance(payload, dict) and "data" in payload:
        return payload["data"]
    return payload


def log(msg: str):
    print(f"[{now_str()}] {msg}")


def get(path: str):
    url = f"{BASE_URL}{path}"
    try:
        print(f"[DEBUG] GET {url}")
        resp = session.get(url, headers=HEADERS, timeout=HTTP_TIMEOUT)
        print(f"[DEBUG] STATUS {resp.status_code}")
        print(f"[DEBUG] BODY {resp.text[:500]}")

        if 200 <= resp.status_code < 300:
            try:
                return unwrap(resp.json())
            except Exception as ex:
                print(f"[DEBUG] JSON parse error: {ex}")
                return None

        return None
    except Exception as ex:
        with lock:
            STATS["exceptions"] += 1
        log(f"GET {path} ex={ex}")
        return None

def post(path: str, data: dict) -> bool:
    url = f"{BASE_URL}{path}"
    try:
        resp = session.post(url, json=data, headers=HEADERS, timeout=HTTP_TIMEOUT)
        ok = 200 <= resp.status_code < 300
        return ok
    except Exception as ex:
        with lock:
            STATS["exceptions"] += 1
        log(f"POST {path} ex={ex}")
        return False


def load_registry():
    ds = get(f"/gw/devices/home/{HOME_ID}")
    if not isinstance(ds, list):
        raise RuntimeError("Cannot load device registry")

    runtime_id = None
    fan_id = None
    light_id = None

    for d in ds:
        key = norm(d.get("deviceKey"))
        if key == KEY["runtime_key"]:
            runtime_id = d.get("id")
        elif key == KEY["fan_key"]:
            fan_id = d.get("id")
        elif key == KEY["light_key"]:
            light_id = d.get("id")

    if not runtime_id or not fan_id or not light_id:
        raise RuntimeError("Missing runtime/fan/light device ids")

    with lock:
        STATE["runtime_id"] = runtime_id
        STATE["fan_id"] = fan_id
        STATE["light_id"] = light_id

    log(f"registry loaded runtime={runtime_id}, fan={fan_id}, light={light_id}")


def load_config():
    data = get(f"/gw/homes/{HOME_ID}/configs")
    if data is None:
        with lock:
            STATS["config_fail"] += 1
            STATE["last_config_sync_ok"] = False
        return

    if isinstance(data, dict) and "configs" in data and isinstance(data["configs"], dict):
        data = data["configs"]

    if isinstance(data, list):
        temp = {}
        for item in data:
            if isinstance(item, dict) and item.get("key") is not None:
                temp[str(item["key"])] = item.get("value")
        data = temp

    if not isinstance(data, dict):
        with lock:
            STATS["config_fail"] += 1
            STATE["last_config_sync_ok"] = False
        return

    mp = {
        "thigh": ("Thigh", safe_float),
        "tlow": ("Tlow", safe_float),
        "lhigh": ("Lhigh", safe_int),
        "llow": ("Llow", safe_int),
        "tsleepHigh": ("Tsleep_high", safe_float),
        "tsleepLow": ("Tsleep_low", safe_float),
        "tawayHigh": ("Taway_high", safe_float),
        "tcritical": ("Tcritical", safe_float),
        "autoFanSpeed": ("auto_fan_speed", safe_int),
        "sleepFanSpeed": ("sleep_fan_speed", safe_int),
        "awayFanSpeed": ("away_fan_speed", safe_int),

        # hỗ trợ key snake_case nếu backend trả kiểu khác
        "tsleep_high": ("Tsleep_high", safe_float),
        "tsleep_low": ("Tsleep_low", safe_float),
        "taway_high": ("Taway_high", safe_float),
        "tcritical": ("Tcritical", safe_float),
        "auto_fan_speed": ("auto_fan_speed", safe_int),
        "sleep_fan_speed": ("sleep_fan_speed", safe_int),
        "away_fan_speed": ("away_fan_speed", safe_int),
    }

    with lock:
        for raw_key, (cfg_key, caster) in mp.items():
            if raw_key in data and data[raw_key] is not None:
                value = caster(data[raw_key], CFG[cfg_key])
                if "speed" in cfg_key.lower():
                    CFG[cfg_key] = clamp(value, 0, 100)
                else:
                    CFG[cfg_key] = value

        STATE["last_config_sync_ok"] = True
        STATS["config_ok"] += 1


def get_state(device_id: Optional[int]):
    if not device_id:
        return None
    return get(f"/gw/devices/{device_id}/state")


def sync_server_state():
    with lock:
        runtime_id = STATE["runtime_id"]
        fan_id = STATE["fan_id"]
        light_id = STATE["light_id"]

    ok1 = ok2 = ok3 = True

    runtime = get_state(runtime_id)
    fan = get_state(fan_id)
    light = get_state(light_id)

    with lock:
        if isinstance(runtime, dict):
            if runtime.get("mode") is not None:
                STATE["server_mode"] = norm(runtime.get("mode"))
            STATE["server_prev_mode"] = runtime.get("prevMode")
            STATE["server_hold_until"] = runtime.get("holdUntil")
        else:
            ok1 = False

        if isinstance(fan, dict):
            if fan.get("fanStatus") is not None:
                STATE["server_fan_status"] = norm(fan.get("fanStatus"))
            if fan.get("fanSpeed") is not None:
                STATE["server_fan_speed"] = clamp(safe_int(fan.get("fanSpeed"), 0), 0, 100)
        else:
            ok2 = False

        if isinstance(light, dict):
            if light.get("lightStatus") is not None:
                STATE["server_light_status"] = norm(light.get("lightStatus"))
        else:
            ok3 = False

        ok_all = ok1 and ok2 and ok3
        STATE["last_state_sync_ok"] = ok_all
        if ok_all:
            STATS["state_ok"] += 1
        else:
            STATS["state_fail"] += 1


def simulated_temperature(t: float) -> float:
    base = 29.0 + 2.6 * math.sin(t / 11.0)
    noise = random.uniform(-0.45, 0.45)
    if int(t) % 37 in (0, 1, 2, 3):
        base += 5.2
    return round(clamp(base + noise, 20.0, 45.0), 2)


def simulated_humidity(t: float) -> float:
    base = 67.0 + 11.0 * math.sin(t / 17.0 + 0.9)
    noise = random.uniform(-1.8, 1.8)
    return round(clamp(base + noise, 35.0, 95.0), 2)


def simulated_light(t: float) -> int:
    base = 52 + 33 * math.sin(t / 19.0)
    noise = random.uniform(-6, 6)
    if int(t) % 41 in (9, 10, 11, 12):
        base -= 28
    return int(clamp(round(base + noise), 0, 100))


def simulated_motion(t: float) -> int:
    phase = int(t) % 30
    if phase in (5, 6, 7, 8, 20, 21, 22):
        return 1
    return 1 if random.random() < 0.04 else 0


def update_sensor_snapshot():
    t = elapsed()
    with lock:
        STATE["temperature"] = simulated_temperature(t)
        STATE["humidity"] = simulated_humidity(t)
        STATE["light"] = simulated_light(t)
        STATE["motion"] = simulated_motion(t)


def expected_from_rules():
    with lock:
        mode = norm(STATE["server_mode"]) or "away"
        t = STATE["temperature"]
        l = STATE["light"]

    fan_status = "off"
    fan_speed = 0
    light_status = "off"

    if t is None or l is None:
        return fan_status, fan_speed, light_status

    if mode == "sleep":
        if t >= CFG["Tsleep_high"]:
            fan_status = "on"
            fan_speed = CFG["sleep_fan_speed"]
        elif t <= CFG["Tsleep_low"]:
            fan_status = "off"
            fan_speed = 0

    elif mode == "away":
        if t >= CFG["Taway_high"]:
            fan_status = "on"
            fan_speed = CFG["away_fan_speed"]

    else:
        if t >= CFG["Thigh"]:
            fan_status = "on"
            fan_speed = CFG["auto_fan_speed"]
        elif t <= CFG["Tlow"]:
            fan_status = "off"
            fan_speed = 0

        if l <= CFG["Llow"]:
            light_status = "on"
        elif l >= CFG["Lhigh"]:
            light_status = "off"

    fan_speed = clamp(safe_int(fan_speed, 0), 0, 100)
    if fan_status == "off":
        fan_speed = 0

    return fan_status, fan_speed, light_status


def send_telemetry(device_key: str, sensor_type: str, value: Any):
    ok = post("/gw/device-telemetry", {
        "deviceKey": device_key,
        "sensorType": sensor_type,
        "value": value
    })

    with lock:
        if ok:
            STATS["telemetry_ok"] += 1
        else:
            STATS["telemetry_fail"] += 1

    log(f"TEL {sensor_type:<12} value={str(value):<6} ok={ok}")


def telemetry_worker():
    next_burst = time.time() + SPIKE_BURST_INTERVAL

    while elapsed() < RUN_SECONDS:
        update_sensor_snapshot()

        with lock:
            temp = STATE["temperature"]
            humidity = STATE["humidity"]
            light = STATE["light"]
            motion = STATE["motion"]

        batch = [
            (KEY["temp_key"], "temperature", temp),
            (KEY["humidity_key"], "humidity", humidity),
            (KEY["light_sensor_key"], "light", light),
            (KEY["motion_key"], "motion", motion),
        ]
        random.shuffle(batch)

        for device_key, sensor_type, value in batch:
            send_telemetry(device_key, sensor_type, value)
            time.sleep(TELEMETRY_INTERVAL)

        # burst để ép transaction cạnh tranh mạnh hơn
        if time.time() >= next_burst:
            log("=== BURST START ===")
            for _ in range(6):
                hot = round(simulated_temperature(elapsed()) + random.uniform(1.2, 2.8), 2)
                dark = int(clamp(simulated_light(elapsed()) - random.randint(10, 30), 0, 100))
                send_telemetry(KEY["temp_key"], "temperature", hot)
                send_telemetry(KEY["light_sensor_key"], "light", dark)
                send_telemetry(KEY["motion_key"], "motion", 1)
                time.sleep(0.04)
            log("=== BURST END ===")
            next_burst = time.time() + SPIKE_BURST_INTERVAL


def state_worker():
    while elapsed() < RUN_SECONDS:
        sync_server_state()
        time.sleep(STATE_POLL_INTERVAL)


def config_worker():
    while elapsed() < RUN_SECONDS:
        load_config()
        time.sleep(CONFIG_POLL_INTERVAL)


def validator_worker():
    while elapsed() < RUN_SECONDS:
        fan_status, fan_speed, light_status = expected_from_rules()

        with lock:
            STATE["expected_fan_status"] = fan_status
            STATE["expected_fan_speed"] = fan_speed
            STATE["expected_light_status"] = light_status

            server_mode = STATE["server_mode"]
            server_fan_status = norm(STATE["server_fan_status"])
            server_fan_speed = clamp(safe_int(STATE["server_fan_speed"], 0), 0, 100)
            server_light_status = norm(STATE["server_light_status"])

            temp = STATE["temperature"]
            hum = STATE["humidity"]
            lig = STATE["light"]
            motion = STATE["motion"]
            last_state_sync_ok = STATE["last_state_sync_ok"]

        if last_state_sync_ok:
            fan_bad = server_fan_status != fan_status
            speed_bad = (fan_status == "on" and server_fan_speed != fan_speed) or (fan_status == "off" and server_fan_speed != 0)
            light_bad = server_light_status != light_status

            if fan_bad or speed_bad or light_bad:
                with lock:
                    STATS["mismatch_count"] += 1

                log(
                    "STATE_MISMATCH "
                    f"mode={server_mode} "
                    f"T={temp} H={hum} L={lig} M={motion} | "
                    f"expected fan={fan_status}/{fan_speed} light={light_status} | "
                    f"server fan={server_fan_status}/{server_fan_speed} light={server_light_status}"
                )
            else:
                log(
                    "STATE_OK "
                    f"mode={server_mode} "
                    f"T={temp} H={hum} L={lig} M={motion} | "
                    f"fan={server_fan_status}/{server_fan_speed} light={server_light_status}"
                )

        time.sleep(0.9)


def summary_worker():
    while elapsed() < RUN_SECONDS:
        with lock:
            log(
                "SUMMARY "
                f"tel_ok={STATS['telemetry_ok']} tel_fail={STATS['telemetry_fail']} "
                f"state_ok={STATS['state_ok']} state_fail={STATS['state_fail']} "
                f"cfg_ok={STATS['config_ok']} cfg_fail={STATS['config_fail']} "
                f"mismatch={STATS['mismatch_count']} ex={STATS['exceptions']}"
            )
        time.sleep(10)


def main():
    load_registry()
    load_config()
    sync_server_state()

    threads = [
        threading.Thread(target=telemetry_worker, daemon=True),
        threading.Thread(target=state_worker, daemon=True),
        threading.Thread(target=config_worker, daemon=True),
        threading.Thread(target=validator_worker, daemon=True),
        threading.Thread(target=summary_worker, daemon=True),
    ]

    for t in threads:
        t.start()

    for t in threads:
        t.join()

    print("\n===== FINAL RESULT =====")
    for k, v in STATS.items():
        print(f"{k}: {v}")


if __name__ == "__main__":
    main()
