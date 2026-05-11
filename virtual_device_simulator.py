import math
import os
import random
import sys
import time
from datetime import datetime

import requests


def load_env_file(path):
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


BASE_URL = os.getenv("GATEWAY_BASE_URL") or "http://{}:{}".format(
    os.getenv("GATEWAY_CLIENT_HOST", GATEWAY_HOST),
    os.getenv("GATEWAY_PORT", str(GATEWAY_PORT)),
)
BASE_URL = BASE_URL.rstrip("/")
HOME_ID = int(os.getenv("HOME_ID", str(AIOT_HOME_ID)))
HEADERS = {
    "X-Device-Token": os.getenv("GATEWAY_DEVICE_TOKEN", DEVICE_TOKEN),
    "Content-Type": "application/json",
}

KEY = {
    "runtime_key": "yolobit-01",
    "fan_key": "ohstem-fan-ctrl-01",
    "light_key": "ohstem-light-ctrl-01",
    "temp_key": "ohstem-temp-01",
    "humidity_key": "ohstem-humidity-01",
    "light_sensor_key": "ohstem-light-01",
    "motion_key": "ohstem-motion-01",
}

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

DOOR_PASS = os.getenv("SIM_DOOR_PASS", "123456")
DOOR_OPEN_MS = int(os.getenv("SIM_DOOR_OPEN_MS", "5000"))
IR_TIMEOUT_MS = int(os.getenv("SIM_IR_TIMEOUT_MS", "15000"))
IR_LOCKOUT_MS = int(os.getenv("SIM_IR_LOCKOUT_MS", "15000"))
MAX_FAIL = int(os.getenv("SIM_MAX_FAIL", "3"))
HTTP_TIMEOUT = float(os.getenv("SIM_HTTP_TIMEOUT", "1.2"))
RUN_SECONDS = float(os.getenv("SIM_RUN_SECONDS", "0"))

ITV = {
    "telemetry": int(float(os.getenv("SIM_TELEMETRY_INTERVAL", "1.2")) * 1000),
    "command": int(float(os.getenv("SIM_COMMAND_INTERVAL", "0.5")) * 1000),
    "ack": int(float(os.getenv("SIM_ACK_INTERVAL", "0.4")) * 1000),
    "sensor": int(float(os.getenv("SIM_SENSOR_INTERVAL", "1.0")) * 1000),
    "door": 50,
    "summary": 10000,
}
LAST = {k: 0 for k in ITV}
TEL = (
    ("temp_key", "temperature", "temperature"),
    ("humidity_key", "humidity", "humidity"),
    ("light_sensor_key", "light", "light"),
    ("motion_key", "motion", "motion"),
)

STATE = {
    "mode": "away",
    "fan_status": "off",
    "fan_speed": 0,
    "light_status": "off",
    "brightness": 0,
    "temperature": None,
    "humidity": None,
    "light": None,
    "motion": 0,
    "door_locked": 1,
    "door_open": 0,
    "door_open_until": None,
    "ir_state": "idle",
    "ir_pass": "",
    "ir_last_input_ms": 0,
    "failed_attempts": 0,
    "last_ir_key": None,
    "last_ir_ms": 0,
}

STATS = {
    "telemetry_ok": 0,
    "telemetry_fail": 0,
    "command_ok": 0,
    "command_fail": 0,
    "ack_ok": 0,
    "ack_fail": 0,
    "exceptions": 0,
}

session = requests.Session()
started_at = time.monotonic()
tel_i = 0
ACKQ = []
RUNNING = True


def now_ms():
    return int(time.monotonic() * 1000)


def elapsed():
    return time.monotonic() - started_at


def due(name, now):
    return now - LAST[name] >= ITV[name]


def done(name, now):
    LAST[name] = now


def now_str():
    return datetime.now().strftime("%H:%M:%S.%f")[:-3]


def log(msg):
    print("[{}] {}".format(now_str(), msg), flush=True)


def clamp(v, a, b):
    return max(a, min(b, v))


def to_int(v, default=0):
    try:
        return int(v)
    except Exception:
        try:
            return int(float(v))
        except Exception:
            return default


def to_float(v, default=0.0):
    try:
        return float(v)
    except Exception:
        return default


def norm(v):
    return "" if v is None else str(v).strip().lower()


def unwrap(payload):
    if isinstance(payload, dict) and "data" in payload:
        return payload.get("data")
    return payload


def get(path):
    try:
        resp = session.get(BASE_URL + path, headers=HEADERS, timeout=HTTP_TIMEOUT)
        if 200 <= resp.status_code < 300:
            return unwrap(resp.json())
        return None
    except Exception as ex:
        STATS["exceptions"] += 1
        log("GET {} ex={}".format(path, ex))
        return None


