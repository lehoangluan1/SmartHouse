import os
import time
import json
import logging
import threading
from urllib.parse import quote
from collections import defaultdict, deque
from concurrent.futures import ThreadPoolExecutor, wait

import requests
from requests.adapters import HTTPAdapter
try:
    from urllib3.util.retry import Retry
except Exception:
    Retry = None
from flask import Flask, request, jsonify, Response


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

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = int(os.getenv("MAX_CONTENT_LENGTH_BYTES", "4096"))

logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO").upper(),
    format="%(asctime)s %(levelname)s %(message)s",
)
logger = logging.getLogger("gateway")

# Diagnostic flags for debugging command latency/shape.
DIAG_HTTP_BODY = os.getenv("DIAG_HTTP_BODY", "false").lower() == "true"
DIAG_HTTP_BODY_LIMIT = int(os.getenv("DIAG_HTTP_BODY_LIMIT", "600"))
DIAG_COMMAND = os.getenv("DIAG_COMMAND", "false").lower() == "true"

BACKEND_BASE = os.getenv("BACKEND_BASE", "http://localhost:8080").rstrip("/")
YOLO_BASE = os.getenv("YOLO_BASE", "http://127.0.0.1:5000").rstrip("/")
GATEWAY_HOST = os.getenv("GATEWAY_HOST", "0.0.0.0")
GATEWAY_PORT = int(os.getenv("GATEWAY_PORT", "9000"))

# IMPORTANT:
# These must be shorter than the YoloBit socket timeout.
# The YoloBit is single-threaded; every slow HTTP request freezes sensor, display and local logic.
REQUEST_TIMEOUT = float(os.getenv("REQUEST_TIMEOUT", "1.2"))
REGISTRY_TIMEOUT = float(os.getenv("REGISTRY_TIMEOUT", "1.2"))
CONFIG_TIMEOUT = float(os.getenv("CONFIG_TIMEOUT", "1.2"))
STATE_TIMEOUT = float(os.getenv("STATE_TIMEOUT", "0.9"))
COMMAND_TIMEOUT = float(os.getenv("COMMAND_TIMEOUT", "0.9"))
TELEMETRY_TIMEOUT = float(os.getenv("TELEMETRY_TIMEOUT", "1.0"))
ACK_TIMEOUT = float(os.getenv("ACK_TIMEOUT", "0.8"))
ALERT_TIMEOUT = float(os.getenv("ALERT_TIMEOUT", "1.0"))
YOLO_TIMEOUT = float(os.getenv("YOLO_TIMEOUT", "1.2"))

# Fast command mode:
# The gateway polls the cloud/backend in the background.
# YoloBit polls the gateway cache, so hardware control is not blocked by slow HTTPS.
COMMAND_PREFETCH_INTERVAL = float(os.getenv("COMMAND_PREFETCH_INTERVAL", "0.3"))
COMMAND_CACHE_TTL = float(os.getenv("COMMAND_CACHE_TTL", "8.0"))
COMMAND_ROUTE_SYNC_FALLBACK = os.getenv("COMMAND_ROUTE_SYNC_FALLBACK", "false").lower() == "true"
COMMAND_ROUTE_MAX_WAIT = float(os.getenv("COMMAND_ROUTE_MAX_WAIT", "0.15"))
COMMAND_LONG_POLL_WAIT_MS = int(os.getenv("COMMAND_LONG_POLL_WAIT_MS", "1500"))
COMMAND_PREFETCH_WAIT_MS = int(os.getenv("COMMAND_PREFETCH_WAIT_MS", "0"))
COMMAND_PREFETCH_TIMEOUT = float(os.getenv("COMMAND_PREFETCH_TIMEOUT", "0.45"))
COMMAND_LONG_POLL_TIMEOUT = max(COMMAND_TIMEOUT, (COMMAND_LONG_POLL_WAIT_MS / 1000.0) + 0.4)
BACKEND_BATCH_COMMANDS = os.getenv("BACKEND_BATCH_COMMANDS", "true").lower() == "true"

UPSTREAM_EXECUTOR = ThreadPoolExecutor(max_workers=int(os.getenv("UPSTREAM_MAX_WORKERS", "8")))

COMMAND_CACHE = {}
COMMAND_CACHE_TS = {}
COMMAND_KEYS_SEEN = set()
COMMAND_PREFETCH_STARTED = False
COMMAND_INFLIGHT = set()
COMMAND_LAST_FETCH = {}
COMMAND_MIN_INTERVAL_PER_KEY = float(os.getenv("COMMAND_MIN_INTERVAL_PER_KEY", "1.0"))
COMMAND_LOCK = threading.RLock()
COMMAND_DELIVERED_TTL = float(os.getenv("COMMAND_DELIVERED_TTL", "60.0"))
COMMAND_DELIVERED = {}
COMMAND_ACKED_LOCAL = {}
COMMAND_ACKED_UPSTREAM = {}

# Local instant command queue.
# Automation can POST directly to gateway; YoloBit receives it on next /gw/commands/next poll.
LOCAL_COMMAND_Q = defaultdict(deque)
LOCAL_COMMAND_MAX_PER_KEY = int(os.getenv("LOCAL_COMMAND_MAX_PER_KEY", "20"))

# Async telemetry queue. YoloBit receives response immediately; gateway forwards to backend in background.
TELEMETRY_ASYNC = os.getenv("TELEMETRY_ASYNC", "true").lower() == "true"
TELEMETRY_QUEUE_MAX = int(os.getenv("TELEMETRY_QUEUE_MAX", "300"))
TELEMETRY_Q = deque(maxlen=TELEMETRY_QUEUE_MAX)
TELEMETRY_WORKER_STARTED = False
TELEMETRY_LOCK = threading.RLock()

