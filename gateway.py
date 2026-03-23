import os
import time
import logging
from collections import defaultdict, deque

import requests
from flask import Flask, request, jsonify, Response

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = int(os.getenv("MAX_CONTENT_LENGTH_BYTES", "4096"))

logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s %(levelname)s %(message)s"
)
logger = logging.getLogger("gateway")

BACKEND_BASE = os.getenv("BACKEND_BASE", "http://127.0.0.1:8080").rstrip("/")
YOLO_BASE = os.getenv("YOLO_BASE", "http://127.0.0.1:5000").rstrip("/")

REQUEST_TIMEOUT = float(os.getenv("REQUEST_TIMEOUT", "5"))
YOLO_TIMEOUT = float(os.getenv("YOLO_TIMEOUT", "2"))

RATE_LIMIT_WINDOW_SECONDS = int(os.getenv("RATE_LIMIT_WINDOW_SECONDS", "60"))
RATE_LIMIT_MAX_REQUESTS_DEFAULT = int(os.getenv("RATE_LIMIT_MAX_REQUESTS_DEFAULT", "120"))
RATE_LIMIT_MAX_REQUESTS_TELEMETRY = int(os.getenv("RATE_LIMIT_MAX_REQUESTS_TELEMETRY", "240"))
RATE_LIMIT_MAX_REQUESTS_YOLO = int(os.getenv("RATE_LIMIT_MAX_REQUESTS_YOLO", "30"))
RATE_LIMIT_MAX_REQUESTS_COMMAND = int(os.getenv("RATE_LIMIT_MAX_REQUESTS_COMMAND", "60"))

request_log = defaultdict(deque)
SESSION = requests.Session()

ALLOWED_SENSOR_TYPES = {"temperature", "humidity", "light", "motion"}
ALLOWED_ALERT_TYPES = {
    "HIGH_TEMPERATURE",
    "WRONG_PASSWORD",
    "DEVICE_OFFLINE",
    "SENSOR_ERROR",
    "MOTION_DETECTED",
}

SAFE_RESPONSE_HEADERS = {"content-type", "cache-control"}


def parse_csv_str_set(value, default=""):
    raw = value if value is not None else default
    return {x.strip() for x in raw.split(",") if x and x.strip()}


def parse_csv_int_set(value, default=""):
    raw = value if value is not None else default
    out = set()
    for x in raw.split(","):
        x = x.strip()
        if x and x.lstrip("-").isdigit():
            out.add(int(x))
    return out


DEMO_DEVICE_TOKEN = os.getenv("GATEWAY_DEVICE_TOKEN", "ohstem-demo-token")

ALLOWED_DEVICE_TOKENS = {
    DEMO_DEVICE_TOKEN: {
        "device_name": os.getenv("DEVICE_NAME", "Virtual OhStem Living Room"),
        "home_id": int(os.getenv("HOME_ID", "1")),
        "device_keys": parse_csv_str_set(
            os.getenv("ALLOWED_DEVICE_KEYS"),
            "yolobit-01,ohstem-fan-ctrl-01,ohstem-light-ctrl-01,ohstem-temp-01,ohstem-humidity-01,ohstem-light-01,ohstem-motion-01"
        ),
        "device_ids": parse_csv_int_set(
            os.getenv("ALLOWED_DEVICE_IDS"),
            "1,2,3"
        ),
        "allow_yolo": os.getenv("ALLOW_YOLO", "true").lower() == "true",
    }
}


def reject(message, status=400):
    return jsonify({"error": message}), status


def client_ip():
    forwarded_for = request.headers.get("X-Forwarded-For", "").strip()
    if forwarded_for:
        return forwarded_for.split(",")[0].strip()
    return request.remote_addr or "unknown"


def mask_token(token):
    if not token:
        return "none"
    if len(token) <= 6:
        return "***"
    return f"{token[:3]}***{token[-3:]}"


def sanitize_upstream_json_response(resp):
    headers = {}
    for k, v in resp.headers.items():
        if k.lower() in SAFE_RESPONSE_HEADERS:
            headers[k] = v

    content_type = resp.headers.get("Content-Type", "")
    if "application/json" in content_type.lower():
        return Response(resp.content, status=resp.status_code, headers=headers)

    if 200 <= resp.status_code < 300:
        return jsonify({"status": "ok"}), resp.status_code

    return jsonify({"error": "Upstream service error"}), resp.status_code


def safe_upstream_error(service_name):
    if service_name == "backend":
        return reject("Backend unavailable", 502)
    if service_name == "yolo":
        return reject("YOLO unavailable", 502)
    return reject("Upstream unavailable", 502)


def verify_device():
    token = request.headers.get("X-Device-Token", "").strip()
    if not token:
        return None, None
    meta = ALLOWED_DEVICE_TOKENS.get(token)
    if not meta:
        return token, None
    return token, meta