def post(path, data):
    try:
        resp = session.post(BASE_URL + path, json=data, headers=HEADERS, timeout=HTTP_TIMEOUT)
        return 200 <= resp.status_code < 300
    except Exception as ex:
        STATS["exceptions"] += 1
        log("POST {} ex={}".format(path, ex))
        return False


def simulated_temperature(t):
    return round(clamp(29.0 + 2.6 * math.sin(t / 11.0) + random.uniform(-0.45, 0.45), 20.0, 45.0), 2)


def simulated_humidity(t):
    return round(clamp(67.0 + 11.0 * math.sin(t / 17.0 + 0.9) + random.uniform(-1.8, 1.8), 35.0, 95.0), 2)


def simulated_light(t):
    return int(clamp(round(52 + 33 * math.sin(t / 19.0) + random.uniform(-6, 6)), 0, 100))


def simulated_motion(t):
    phase = int(t) % 30
    return 1 if phase in (5, 6, 7, 8, 20, 21, 22) or random.random() < 0.04 else 0


def run_sensor_task(now):
    if not due("sensor", now):
        return
    t = elapsed()
    STATE["temperature"] = simulated_temperature(t)
    STATE["humidity"] = simulated_humidity(t)
    STATE["light"] = simulated_light(t)
    STATE["motion"] = simulated_motion(t)
    done("sensor", now)


def send_one_telemetry():
    global tel_i
    for _ in range(len(TEL)):
        key_name, sensor_type, state_name = TEL[tel_i]
        tel_i = (tel_i + 1) % len(TEL)
        value = STATE.get(state_name)
        device_key = KEY.get(key_name)
        if value is None:
            continue
        log("TEL deviceKey={} type={} value={}".format(device_key, sensor_type, value))
        ok = post("/gw/device-telemetry", {
            "deviceKey": device_key,
            "sensorType": sensor_type,
            "value": value,
        })
        if ok:
            STATS["telemetry_ok"] += 1
        else:
            STATS["telemetry_fail"] += 1
        return


def run_telemetry_task(now):
    if due("telemetry", now):
        send_one_telemetry()
        done("telemetry", now)


def normalize_target(value):
    normalized = norm(value).replace("-", "_")
    mapping = {
        "mode": "mode",
        "power": "power",
        "fan": "power",
        "fanstatus": "power",
        "fan_status": "power",
        "fanspeed": "speed",
        "fan_speed": "speed",
        "speed": "speed",
        "light": "power",
        "lightstatus": "power",
        "light_status": "power",
        "brightness": "brightness",
        "lightlevel": "brightness",
        "light_level": "brightness",
    }
    return mapping.get(normalized, normalized)


def norm_switch(value, default="off"):
    normalized = norm(value)
    if normalized in ("on", "1", "true", "yes"):
        return "on"
    if normalized in ("off", "0", "false", "no"):
        return "off"
    return default


def command_id(cmd):
    if not isinstance(cmd, dict):
        return None
    return cmd.get("id") or cmd.get("commandId") or cmd.get("command_id")


def normalize_command(raw):
    item = raw
    if isinstance(item, dict):
        for key in ("data", "command", "result", "payload"):
            inner = item.get(key)
            if isinstance(inner, dict):
                item = inner
                break
    if isinstance(item, list):
        item = item[0] if item else None
    if not isinstance(item, dict):
        return None
    cid = command_id(item)
    target = item.get("target") or item.get("command") or item.get("commandType") or item.get("type") or item.get("field")
    value = item.get("value")
    if value is None:
        value = item.get("commandValue")
    if value is None:
        value = item.get("payload")
    if value is None:
        value = item.get("status")
    if cid is None or target is None:
        return None
    out = dict(item)
    out["id"] = cid
    out["target"] = target
    out["value"] = value
    return out


def queue_ack(device_key, cid):
    if cid is None:
        return
    for existing_key, existing_id in ACKQ:
        if existing_key == device_key and existing_id == cid:
            return
    ACKQ.append((device_key, cid))


def flush_acks():
    if not ACKQ:
        return
    payload = {"acks": [{"deviceKey": key, "id": cid} for key, cid in ACKQ]}
    ok = post("/gw/commands/ack", payload)
    if ok:
        STATS["ack_ok"] += len(ACKQ)
        ACKQ[:] = []
    else:
        STATS["ack_fail"] += 1


def hw_mode(mode):
    log("HW mode {}".format(mode))


def hw_fan():
    if STATE["fan_status"] == "off":
        STATE["fan_speed"] = 0
        log("HW fan off speed=0")
    else:
        if STATE["fan_speed"] <= 0:
            STATE["fan_speed"] = CFG["auto_fan_speed"]
        log("HW fan on speed={}".format(STATE["fan_speed"]))