ACK_QUEUE_MAX = int(os.getenv("ACK_QUEUE_MAX", "200"))
ACK_RETRY_DELAY = float(os.getenv("ACK_RETRY_DELAY", "0.4"))
ACK_MAX_ATTEMPTS = int(os.getenv("ACK_MAX_ATTEMPTS", "8"))
ACK_Q = deque()
ACK_LOCK = threading.RLock()
ACK_WORKER_STARTED = False


RATE_LIMIT_WINDOW_SECONDS = int(os.getenv("RATE_LIMIT_WINDOW_SECONDS", "60"))
RATE_LIMIT_MAX_REQUESTS_DEFAULT = int(os.getenv("RATE_LIMIT_MAX_REQUESTS_DEFAULT", "180"))
RATE_LIMIT_MAX_REQUESTS_TELEMETRY = int(os.getenv("RATE_LIMIT_MAX_REQUESTS_TELEMETRY", "360"))
RATE_LIMIT_MAX_REQUESTS_YOLO = int(os.getenv("RATE_LIMIT_MAX_REQUESTS_YOLO", "60"))
RATE_LIMIT_MAX_REQUESTS_COMMAND = int(os.getenv("RATE_LIMIT_MAX_REQUESTS_COMMAND", "900"))

request_log = defaultdict(deque)

ALLOWED_SENSOR_TYPES = {"temperature", "humidity", "light", "motion"}
BACKEND_SENSOR_TYPE_MAP = {
    "temperature": "TEMPERATURE",
    "humidity": "HUMIDITY",
    "light": "LIGHT",
    "motion": "MOTION",
}
ALLOWED_ALERT_TYPES = {
    "HIGH_TEMPERATURE",
    "WRONG_PASSWORD",
    "DEVICE_OFFLINE",
    "SENSOR_ERROR",
    "MOTION_DETECTED",
}

SAFE_RESPONSE_HEADERS = {"content-type", "cache-control"}
DEMO_DEVICE_TOKEN = os.getenv("GATEWAY_DEVICE_TOKEN", "ohstem-demo-token")


def make_http_session():
    session = requests.Session()
    session.trust_env = False
    if Retry is not None:
        retry = Retry(
            total=0,
            connect=0,
            read=0,
            redirect=0,
            status=0,
            raise_on_status=False,
        )
        adapter = HTTPAdapter(max_retries=retry, pool_connections=20, pool_maxsize=20)
        session.mount("http://", adapter)
        session.mount("https://", adapter)
    return session


HTTP = make_http_session()


def auto_cast_value(value):
    if isinstance(value, str):
        text = value.strip()
        lower = text.lower()

        if lower in {"true", "false"}:
            return lower == "true"

        try:
            if "." in text:
                return float(text)
            return int(text)
        except Exception:
            return value

    return value


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