def rate_limit_bucket():
    path = request.path

    if path == "/gw/device-telemetry":
        return "telemetry", RATE_LIMIT_MAX_REQUESTS_TELEMETRY
    if path.startswith("/gw/yolo/"):
        return "yolo", RATE_LIMIT_MAX_REQUESTS_YOLO
    if "/commands/" in path or path.endswith("/alerts"):
        return "command", RATE_LIMIT_MAX_REQUESTS_COMMAND
    return "default", RATE_LIMIT_MAX_REQUESTS_DEFAULT


def apply_rate_limit(token):
    now = time.time()
    ip = client_ip()
    bucket_name, bucket_limit = rate_limit_bucket()
    key = f"{token}|{ip}|{bucket_name}"

    q = request_log[key]
    while q and now - q[0] > RATE_LIMIT_WINDOW_SECONDS:
        q.popleft()

    if len(q) >= bucket_limit:
        return False

    q.append(now)
    return True


def ensure_home_allowed(home_id):
    if home_id != request.device_meta["home_id"]:
        return reject("Forbidden homeId", 403)
    return None


def ensure_device_key_allowed(device_key):
    allowed = request.device_meta.get("device_keys", set())
    if device_key not in allowed:
        return reject("Forbidden deviceKey", 403)
    return None


def ensure_device_id_allowed(device_id):
    allowed = request.device_meta.get("device_ids", set())
    if device_id not in allowed:
        return reject("Forbidden deviceId", 403)
    return None


def ensure_yolo_allowed():
    if not request.device_meta.get("allow_yolo", False):
        return reject("YOLO access forbidden", 403)
    return None


def proxy_get(url, timeout):
    return SESSION.get(url, timeout=timeout)


def proxy_post(url, payload, timeout):
    return SESSION.post(url, json=payload, timeout=timeout)


@app.before_request
def security_check():
    if request.path == "/health":
        return None

    token, meta = verify_device()
    if not meta:
        logger.warning(
            "Unauthorized request path=%s ip=%s token=%s",
            request.path,
            client_ip(),
            mask_token(token),
        )
        return reject("Unauthorized device", 401)

    if not apply_rate_limit(token):
        logger.warning(
            "Rate limit exceeded path=%s ip=%s token=%s",
            request.path,
            client_ip(),
            mask_token(token),
        )
        return reject("Rate limit exceeded", 429)

    request.device_token = token
    request.device_meta = meta
    return None


@app.errorhandler(413)
def payload_too_large(_):
    return reject("Payload too large", 413)


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