def hw_light():
    if STATE["light_status"] == "off":
        STATE["brightness"] = 0
        log("HW light off")
    else:
        if STATE["brightness"] <= 0:
            STATE["brightness"] = 100
        log("HW light on brightness={}".format(STATE["brightness"]))


def apply_runtime_command(cmd):
    cid = cmd.get("id")
    target = normalize_target(cmd.get("target"))
    value = cmd.get("value")
    if target == "mode":
        mode = norm(value)
        log("CMD runtime id={} target=mode value={}".format(cid, mode))
        if mode in ("auto", "manual", "sleep", "away"):
            STATE["mode"] = mode
            hw_mode(mode)
    queue_ack(KEY["runtime_key"], cid)


def apply_fan_command(cmd):
    cid = cmd.get("id")
    target = normalize_target(cmd.get("target"))
    value = cmd.get("value")
    if target == "power":
        next_value = norm_switch(value, STATE["fan_status"])
        log("CMD fan id={} target=power value={}".format(cid, next_value))
        STATE["fan_status"] = next_value
        if next_value == "off":
            STATE["fan_speed"] = 0
        hw_fan()
    elif target == "speed":
        speed = clamp(to_int(value, STATE["fan_speed"]), 0, 100)
        log("CMD fan id={} target=speed value={}".format(cid, speed))
        STATE["fan_speed"] = speed
        STATE["fan_status"] = "on" if speed > 0 else "off"
        hw_fan()
    queue_ack(KEY["fan_key"], cid)


def apply_light_command(cmd):
    cid = cmd.get("id")
    target = normalize_target(cmd.get("target"))
    value = cmd.get("value")
    if target == "power":
        next_value = norm_switch(value, STATE["light_status"])
        log("CMD light id={} target=power value={}".format(cid, next_value))
        STATE["light_status"] = next_value
        if next_value == "off":
            STATE["brightness"] = 0
        hw_light()
    elif target == "brightness":
        brightness = clamp(to_int(value, STATE["brightness"]), 0, 100)
        log("CMD light id={} target=brightness value={}".format(cid, brightness))
        STATE["brightness"] = brightness
        STATE["light_status"] = "on" if brightness > 0 else "off"
        hw_light()
    queue_ack(KEY["light_key"], cid)


def run_backend_task(now):
    if due("ack", now):
        flush_acks()
        done("ack", now)

    if not due("command", now):
        return

    keys = "{},{},{}".format(KEY["runtime_key"], KEY["fan_key"], KEY["light_key"])
    data = get("/gw/commands/next?keys={}".format(keys))
    if not isinstance(data, dict):
        STATS["command_fail"] += 1
        done("command", now)
        return

    handlers = (
        (KEY["runtime_key"], apply_runtime_command),
        (KEY["fan_key"], apply_fan_command),
        (KEY["light_key"], apply_light_command),
    )
    for device_key, handler in handlers:
        cmd = normalize_command(data.get(device_key))
        if cmd is None:
            continue
        handler(cmd)
        STATS["command_ok"] += 1

    done("command", now)


def set_ir_state(state):
    STATE["ir_state"] = state
    log("IR state={}".format(state))


def ir_start(now):
    STATE["ir_pass"] = ""
    STATE["ir_last_input_ms"] = now
    log("IR start password input")
    set_ir_state("password_input")


def ir_cancel():
    STATE["ir_pass"] = ""
    STATE["ir_last_input_ms"] = 0
    set_ir_state("idle")
    log("IR cancel")


def ir_backspace(now):
    if STATE["ir_state"] != "password_input":
        return
    if STATE["ir_pass"]:
        STATE["ir_pass"] = STATE["ir_pass"][:-1]
    STATE["ir_last_input_ms"] = now
    log("IR backspace buffer={}".format("*" * len(STATE["ir_pass"])))


def door_open(now):
    STATE["door_open"] = 1
    STATE["door_locked"] = 0
    STATE["door_open_until"] = now + DOOR_OPEN_MS
    set_ir_state("door_open")
    log("DOOR open")
    log("DOOR open until={}".format(STATE["door_open_until"]))


def door_close():
    if STATE["door_open"]:
        log("DOOR close")
    STATE["door_open"] = 0
    STATE["door_locked"] = 1
    STATE["door_open_until"] = None
    if STATE["ir_state"] == "door_open":
        STATE["ir_state"] = "idle"


def ir_ok(now):
    if STATE["ir_state"] != "password_input":
        return
    set_ir_state("verifying")
    if STATE["ir_pass"] == DOOR_PASS:
        STATE["ir_pass"] = ""
        STATE["ir_last_input_ms"] = 0
        STATE["failed_attempts"] = 0
        log("IR password ok")
        door_open(now)
    else:
        STATE["failed_attempts"] += 1
        log("IR password wrong failed_attempts={}".format(STATE["failed_attempts"]))
        STATE["ir_pass"] = ""
        STATE["ir_last_input_ms"] = now
        if STATE["failed_attempts"] >= MAX_FAIL:
            set_ir_state("locked_out")
        else:
            set_ir_state("password_input")