ALLOWED_DEVICE_TOKENS = {
    DEMO_DEVICE_TOKEN: {
        "device_name": os.getenv("DEVICE_NAME", "Virtual OhStem Living Room"),
        "home_id": int(os.getenv("HOME_ID", "1")),
        "device_keys": parse_csv_str_set(
            os.getenv("ALLOWED_DEVICE_KEYS"),
            "yolobit-01,ohstem-fan-ctrl-01,ohstem-light-ctrl-01,ohstem-temp-01,ohstem-humidity-01,ohstem-light-01,ohstem-motion-01",
        ),
        "device_ids": parse_csv_int_set(os.getenv("ALLOWED_DEVICE_IDS"), "1,2,3"),
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


def upstream_data(resp):
    if not 200 <= resp.status_code < 300:
        return None
    try:
        body = resp.json()
    except ValueError:
        return None
    if isinstance(body, dict) and "data" in body:
        return body.get("data")
    return body


def safe_upstream_error(service_name):
    if service_name == "backend":
        return reject("Backend unavailable", 502)
    if service_name == "yolo":
        return reject("YOLO unavailable", 502)
    return reject("Upstream unavailable", 502)


def get_request_json():
    data = request.get_json(silent=True)
    if isinstance(data, dict):
        return data

    try:
        raw = request.data.decode("utf-8")
        if raw:
            return json.loads(raw)
    except Exception:
        pass

    if request.form:
        return request.form.to_dict()

    return None


def verify_device():
    token = request.headers.get("X-Device-Token", "").strip()
    meta = ALLOWED_DEVICE_TOKENS.get(token)

    # Keep this relaxed for demo/dev so a wrong token does not kill the device loop.
    if not meta:
        logger.warning("[INVALID TOKEN] %s -> allow demo token", mask_token(token))
        return DEMO_DEVICE_TOKEN, ALLOWED_DEVICE_TOKENS[DEMO_DEVICE_TOKEN]

    return token, meta


def rate_limit_bucket():
    path = request.path

    if path == "/gw/device-telemetry":
        return "telemetry", RATE_LIMIT_MAX_REQUESTS_TELEMETRY
    if path.startswith("/gw/yolo/"):
        return "yolo", RATE_LIMIT_MAX_REQUESTS_YOLO
    if "/commands/" in path or path.startswith("/gw/commands") or path.endswith("/alerts"):
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


def timeout_pair(timeout):
    return (timeout, timeout)


def proxy_get(url, timeout):
    return HTTP.get(
        url,
        timeout=timeout_pair(timeout),
        headers={
            "Accept": "application/json",
            "Connection": "close",
            "User-Agent": "smart-house-gateway/fast-control",
        },
    )


def proxy_post(url, payload, timeout):
    return HTTP.post(
        url,
        json=payload,
        timeout=timeout_pair(timeout),
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Connection": "close",
            "User-Agent": "smart-house-gateway/fast-control",
        },
    )

def short_text(text, limit=None):
    if limit is None:
        limit = DIAG_HTTP_BODY_LIMIT
    try:
        text = str(text)
    except Exception:
        return "<unprintable>"

    text = text.replace("\n", "\\n").replace("\r", "\\r")
    return text[:limit]


def fetch_backend_data(label, url, timeout):
    t0 = time.time()
    try:
        resp = proxy_get(url, timeout=timeout)
        elapsed = round((time.time() - t0) * 1000)
        body_preview = ""
        if DIAG_HTTP_BODY:
            try:
                body_preview = short_text(resp.text)
            except Exception as exc:
                body_preview = "<body-read-error %s>" % type(exc).__name__

        logger.info(
            "UPSTREAM %s status=%s %sms url=%s body=%s",
            label,
            resp.status_code,
            elapsed,
            url,
            body_preview,
        )

        if 200 <= resp.status_code < 300:
            data = upstream_data(resp)
            if DIAG_COMMAND and "command" in label:
                logger.info(
                    "UPSTREAM-DATA %s type=%s data=%s",
                    label,
                    type(data).__name__,
                    short_text(data),
                )
            return data

        return None

    except requests.RequestException as exc:
        elapsed = round((time.time() - t0) * 1000)
        logger.warning(
            "UPSTREAM %s failed %sms err=%s detail=%s url=%s",
            label,
            elapsed,
            type(exc).__name__,
            short_text(exc, 300),
            url,
        )
        return None


def parallel_fetch(items, timeout):
    # items: list[(output_key, label, url)]
    # IMPORTANT: do not create a local "with ThreadPoolExecutor".
    # Its shutdown waits for slow HTTPS calls and defeats the timeout.
    if not items:
        return {}

    out = {key: None for key, _, _ in items}
    futures = {
        UPSTREAM_EXECUTOR.submit(fetch_backend_data, label, url, timeout): key
        for key, label, url in items
    }

    done, not_done = wait(list(futures.keys()), timeout=timeout + 0.25)

    for future in done:
        key = futures.get(future)
        try:
            out[key] = future.result()
        except Exception as exc:
            logger.warning("UPSTREAM future key=%s err=%s", key, type(exc).__name__)
            out[key] = None

    for future in not_done:
        key = futures.get(future)
        future.cancel()
        logger.warning("UPSTREAM deadline key=%s timeout=%ss", key, timeout)

    return out


def coerce_int(value):
    if isinstance(value, bool):
        return None
    if isinstance(value, int):
        return value
    if isinstance(value, float) and value.is_integer():
        return int(value)
    if isinstance(value, str):
        text = value.strip()
        if text and text.lstrip("-").isdigit():
            return int(text)
    return None


def coerce_float(value):
    if isinstance(value, bool):
        return None
    if isinstance(value, (int, float)):
        return float(value)
    if isinstance(value, str):
        text = value.strip()
        if not text:
            return None
        try:
            return float(text)
        except ValueError:
            return None
    return None


def coerce_bool(value):
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return bool(value)
    if isinstance(value, str):
        text = value.strip().lower()
        if text in {"1", "true", "yes", "on", "motion", "detected"}:
            return True
        if text in {"0", "false", "no", "off", "idle", "clear", "none"}:
            return False
    return None


def normalize_sensor_type(raw_value):
    if not isinstance(raw_value, str):
        return None, None

    normalized = raw_value.strip().lower()
    if normalized not in ALLOWED_SENSOR_TYPES:
        return None, None

    return normalized, BACKEND_SENSOR_TYPE_MAP[normalized]


@app.before_request
def security_check():
    request.start_time = time.time()
    request.req_id = "%08x" % int((time.time() * 1000000) % 0xFFFFFFFF)

    if request.method == "OPTIONS":
        return "", 200

    if request.path == "/health":
        return None

    token, meta = verify_device()
    if not meta:
        return reject("Unauthorized device", 401)

    if not apply_rate_limit(token):
        return reject("Rate limit exceeded", 429)

    request.device_meta = meta
    request.device_token = token
    return None


@app.after_request
def after_request(resp):
    elapsed = 0
    try:
        elapsed = round((time.time() - request.start_time) * 1000)
    except Exception:
        pass

    logger.info(
        "REQ %s %s %s -> %s %sms ip=%s qs=%s",
        getattr(request, "req_id", "-"),
        request.method,
        request.path,
        resp.status_code,
        elapsed,
        client_ip(),
        request.query_string.decode("utf-8", "ignore"),
    )

    resp.headers["Connection"] = "close"
    resp.headers["Cache-Control"] = "no-store"
    return resp


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
        resp = proxy_get(f"{BACKEND_BASE}/api/devices/home/{home_id}", timeout=REGISTRY_TIMEOUT)
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
        resp = proxy_get(f"{BACKEND_BASE}/api/devices/{device_id}/state", timeout=STATE_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on get_device_state")
        return safe_upstream_error("backend")


@app.route("/gw/devices/states", methods=["GET"])
def get_device_states():
    raw_ids = request.args.get("ids", "")
    ids = []
    for raw_id in raw_ids.split(","):
        device_id = coerce_int(raw_id)
        if device_id is not None and device_id > 0:
            ids.append(device_id)

    if not ids or len(ids) > 6:
        return reject("Invalid device ids", 400)

    items = []
    for device_id in ids:
        device_check = ensure_device_id_allowed(device_id)
        if device_check:
            return device_check
        items.append(
            (
                str(device_id),
                f"state id={device_id}",
                f"{BACKEND_BASE}/api/devices/{device_id}/state",
            )
        )

    out = parallel_fetch(items, timeout=STATE_TIMEOUT)
    return jsonify({"data": out})


@app.route("/gw/homes/<int:home_id>/configs", methods=["GET"])
def get_home_config(home_id):
    home_check = ensure_home_allowed(home_id)
    if home_check:
        return home_check

    try:
        resp = proxy_get(f"{BACKEND_BASE}/api/homes/{home_id}/configs", timeout=CONFIG_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on get_home_config")
        return safe_upstream_error("backend")


@app.route("/gw/homes/<int:home_id>/alerts", methods=["POST"])
def create_alert(home_id):
    home_check = ensure_home_allowed(home_id)
    if home_check:
        return home_check

    body = get_request_json()
    if not isinstance(body, dict):
        return reject("Invalid JSON body", 400)

    message = body.get("message")
    alert_type = body.get("type")
    device_id = coerce_int(body.get("deviceId"))
    raw_sensor_id = body.get("sensorId")
    sensor_id = None if raw_sensor_id is None else coerce_int(raw_sensor_id)

    if isinstance(alert_type, str):
        alert_type = alert_type.strip().upper()

    if alert_type not in ALLOWED_ALERT_TYPES:
        return reject("Invalid alert type", 400)

    if not isinstance(message, str) or not message.strip():
        return reject("Invalid message", 400)

    message = message.strip()
    if len(message) > 255:
        return reject("Message too long", 400)

    if device_id is None:
        return reject("Invalid deviceId", 400)

    if raw_sensor_id is not None and sensor_id is None:
        return reject("Invalid sensorId", 400)

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
        resp = proxy_post(f"{BACKEND_BASE}/api/homes/{home_id}/alerts", payload, timeout=ALERT_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on create_alert")
        return safe_upstream_error("backend")


@app.route("/gw/device-telemetry", methods=["POST"])
def create_telemetry():
    body = get_request_json()
    logger.debug("Telemetry body=%s", body)

    if not isinstance(body, dict):
        return reject("Invalid JSON body", 400)

    device_key = body.get("deviceKey")
    sensor_type, backend_sensor_type = normalize_sensor_type(body.get("sensorType"))
    value = auto_cast_value(body.get("value"))

    if not isinstance(device_key, str) or not device_key.strip():
        return reject("Invalid deviceKey", 400)

    device_key = device_key.strip()
    if len(device_key) > 100:
        return reject("deviceKey too long", 400)

    key_check = ensure_device_key_allowed(device_key)
    if key_check:
        return key_check

    if sensor_type is None:
        return reject("Invalid sensorType", 400)

    if value is None:
        return reject("Missing value", 400)

    if sensor_type == "temperature":
        value = coerce_float(value)
        if value is None:
            return reject("Invalid temperature", 400)
        if value < -50 or value > 120:
            return reject("Temperature out of range", 400)

    elif sensor_type == "humidity":
        value = coerce_float(value)
        if value is None:
            return reject("Invalid humidity", 400)
        if value < 0 or value > 100:
            return reject("Humidity out of range", 400)

    elif sensor_type == "light":
        value = coerce_int(value)
        if value is None:
            return reject("Invalid light", 400)
        if value < 0 or value > 100:
            return reject("Light out of range", 400)

    elif sensor_type == "motion":
        value = coerce_bool(value)
        if value is None:
            return reject("Invalid motion value", 400)

    payload = {
        "deviceKey": device_key,
        "sensorType": backend_sensor_type,
        "value": value,
    }

    if TELEMETRY_ASYNC:
        with TELEMETRY_LOCK:
            before = len(TELEMETRY_Q)
            TELEMETRY_Q.append(payload)
            dropped = before == TELEMETRY_QUEUE_MAX and len(TELEMETRY_Q) == TELEMETRY_QUEUE_MAX
            queue_size = len(TELEMETRY_Q)
        if dropped:
            logger.warning("ASYNC telemetry queue full; oldest sample dropped")
        return jsonify({"data": {"queued": True, "queueSize": queue_size}})

    try:
        resp = proxy_post(f"{BACKEND_BASE}/api/device-telemetry", payload, timeout=TELEMETRY_TIMEOUT)
        return sanitize_upstream_json_response(resp)
    except requests.RequestException:
        logger.exception("Backend unavailable on create_telemetry")
        return safe_upstream_error("backend")



def remember_command_keys(keys):
    allowed = request.device_meta.get("device_keys", set()) if hasattr(request, "device_meta") else set()
    with COMMAND_LOCK:
        for key in keys:
            if key in allowed:
                COMMAND_KEYS_SEEN.add(key)


def command_cache_get(device_key):
    with COMMAND_LOCK:
        cmd = COMMAND_CACHE.get(device_key)
        ts = COMMAND_CACHE_TS.get(device_key, 0)

        if cmd is None:
            if DIAG_COMMAND:
                logger.info("COMMAND cache miss key=%s", device_key)
            return None

        age = time.time() - ts
        if age > COMMAND_CACHE_TTL:
            COMMAND_CACHE.pop(device_key, None)
            COMMAND_CACHE_TS.pop(device_key, None)
            logger.info("COMMAND cache expired key=%s id=%s age=%.2fs", device_key, command_id_of(cmd), age)
            return None

        # Consume once. Backend will re-serve it only after redelivery timeout if ACK fails.
        COMMAND_CACHE.pop(device_key, None)
        COMMAND_CACHE_TS.pop(device_key, None)
    logger.info("COMMAND cache consume key=%s id=%s age=%.2fs", device_key, command_id_of(cmd), age)
    return cmd


def normalize_backend_command(device_key, raw):
    # Accept multiple backend response shapes:
    # {"data": {...}}, {"command": {...}}, {"result": {...}}, or list with one command.
    x = raw
    path = "root"

    if x is None:
        if DIAG_COMMAND:
            logger.info("COMMAND ignored key=%s reason=raw_none", device_key)
        return None

    if isinstance(x, dict):
        for key in ("data", "command", "result", "payload"):
            inner = x.get(key)
            if isinstance(inner, (dict, list)):
                x = inner
                path += ".%s" % key
                break
            if inner is None and key in x:
                if DIAG_COMMAND:
                    logger.info("COMMAND ignored key=%s reason=%s_is_null raw=%s", device_key, key, short_text(raw))
                return None

    if isinstance(x, list):
        if not x:
            if DIAG_COMMAND:
                logger.info("COMMAND ignored key=%s reason=list_empty raw=%s", device_key, short_text(raw))
            return None
        x = x[0]
        path += "[0]"

    if not isinstance(x, dict):
        if DIAG_COMMAND:
            logger.info(
                "COMMAND ignored key=%s reason=not_dict path=%s type=%s raw=%s",
                device_key,
                path,
                type(x).__name__,
                short_text(raw),
            )
        return None

    cid = (
        x.get("id")
        or x.get("commandId")
        or x.get("command_id")
        or x.get("deviceCommandId")
        or x.get("device_command_id")
    )

    target = (
        x.get("target")
        or x.get("command")
        or x.get("commandType")
        or x.get("type")
        or x.get("field")
        or x.get("action")
    )

    value = x.get("value")
    if value is None:
        value = x.get("commandValue")
    if value is None:
        value = x.get("payload")
    if value is None:
        value = x.get("status")

    if cid is None:
        logger.info(
            "COMMAND ignored key=%s reason=missing_id path=%s keys=%s raw=%s",
            device_key,
            path,
            sorted(list(x.keys())),
            short_text(raw),
        )
        return None

    if target is None:
        logger.info(
            "COMMAND ignored key=%s reason=missing_target path=%s id=%s keys=%s raw=%s",
            device_key,
            path,
            cid,
            sorted(list(x.keys())),
            short_text(raw),
        )
        return None

    out = dict(x)
    out["id"] = cid
    out["deviceKey"] = x.get("deviceKey") or x.get("device_key") or device_key
    out["target"] = target
    out["value"] = value

    if DIAG_COMMAND:
        logger.info(
            "COMMAND normalized key=%s path=%s id=%s target=%s value=%s raw_keys=%s",
            device_key,
            path,
            cid,
            target,
            value,
            sorted(list(x.keys())),
        )

    return out


def command_id_of(cmd):
    if isinstance(cmd, dict):
        return cmd.get("id")
    return None


def command_record_key(device_key, command_id):
    return "%s:%s" % (device_key, command_id)


def prune_command_records(now=None):
    if now is None:
        now = time.time()
    for table in (COMMAND_DELIVERED, COMMAND_ACKED_LOCAL, COMMAND_ACKED_UPSTREAM):
        expired = [key for key, ts in table.items() if now - ts > COMMAND_DELIVERED_TTL]
        for key in expired:
            table.pop(key, None)


def has_recent_command_record(device_key, command_id):
    if command_id is None:
        return False
    key = command_record_key(device_key, command_id)
    now = time.time()
    prune_command_records(now)
    return (
        key in COMMAND_DELIVERED
        or key in COMMAND_ACKED_LOCAL
        or key in COMMAND_ACKED_UPSTREAM
    )


def mark_command_delivered(device_key, cmd):
    command_id = command_id_of(cmd)
    if command_id is None:
        return
    with COMMAND_LOCK:
        prune_command_records()
        COMMAND_DELIVERED[command_record_key(device_key, command_id)] = time.time()
    logger.info(
        "COMMAND delivered_to_device key=%s id=%s target=%s value=%s source=%s",
        device_key,
        command_id,
        cmd.get("target") if isinstance(cmd, dict) else None,
        cmd.get("value") if isinstance(cmd, dict) else None,
        cmd.get("source") if isinstance(cmd, dict) else None,
    )


def command_cache_put(device_key, cmd):
    cmd = normalize_backend_command(device_key, cmd)
    if cmd is None:
        return

    with COMMAND_LOCK:
        if has_recent_command_record(device_key, cmd.get("id")):
            logger.info("COMMAND cache suppress already-delivered key=%s id=%s", device_key, cmd.get("id"))
            return

        old = COMMAND_CACHE.get(device_key)
        if isinstance(old, dict) and old.get("id") == cmd.get("id"):
            if DIAG_COMMAND:
                logger.info("COMMAND cache skip duplicate key=%s id=%s", device_key, cmd.get("id"))
            return

        COMMAND_CACHE[device_key] = cmd
        COMMAND_CACHE_TS[device_key] = time.time()
        cache_keys = list(COMMAND_CACHE.keys())
    logger.info(
        "COMMAND received/route key=%s id=%s target=%s value=%s source=%s cache_keys=%s",
        device_key,
        cmd.get("id"),
        cmd.get("target"),
        cmd.get("value"),
        cmd.get("source"),
        cache_keys,
    )


def fetch_one_command_for_cache(device_key):
    try:
        data = fetch_backend_data(
            f"command-prefetch key={device_key}",
            f"{BACKEND_BASE}/api/v1/device/{device_key}/commands/next?waitMs={COMMAND_PREFETCH_WAIT_MS}",
            timeout=COMMAND_PREFETCH_TIMEOUT,
        )
        command_cache_put(device_key, data)
    finally:
        with COMMAND_LOCK:
            COMMAND_INFLIGHT.discard(device_key)


def fetch_batch_commands_for_cache(keys):
    try:
        encoded_keys = quote(",".join(keys), safe=",")
        data = fetch_backend_data(
            "command-prefetch-batch keys=%s" % ",".join(keys),
            f"{BACKEND_BASE}/api/v1/device/commands/next-batch?keys={encoded_keys}&waitMs={COMMAND_PREFETCH_WAIT_MS}",
            timeout=COMMAND_PREFETCH_TIMEOUT,
        )
        if isinstance(data, dict):
            for key in keys:
                command_cache_put(key, data.get(key))
    finally:
        with COMMAND_LOCK:
            for key in keys:
                COMMAND_INFLIGHT.discard(key)


def command_prefetch_loop():
    while True:
        try:
            with COMMAND_LOCK:
                keys = list(COMMAND_KEYS_SEEN)
            if not keys:
                # Warm up common control keys.
                with COMMAND_LOCK:
                    for meta in ALLOWED_DEVICE_TOKENS.values():
                        for key in meta.get("device_keys", set()):
                            if key in {"yolobit-01", "ohstem-fan-ctrl-01", "ohstem-light-ctrl-01"}:
                                COMMAND_KEYS_SEEN.add(key)
                    keys = list(COMMAND_KEYS_SEEN)

            now = time.time()

            # Fetch only keys without cache and without an active upstream request.
            # This prevents the old flood of duplicated Render requests.
            ready = []
            with COMMAND_LOCK:
                for key in keys:
                    if COMMAND_CACHE.get(key) is not None:
                        continue
                    if key in COMMAND_INFLIGHT:
                        continue
                    if now - COMMAND_LAST_FETCH.get(key, 0) < COMMAND_MIN_INTERVAL_PER_KEY:
                        continue
                    ready.append(key)

                ready = ready[:6]
                for key in ready:
                    COMMAND_INFLIGHT.add(key)
                    COMMAND_LAST_FETCH[key] = now

            if ready and BACKEND_BATCH_COMMANDS:
                UPSTREAM_EXECUTOR.submit(fetch_batch_commands_for_cache, ready)
            else:
                for key in ready:
                    UPSTREAM_EXECUTOR.submit(fetch_one_command_for_cache, key)

        except Exception as exc:
            logger.warning("COMMAND prefetch loop err=%s", type(exc).__name__)

        time.sleep(COMMAND_PREFETCH_INTERVAL)


def start_command_prefetch_once():
    global COMMAND_PREFETCH_STARTED
    if COMMAND_PREFETCH_STARTED:
        return
    COMMAND_PREFETCH_STARTED = True
    import threading
    t = threading.Thread(target=command_prefetch_loop, daemon=True)
    t.start()
    logger.info(
        "Command prefetch started interval=%ss waitMs=%s timeout=%ss",
        COMMAND_PREFETCH_INTERVAL,
        COMMAND_PREFETCH_WAIT_MS,
        COMMAND_PREFETCH_TIMEOUT,
    )



def make_local_command_id():
    # Negative ids are local-only. YoloBit can ACK them, but gateway will not forward them to backend.
    return -int(time.time() * 1000)


def local_command_put(device_key, target, value, command_id=None):
    if command_id is None:
        command_id = make_local_command_id()

    cmd = {
        "id": command_id,
        "deviceKey": device_key,
        "target": target,
        "value": value,
        "source": "gateway-local",
        "createdAt": int(time.time() * 1000),
    }

    with COMMAND_LOCK:
        q = LOCAL_COMMAND_Q[device_key]
        q.append(cmd)
        while len(q) > LOCAL_COMMAND_MAX_PER_KEY:
            q.popleft()
        queue_size = len(q)

    logger.info(
        "LOCAL command queued key=%s id=%s target=%s value=%s queue=%s",
        device_key,
        command_id,
        target,
        value,
        queue_size,
    )
    return cmd


def local_command_get(device_key):
    with COMMAND_LOCK:
        q = LOCAL_COMMAND_Q.get(device_key)
        if not q:
            return None
        try:
            return q.popleft()
        except IndexError:
            return None


def normalize_local_command_body(body):
    if not isinstance(body, dict):
        return None
    if isinstance(body.get("commands"), list):
        return body.get("commands")
    return [body]


def telemetry_worker_loop():
    while True:
        try:
            with TELEMETRY_LOCK:
                if TELEMETRY_Q:
                    payload = TELEMETRY_Q.popleft()
                else:
                    payload = None
            if payload is None:
                time.sleep(0.03)
                continue

            t0 = time.time()
            try:
                resp = proxy_post(f"{BACKEND_BASE}/api/device-telemetry", payload, timeout=TELEMETRY_TIMEOUT)
                elapsed = round((time.time() - t0) * 1000)
                logger.info("ASYNC telemetry status=%s %sms", resp.status_code, elapsed)
            except requests.RequestException as exc:
                elapsed = round((time.time() - t0) * 1000)
                logger.warning("ASYNC telemetry failed %sms err=%s", elapsed, type(exc).__name__)

        except Exception as exc:
            logger.warning("ASYNC telemetry loop err=%s", type(exc).__name__)
            time.sleep(0.2)


def start_telemetry_worker_once():
    global TELEMETRY_WORKER_STARTED
    if TELEMETRY_WORKER_STARTED:
        return
    TELEMETRY_WORKER_STARTED = True
    import threading
    t = threading.Thread(target=telemetry_worker_loop, daemon=True)
    t.start()
    logger.info("Async telemetry worker started queue_max=%s", TELEMETRY_QUEUE_MAX)


def enqueue_ack(device_key, command_id):
    if command_id < 0:
        logger.info("GATEWAY command ack local key=%s id=%s", device_key, command_id)
        return True

    now = time.time()
    record_key = command_record_key(device_key, command_id)
    with COMMAND_LOCK:
        prune_command_records(now)
        COMMAND_ACKED_LOCAL[record_key] = now
        COMMAND_CACHE.pop(device_key, None)
        COMMAND_CACHE_TS.pop(device_key, None)

    with ACK_LOCK:
        for existing_key, existing_id, _attempt, _not_before in ACK_Q:
            if existing_key == device_key and existing_id == command_id:
                logger.info("COMMAND ack_received_from_device duplicate_queued key=%s id=%s", device_key, command_id)
                return True

        if len(ACK_Q) >= ACK_QUEUE_MAX:
            dropped = ACK_Q.popleft()
            logger.warning("ACK queue full; dropped oldest key=%s id=%s", dropped[0], dropped[1])

        ACK_Q.append((device_key, command_id, 1, now))
        logger.info("COMMAND ack_received_from_device key=%s id=%s queued=True", device_key, command_id)
        return True


def ack_worker_loop():
    while True:
        try:
            with ACK_LOCK:
                item = ACK_Q.popleft() if ACK_Q else None

            if item is None:
                time.sleep(0.02)
                continue

            device_key, command_id, attempt, not_before = item
            now = time.time()
            if not_before > now:
                with ACK_LOCK:
                    ACK_Q.append(item)
                time.sleep(min(0.05, max(0.01, not_before - now)))
                continue

            try:
                resp = proxy_post(
                    f"{BACKEND_BASE}/api/v1/device/{device_key}/commands/ack",
                    {"id": command_id},
                    timeout=ACK_TIMEOUT,
                )
                if 200 <= resp.status_code < 300:
                    with COMMAND_LOCK:
                        COMMAND_ACKED_UPSTREAM[command_record_key(device_key, command_id)] = time.time()
                    logger.info(
                        "COMMAND ack_forwarded_upstream key=%s id=%s status=%s attempt=%s",
                        device_key,
                        command_id,
                        resp.status_code,
                        attempt,
                    )
                else:
                    retry_ack(device_key, command_id, attempt, "status_%s" % resp.status_code)
            except requests.RequestException as exc:
                retry_ack(device_key, command_id, attempt, type(exc).__name__)

        except Exception as exc:
            logger.warning("ACK worker loop err=%s", type(exc).__name__)
            time.sleep(0.1)


def retry_ack(device_key, command_id, attempt, reason):
    if attempt >= ACK_MAX_ATTEMPTS:
        logger.warning(
            "COMMAND ack_forward_give_up key=%s id=%s attempts=%s reason=%s",
            device_key,
            command_id,
            attempt,
            reason,
        )
        return

    delay = min(ACK_RETRY_DELAY * (2 ** max(0, attempt - 1)), 5.0)
    next_attempt = attempt + 1
    not_before = time.time() + delay
    with ACK_LOCK:
        ACK_Q.append((device_key, command_id, next_attempt, not_before))
    logger.warning(
        "COMMAND ack_forward_failed_retry_scheduled key=%s id=%s attempt=%s nextAttempt=%s delay=%.2fs reason=%s",
        device_key,
        command_id,
        attempt,
        next_attempt,
        delay,
        reason,
    )


def start_ack_worker_once():
    global ACK_WORKER_STARTED
    if ACK_WORKER_STARTED:
        return
    ACK_WORKER_STARTED = True
    t = threading.Thread(target=ack_worker_loop, daemon=True)
    t.start()
    logger.info("Async ACK worker started queue_max=%s", ACK_QUEUE_MAX)



@app.route("/gw/local/command", methods=["POST"])
def create_local_command():
    body = get_request_json()
    items = normalize_local_command_body(body)

    if not isinstance(items, list) or not items:
        return reject("Invalid command body", 400)

    accepted = []

    for idx, item in enumerate(items):
        if not isinstance(item, dict):
            return reject("Invalid command item", 400)

        device_key = item.get("deviceKey")
        target = item.get("target")
        value = item.get("value")
        command_id = coerce_int(item.get("id"))

        if not isinstance(device_key, str) or not device_key.strip() or len(device_key) > 100:
            return reject("Invalid deviceKey", 400)
        device_key = device_key.strip()

        key_check = ensure_device_key_allowed(device_key)
        if key_check:
            return key_check

        if not isinstance(target, str) or not target.strip() or len(target) > 100:
            return reject("Invalid target", 400)

        if command_id is None:
            command_id = make_local_command_id() - idx

        cmd = local_command_put(device_key, target.strip(), value, command_id=command_id)
        accepted.append(cmd)

    return jsonify({"data": {"accepted": len(accepted), "commands": accepted}})


@app.route("/gw/device/<string:device_key>/commands/next", methods=["GET"])
def get_next_command(device_key):
    if not device_key or len(device_key) > 100:
        return reject("Invalid deviceKey", 400)

    key_check = ensure_device_key_allowed(device_key)
    if key_check:
        return key_check

    remember_command_keys([device_key])

    # Priority 1: instant local automation command.
    data = local_command_get(device_key)

    # Priority 2: prefetched backend command.
    if data is None:
        data = command_cache_get(device_key)

    if data is None and COMMAND_ROUTE_SYNC_FALLBACK:
        # Disabled by default. Do not enable if you need strict low latency.
        data = fetch_backend_data(
            f"command-sync key={device_key}",
            f"{BACKEND_BASE}/api/v1/device/{device_key}/commands/next",
            timeout=COMMAND_ROUTE_MAX_WAIT,
        )

    if isinstance(data, dict):
        mark_command_delivered(device_key, data)
        logger.info(
            "COMMAND received/route key=%s id=%s target=%s value=%s source=%s",
            device_key,
            data.get("id"),
            data.get("target"),
            data.get("value"),
            data.get("source"),
        )
    else:
        with COMMAND_LOCK:
            cache_remaining = {k: command_id_of(v) for k, v in COMMAND_CACHE.items()}
        logger.info(
            "GATEWAY command returned to YoloBit key=%s id=%s cache_remaining=%s",
            device_key,
            command_id_of(data),
            cache_remaining,
        )
    return jsonify({"data": data})


@app.route("/gw/commands/next", methods=["GET"])
def get_next_commands():
    raw_keys = request.args.get("keys", "")
    keys = [key.strip() for key in raw_keys.split(",") if key.strip()]

    if not keys or len(keys) > 6 or any(len(key) > 100 for key in keys):
        return reject("Invalid device keys", 400)

    for device_key in keys:
        key_check = ensure_device_key_allowed(device_key)
        if key_check:
            return key_check

    remember_command_keys(keys)

    out = {}
    for device_key in keys:
        data = local_command_get(device_key)
        if data is None:
            data = command_cache_get(device_key)
        out[device_key] = data

    if COMMAND_ROUTE_SYNC_FALLBACK and all(v is None for v in out.values()):
        # Disabled by default. Do not enable if you need strict low latency.
        items = [
            (
                device_key,
                f"command-sync key={device_key}",
                f"{BACKEND_BASE}/api/v1/device/{device_key}/commands/next",
            )
            for device_key in keys
        ]
        out = parallel_fetch(items, timeout=COMMAND_ROUTE_MAX_WAIT)

    for device_key, data in out.items():
        if isinstance(data, dict):
            mark_command_delivered(device_key, data)
            logger.info(
                "COMMAND received/route key=%s id=%s target=%s value=%s source=%s",
                device_key,
                data.get("id"),
                data.get("target"),
                data.get("value"),
                data.get("source"),
            )

    with COMMAND_LOCK:
        cache_remaining = {k: command_id_of(v) for k, v in COMMAND_CACHE.items()}
        local_remaining = {k: len(v) for k, v in LOCAL_COMMAND_Q.items()} if "LOCAL_COMMAND_Q" in globals() else {}
    logger.info(
        "GATEWAY command returned to YoloBit bulk ids=%s empty=%s cache_remaining=%s local_remaining=%s",
        {k: command_id_of(v) for k, v in out.items()},
        all(v is None for v in out.values()),
        cache_remaining,
        local_remaining,
    )
    return jsonify({"data": out})


@app.route("/gw/device/<string:device_key>/commands/ack", methods=["POST"])
def ack_command(device_key):
    if not device_key or len(device_key) > 100:
        return reject("Invalid deviceKey", 400)

    key_check = ensure_device_key_allowed(device_key)
    if key_check:
        return key_check

    body = get_request_json()
    logger.debug("ACK gateway in: device_key=%s, body=%s", device_key, body)

    if not isinstance(body, dict):
        return reject("Invalid JSON body", 400)

    command_id = coerce_int(body.get("id"))
    if command_id is None:
        return reject("Invalid command id", 400)

    if command_id < 0:
        enqueue_ack(device_key, command_id)
        return jsonify({"data": {"acked": 1, "local": True}})

    enqueue_ack(device_key, command_id)
    return jsonify({"data": {"acked": 1, "queued": True}})


@app.route("/gw/commands/ack", methods=["POST"])
def ack_commands():
    body = get_request_json()

    if not isinstance(body, dict) or not isinstance(body.get("acks"), list):
        return reject("Invalid JSON body", 400)

    acks = body.get("acks")
    if len(acks) > 8:
        return reject("Too many ACKs", 400)

    ok_count = 0

    for item in acks:
        if not isinstance(item, dict):
            return reject("Invalid ACK item", 400)

        device_key = item.get("deviceKey")
        command_id = coerce_int(item.get("id"))

        if not isinstance(device_key, str) or not device_key.strip() or len(device_key) > 100:
            return reject("Invalid deviceKey", 400)

        device_key = device_key.strip()
        key_check = ensure_device_key_allowed(device_key)
        if key_check:
            return key_check

        if command_id is None:
            return reject("Invalid command id", 400)

        if command_id < 0:
            enqueue_ack(device_key, command_id)
            ok_count += 1
            continue

        enqueue_ack(device_key, command_id)
        ok_count += 1

    # Return 200 anyway so YoloBit does not get stuck retrying forever.
    return jsonify({"data": {"acked": ok_count, "requested": len(acks), "queued": True}})


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
        "Gateway starting home_id=%s device_ids=%s device_keys=%s backend=%s yolo=%s",
        ALLOWED_DEVICE_TOKENS[DEMO_DEVICE_TOKEN]["home_id"],
        sorted(ALLOWED_DEVICE_TOKENS[DEMO_DEVICE_TOKEN]["device_ids"]),
        sorted(ALLOWED_DEVICE_TOKENS[DEMO_DEVICE_TOKEN]["device_keys"]),
        BACKEND_BASE,
        YOLO_BASE,
    )
    logger.info(
        "DIAG config diag_http_body=%s body_limit=%s diag_command=%s command_timeout=%s command_wait_ms=%s command_prefetch_wait_ms=%s command_prefetch_timeout=%s command_cache_ttl=%s route_sync_fallback=%s backend_batch=%s ack_queue_max=%s ack_retry_delay=%s ack_max_attempts=%s",
        DIAG_HTTP_BODY,
        DIAG_HTTP_BODY_LIMIT,
        DIAG_COMMAND,
        COMMAND_TIMEOUT,
        COMMAND_LONG_POLL_WAIT_MS,
        COMMAND_PREFETCH_WAIT_MS,
        COMMAND_PREFETCH_TIMEOUT,
        COMMAND_CACHE_TTL,
        COMMAND_ROUTE_SYNC_FALLBACK,
        BACKEND_BATCH_COMMANDS,
        ACK_QUEUE_MAX,
        ACK_RETRY_DELAY,
        ACK_MAX_ATTEMPTS,
    )
    start_telemetry_worker_once()
    start_ack_worker_once()
    start_command_prefetch_once()
    app.run(
        host=GATEWAY_HOST,
        port=GATEWAY_PORT,
        debug=False,
        threaded=True,
    )