@app.route("/gw/devices/home/<int:home_id>", methods=["GET"])
def get_devices_by_home(home_id):
    home_check = ensure_home_allowed(home_id)
    if home_check:
        return home_check
    try:
        resp = proxy_get(f"{BACKEND_BASE}/api/devices/home/{home_id}", timeout=REQUEST_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on get_devices_by_home")
        return safe_upstream_error("backend")


@app.route("/gw/devices/<int:device_id>/state", methods=["GET"])
def get_device_state(device_id):
    if device_id <= 0:
        return reject("Invalid deviceId", 400)

    device_check = ensure_device_id_allowed(device_id)
    if device_check:
        return device_check

    try:
        resp = proxy_get(f"{BACKEND_BASE}/api/devices/{device_id}/state", timeout=REQUEST_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on get_device_state")
        return safe_upstream_error("backend")


@app.route("/gw/homes/<int:home_id>/configs", methods=["GET"])
def get_home_config(home_id):
    home_check = ensure_home_allowed(home_id)
    if home_check:
        return home_check

    try:
        resp = proxy_get(f"{BACKEND_BASE}/api/homes/{home_id}/configs", timeout=REQUEST_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on get_home_config")
        return safe_upstream_error("backend")


@app.route("/gw/homes/<int:home_id>/alerts", methods=["POST"])
def create_alert(home_id):
    home_check = ensure_home_allowed(home_id)
    if home_check:
        return home_check

    body = request.get_json(silent=True)
    if not isinstance(body, dict):
        return reject("Invalid JSON body", 400)

    message = body.get("message")
    alert_type = body.get("type")
    device_id = body.get("deviceId")
    sensor_id = body.get("sensorId")

    if alert_type not in ALLOWED_ALERT_TYPES:
        return reject("Invalid alert type", 400)

    if not isinstance(message, str) or not message.strip():
        return reject("Invalid message", 400)

    message = message.strip()
    if len(message) > 255:
        return reject("Message too long", 400)

    if device_id is None or not isinstance(device_id, int):
        return reject("Invalid deviceId", 400)

    device_check = ensure_device_id_allowed(device_id)
    if device_check:
        return device_check

    payload = {
        "deviceId": device_id,
        "sensorId": sensor_id,
        "type": alert_type,
        "message": message,
    }

    try:
        resp = proxy_post(f"{BACKEND_BASE}/api/homes/{home_id}/alerts", payload, timeout=REQUEST_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on create_alert")
        return safe_upstream_error("backend")


@app.route("/gw/device-telemetry", methods=["POST"])
def create_telemetry():
    body = request.get_json(silent=True)
    if not isinstance(body, dict):
        return reject("Invalid JSON body", 400)

    device_key = body.get("deviceKey")
    sensor_type = body.get("sensorType")
    value = body.get("value")

    if not isinstance(device_key, str) or not device_key.strip():
        return reject("Invalid deviceKey", 400)

    device_key = device_key.strip()
    if len(device_key) > 100:
        return reject("deviceKey too long", 400)

    key_check = ensure_device_key_allowed(device_key)
    if key_check:
        return key_check

    if sensor_type not in ALLOWED_SENSOR_TYPES:
        return reject("Invalid sensorType", 400)

    if value is None:
        return reject("Missing value", 400)

    if sensor_type == "temperature":
        try:
            value = float(value)
        except (TypeError, ValueError):
            return reject("Invalid temperature", 400)
        if value < -50 or value > 120:
            return reject("Temperature out of range", 400)

    elif sensor_type == "humidity":
        try:
            value = float(value)
        except (TypeError, ValueError):
            return reject("Invalid humidity", 400)
        if value < 0 or value > 100:
            return reject("Humidity out of range", 400)

    elif sensor_type == "light":
        try:
            value = int(value)
        except (TypeError, ValueError):
            return reject("Invalid light", 400)
        if value < 0 or value > 100:
            return reject("Light out of range", 400)

    elif sensor_type == "motion":
        if not isinstance(value, bool):
            return reject("Invalid motion value", 400)

    payload = {
        "deviceKey": device_key,
        "sensorType": sensor_type,
        "value": value,
    }

    try:
        resp = proxy_post(f"{BACKEND_BASE}/api/device-telemetry", payload, timeout=REQUEST_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on create_telemetry")
        return safe_upstream_error("backend")


@app.route("/gw/device/<string:device_key>/commands/next", methods=["GET"])
def get_next_command(device_key):
    if not device_key or len(device_key) > 100:
        return reject("Invalid deviceKey", 400)

    key_check = ensure_device_key_allowed(device_key)
    if key_check:
        return key_check

    try:
        resp = proxy_get(f"{BACKEND_BASE}/api/v1/device/{device_key}/commands/next", timeout=REQUEST_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on get_next_command")
        return safe_upstream_error("backend")


@app.route("/gw/device/<string:device_key>/commands/ack", methods=["POST"])
def ack_command(device_key):
    if not device_key or len(device_key) > 100:
        return reject("Invalid deviceKey", 400)

    key_check = ensure_device_key_allowed(device_key)
    if key_check:
        return key_check

    body = request.get_json(silent=True)
    if not isinstance(body, dict):
        return reject("Invalid JSON body", 400)

    command_id = body.get("id")
    if command_id is None or not isinstance(command_id, int):
        return reject("Invalid command id", 400)

    try:
        resp = proxy_post(
            f"{BACKEND_BASE}/api/v1/device/{device_key}/commands/ack",
            {"id": command_id},
            timeout=REQUEST_TIMEOUT
        )
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on ack_command")
        return safe_upstream_error("backend")


@app.route("/gw/yolo/health", methods=["GET"])
def yolo_health():
    yolo_check = ensure_yolo_allowed()
    if yolo_check:
        return yolo_check

    try:
        resp = proxy_get(f"{YOLO_BASE}/health", timeout=YOLO_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("YOLO unavailable on yolo_health")
        return safe_upstream_error("yolo")


@app.route("/gw/yolo/check_human", methods=["GET"])
def yolo_check_human():
    yolo_check = ensure_yolo_allowed()
    if yolo_check:
        return yolo_check

    try:
        resp = proxy_get(f"{YOLO_BASE}/check_human", timeout=YOLO_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("YOLO unavailable on yolo_check_human")
        return safe_upstream_error("yolo")


if __name__ == "__main__":
    logger.info(
        "Gateway starting home_id=%s device_ids=%s device_keys=%s",
        ALLOWED_DEVICE_TOKENS[DEMO_DEVICE_TOKEN]["home_id"],
        sorted(ALLOWED_DEVICE_TOKENS[DEMO_DEVICE_TOKEN]["device_ids"]),
        sorted(ALLOWED_DEVICE_TOKENS[DEMO_DEVICE_TOKEN]["device_keys"]),
    )
    app.run(
        host=os.getenv("GATEWAY_HOST", "0.0.0.0"),
        port=int(os.getenv("GATEWAY_PORT", "9000")),
        debug=False,
    )