def ir_digit(ch, now):
    if STATE["ir_state"] != "password_input":
        return
    if len(STATE["ir_pass"]) < len(DOOR_PASS):
        STATE["ir_pass"] += str(ch)
    STATE["ir_last_input_ms"] = now
    log("IR digit={} buffer={}".format(ch, "*" * len(STATE["ir_pass"])))


def process_ir_key(key, now):
    key = norm(key)
    if STATE["last_ir_key"] == key and now - STATE["last_ir_ms"] < 180:
        log("IR duplicate ignored code={}".format(key))
        return
    STATE["last_ir_key"] = key
    STATE["last_ir_ms"] = now

    if key in ("unlock", "setup", "door", "password"):
        if STATE["ir_state"] == "locked_out":
            log("IR locked_out")
            return
        ir_start(now)
        return
    if key in ("cancel", "esc"):
        ir_cancel()
        return
    if key in ("back", "backspace"):
        ir_backspace(now)
        return
    if key in ("ok", "enter"):
        ir_ok(now)
        return
    if len(key) == 1 and key.isdigit():
        ir_digit(key, now)


def run_ir_task(now):
    if STATE["ir_state"] == "password_input" and now - STATE["ir_last_input_ms"] >= IR_TIMEOUT_MS:
        STATE["ir_pass"] = ""
        STATE["ir_last_input_ms"] = 0
        set_ir_state("idle")
        log("IR password timeout")
    if STATE["ir_state"] == "locked_out" and now - STATE["ir_last_input_ms"] >= IR_LOCKOUT_MS:
        set_ir_state("idle")


def run_door_task(now):
    if STATE["door_open"] and STATE["door_open_until"] is not None and now >= STATE["door_open_until"]:
        door_close()


class ConsoleInput:
    def __init__(self):
        self.buffer = ""
        self.prompted = False
        try:
            import msvcrt
            self.msvcrt = msvcrt
        except Exception:
            self.msvcrt = None

    def prompt(self):
        if not self.prompted:
            print("sim> ", end="", flush=True)
            self.prompted = True

    def poll(self):
        self.prompt()
        if self.msvcrt:
            while self.msvcrt.kbhit():
                ch = self.msvcrt.getwch()
                if ch in ("\r", "\n"):
                    print()
                    line = self.buffer.strip()
                    self.buffer = ""
                    self.prompted = False
                    return line
                if ch in ("\b", "\x7f"):
                    self.buffer = self.buffer[:-1]
                    print("\b \b", end="", flush=True)
                    continue
                self.buffer += ch
                print(ch, end="", flush=True)
            return None

        try:
            import select
            ready, _, _ = select.select([sys.stdin], [], [], 0)
            if ready:
                self.prompted = False
                return sys.stdin.readline().strip()
        except Exception:
            return None
        return None


def handle_console(line, now):
    global RUNNING
    if not line:
        return
    parts = line.strip().split()
    if not parts:
        return
    if parts[0].lower() in ("quit", "exit", "q"):
        RUNNING = False
        return
    if parts[0].lower() == "ir" and len(parts) >= 2:
        process_ir_key(parts[1], now)
        return
    log("Commands: ir unlock | ir 1 | ir ok | ir cancel | ir back | quit")


def print_summary():
    log(
        "SUMMARY tel_ok={} tel_fail={} cmd_ok={} cmd_fail={} ack_ok={} ack_fail={} ex={}".format(
            STATS["telemetry_ok"],
            STATS["telemetry_fail"],
            STATS["command_ok"],
            STATS["command_fail"],
            STATS["ack_ok"],
            STATS["ack_fail"],
            STATS["exceptions"],
        )
    )


def main():
    global RUNNING
    log("Virtual YoloBit simulator gateway={}".format(BASE_URL))
    log("IR console: ir unlock, ir 1..9, ir 0, ir ok, ir cancel, ir back")
    console = ConsoleInput()

    while RUNNING:
        now = now_ms()
        if RUN_SECONDS > 0 and elapsed() >= RUN_SECONDS:
            break

        line = console.poll()
        if line is not None:
            handle_console(line, now)

        run_sensor_task(now)
        run_ir_task(now)
        run_door_task(now)
        run_backend_task(now)
        run_telemetry_task(now)

        if due("summary", now):
            print_summary()
            done("summary", now)

        time.sleep(0.02)

    print()
    print("===== FINAL RESULT =====")
    for key, value in STATS.items():
        print("{}: {}".format(key, value))


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nStopped")
