import urequests, ujson, gc, time
from yolobit import *
from aiot_rgbled import RGBLed
from mqtt import *
from aiot_lcd1602 import LCD1602
from aiot_dht20 import DHT20
from aiot_ir_receiver import *
try:
    import _thread
    THREAD_OK = True
except Exception as _e:
    _thread = None
    THREAD_OK = False
try:
    import usocket
    usocket.setdefaulttimeout(0.35)
except:
    pass
DEBUG = False
PERF_LOG = False
STATUS_LOG = True
IR_DEBUG = True
button_a.on_pressed = button_b.on_pressed = None
button_a.on_pressed_ab = button_b.on_pressed_ab = -1
rgb = RGBLed(pin1.pin, 4)
lcd = LCD1602()
dht = DHT20()
try:
    from aiot_local_config import GATEWAY_HOST, GATEWAY_PORT, HOME_ID, DEVICE_TOKEN, WIFI_SSID as CFG_WIFI_SSID, WIFI_PASS as CFG_WIFI_PASS
except:
    GATEWAY_HOST = '192.168.1.166'
    GATEWAY_PORT = 9000
    HOME_ID = 1
    DEVICE_TOKEN = 'ohstem-demo-token'
    CFG_WIFI_SSID = 'Test'
    CFG_WIFI_PASS = '12345678'
HOST, PORT, HOME = (GATEWAY_HOST, GATEWAY_PORT, HOME_ID)
BASE = 'http://%s:%s' % (HOST, PORT)
HEAD = {'X-Device-Token': DEVICE_TOKEN, 'Content-Type': 'application/json', 'Connection': 'close'}
WIFI_SSID, WIFI_PASS = (CFG_WIFI_SSID, CFG_WIFI_PASS)
DOOR_PASS = '123456'
DOOR_OPEN_MS = 5000
IR_TIMEOUT_MS = 15000
MAX_FAIL = 3
BOOT_GRACE_MS = 8000
IR_NET_QUIET_MS = 6000
IR_HTTP_PAUSE = 1
HTTP_COOLDOWN_MS = 900
ERROR_LED_GRACE_MS = 5000
SERVER_LED_GRACE_MS = 6000
ALERT_AWAY_GRACE_MS = 15000
ALERT_TEMP_GRACE_MS = 10000
IR_THREAD_SLEEP_MS = 12
LED4_SHOW_ENV_ALERT = 0
DOOR_OPEN_ANGLE = 180
DOOR_CLOSE_ANGLE = 0
CFG = {'Thigh': 30.0, 'Tlow': 27.0, 'Lhigh': 55, 'Llow': 35, 'Tsleep_high': 32.0, 'Tsleep_low': 26.0, 'Taway_high': 33.0, 'Tcritical': 35.0, 'auto_fan_speed': 70, 'sleep_fan_speed': 30, 'away_fan_speed': 60}
KEY = {'runtime_key': 'yolobit-01', 'fan_key': 'ohstem-fan-ctrl-01', 'light_key': 'ohstem-light-ctrl-01', 'temp_key': 'ohstem-temp-01', 'humidity_key': 'ohstem-humidity-01', 'light_sensor_key': 'ohstem-light-01', 'motion_key': 'ohstem-motion-01', 'runtime_id': None, 'fan_id': None, 'light_id': None}
S = {'boot_ms': 0, 'mode': 'away', 'prev_mode': None, 'hold_until': None, 'fan_status': 'off', 'fan_speed': 0, 'light_status': 'off', 'local_fan_status': 'off', 'local_fan_speed': 0, 'local_light_status': 'off', 'sensor_error': 0, 'state_error': 0, 'config_error': 0, 'telemetry_error': 0, 'command_error': 0, 'registry_error': 0, 'yolo_error': 0, 'alert_active': 0, 'security_alert_active': 0, 'nhiet_do': None, 'do_am': None, 'shine': None, 'pir_motion': 0, 'camera_human_detected': 0, 'camera_human_count': 0, 'camera_confidence': 0.0, 'camera_motion_detected': 0, 'camera_motion_score': 0.0, 'someone': 0, 'last_human_seen_ms': 0, 'door_locked': 1, 'door_open': 0, 'door_open_until': None, 'ir_state': 'idle', 'ir_typing': 0, 'ir_pass': '', 'ir_last_input_ms': 0, 'failed_attempts': 0, 'last_ir_code': None, 'last_ir_ms': 0, 'ir_pending': None, 'ir_quiet_until': 0, 'last_security_alert_ms': 0, 'last_gc': 0, 'server_ready': 0, 'net_block_until': 0, 'next_http_ms': 0, 'consecutive_http_fail': 0, 'last_cmd_apply_ms': 0, 'last_l1': '', 'last_l2': '', 'last_fan_hw': None, 'last_light_hw': None, 'last_door_hw': None, 'sensor_error_since': 0, 'server_error_since': 0, 'away_alert_since': 0, 'temp_alert_since': 0, 'last_registry_debug_ms': 0}
# Safety defaults for values touched by the IR thread.
# Kept outside the large S literal to survive edits/copies on MicroPython.
try:
    S.setdefault('last_door_hw', None)
    S.setdefault('door_open', 0)
    S.setdefault('door_open_until', None)
    S.setdefault('ir_state', 'idle')
    S.setdefault('ir_pass', '')
    S.setdefault('ir_last_input_ms', 0)
except:
    pass

IR_DIGIT_MAP = {IR_REMOTE_0: '0', IR_REMOTE_1: '1', IR_REMOTE_2: '2', IR_REMOTE_3: '3', IR_REMOTE_4: '4', IR_REMOTE_5: '5', IR_REMOTE_6: '6', IR_REMOTE_7: '7', IR_REMOTE_8: '8', IR_REMOTE_9: '9'}
RAW_IR_DIGIT_MAP = {22: '0', 12: '1', 24: '2', 94: '3', 8: '4', 28: '5', 90: '6', 66: '7', 82: '8', 74: '9'}
RAW_IR_KEY_SETUP = 21
RAW_IR_KEY_OK = 13
RAW_IR_KEY_CANCEL = 7
RAW_IR_KEY_BACK = 9
IR_KEY_SETUP = IR_REMOTE_SETUP
IR_KEY_OK = IR_REMOTE_F
IR_KEY_CANCEL = globals().get('IR_REMOTE_A', None)
IR_KEY_BACK = globals().get('IR_REMOTE_B', None)
IR_DEBOUNCE_MS = 180
IR_LOCKOUT_MS = 15000
ITV = {'registry': 120000, 'state': 45000, 'config': 120000, 'telemetry': 10000, 'command': 2500, 'yolo': 30000, 'status': 2500, 'sensor': 1000, 'fast_sensor': 100, 'display': 150, 'led': 100, 'ack': 1000, 'alert': 2000}
LAST = {k: 0 for k in ITV}
TEL = (('temp_key', 'temperature', 'nhiet_do'), ('humidity_key', 'humidity', 'do_am'), ('light_sensor_key', 'light', 'shine'), ('motion_key', 'motion', 'someone'))
tel_i = 0
temp_alert_last = 0
PENDING_ALERT = None
ACKQ = []
IR_THREAD_STARTED = False
IR_Q = []
IR_LOCK = None
try:
    IR_LOCK = _thread.allocate_lock() if THREAD_OK else None
except:
    IR_LOCK = None
PERF = {'loop_sum': 0, 'loop_n': 0, 'loop_max': 0, 'http_ms': 0, 'http_max': 0, 'http_path': '', 'cmd_apply_ms': 0, 'ack_ms': 0, 'tel_delay': 0, 'last': 0, 'mem': 0}

def j_text(txt):
    try:
        return ujson.loads(txt)
    except:
        return None

def close_resp(r):
    try:
        r and r.close()
    except:
        pass

def ok(r):
    try:
        return r and 200 <= r.status_code < 300
    except:
        return False

def unwrap(x):
    return x.get('data') if isinstance(x, dict) and 'data' in x else x

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
    return '' if v is None else str(v).strip().lower()

def fit16(s):
    s = str(s)
    return s[:16] if len(s) > 16 else s + ' ' * (16 - len(s))

def ms():
    return time.ticks_ms()

def in_boot_grace():
    return time.ticks_diff(ms(), S['boot_ms']) < BOOT_GRACE_MS

def raw_server_error():
    return 0 if in_boot_grace() else 1 if S['state_error'] or S['config_error'] or S['command_error'] or S['registry_error'] else 0

def debounced_visible(raw, since_key, grace_ms):
    if in_boot_grace():
        return 0
    if raw:
        if not S.get(since_key):
            S[since_key] = ms()
            return 0
        return 1 if time.ticks_diff(ms(), S.get(since_key)) >= grace_ms else 0
    S[since_key] = 0
    return 0

def visible_sensor_error():
    return debounced_visible(S['sensor_error'], 'sensor_error_since', ERROR_LED_GRACE_MS)

def server_error():
    return debounced_visible(raw_server_error(), 'server_error_since', SERVER_LED_GRACE_MS)

def state_unavailable():
    return 0 if in_boot_grace() else 1 if S['state_error'] or S['registry_error'] else 0

def mark_ir_activity(extra_ms=None):
    if extra_ms is None:
        extra_ms = IR_NET_QUIET_MS
    try:
        S['ir_quiet_until'] = time.ticks_add(ms(), extra_ms)
    except:
        pass

def ir_http_pause():
    if not IR_HTTP_PAUSE:
        return 0

    # Use the older, more conservative gate that worked well with IR.
    # S['ir_pending'] intentionally stays non-None briefly after a callback,
    # so HTTP does not immediately steal CPU from the first remote press.
    try:
        if S.get('ir_pending', None) is not None:
            return 1
    except:
        pass

    if S.get('ir_state') in ('password_input', 'verifying', 'door_open', 'locked_out'):
        return 1

    try:
        q_until = S.get('ir_quiet_until', 0)
        if q_until and time.ticks_diff(ms(), q_until) < 0:
            return 1
    except:
        pass

    return 0

def net_allowed():
    return time.ticks_diff(ms(), S['net_block_until']) >= 0

def net_ready():
    try:
        nxt = S.get('next_http_ms', 0)
        if nxt is None:
            nxt = 0
        return net_allowed() and time.ticks_diff(ms(), nxt) >= 0 and (not ir_http_pause())
    except Exception as e:
        try:
            print('net_ready err', e)
        except:
            pass
        return False

def mark_http_slot_used(extra=None):
    if extra is None:
        extra = HTTP_COOLDOWN_MS
    try:
        S['next_http_ms'] = time.ticks_add(ms(), extra)
    except:
        pass

def mark_net_fail(wait=0):
    S['consecutive_http_fail'] += 1
    if wait:
        S['net_block_until'] = time.ticks_add(ms(), wait)
    elif S['consecutive_http_fail'] >= 4:
        delay = 1000 * S['consecutive_http_fail']
        if delay > 15000:
            delay = 15000
        S['net_block_until'] = time.ticks_add(ms(), delay)

def mark_net_ok():
    S['consecutive_http_fail'] = 0
    S['net_block_until'] = 0

def perf_http(path, d):
    if PERF_LOG:
        PERF['http_ms'] = d
        PERF['http_path'] = path
        if d > PERF['http_max']:
            PERF['http_max'] = d

def req(method, path, data=None):
    if ir_http_pause() and (not path.startswith('/gw/commands/ack')):
        return None if method == 'GET' else False
    r = None
    t0 = ms() if PERF_LOG else 0
    try:
        url = BASE + path
        if DEBUG:
            print('HTTP', method, url)
        if method == 'GET':
            r = urequests.get(url, headers=HEAD)
        else:
            r = urequests.post(url, json=data, headers=HEAD)
        status = r.status_code
        body = ''
        if method == 'GET':
            try:
                body = r.text
                if body and len(body) > 1200:
                    body = body[:1200]
            except Exception as e:
                if DEBUG:
                    print('HTTP body read err', path, e)
        if DEBUG:
            print('HTTP status', status, path)
        if 200 <= status < 300:
            mark_net_ok()
            if method == 'GET':
                x = j_text(body) if body else None
                return unwrap(x)
            return True
        if DEBUG:
            print('HTTP bad', status, path, body[:80] if body else '')
        mark_net_fail()
    except Exception as e:
        if DEBUG:
            print('HTTP err', method, path, e)
        mark_net_fail()
    finally:
        mark_http_slot_used()
        if PERF_LOG:
            perf_http(path, time.ticks_diff(ms(), t0))
        close_resp(r)
        try:
            gc.collect()
        except:
            pass
    return None if method == 'GET' else False
GET = lambda p: req('GET', p)
POST = lambda p, d: req('POST', p, d)

def norm_switch(v, d='off'):
    s = norm(v)
    if s in ('on', '1', 'true', 'yes'):
        return 'on'
    if s in ('off', '0', 'false', 'no'):
        return 'off'
    try:
        return 'on' if int(v) else 'off'
    except:
        return d

def active_fan_status():
    return S['local_fan_status'] if state_unavailable() else S['fan_status']

def active_fan_speed():
    return S['local_fan_speed'] if state_unavailable() else S['fan_speed']

def active_light_status():
    return S['local_light_status'] if state_unavailable() else S['light_status']

def fan_hw(on, speed):
    try:
        pin0.write_digital(1 if on else 0)
    except:
        pass

def door_hw(op):
    try:
        ang = DOOR_OPEN_ANGLE if op else DOOR_CLOSE_ANGLE
        pin4.servo_write(ang)
        print('DOOR hw pin4 angle=', ang)
    except Exception as e:
        try:
            print('DOOR hw err', e)
        except:
            pass

def open_door():
    mark_ir_activity(DOOR_OPEN_MS + 1000)
    S['door_open'] = 1
    S['door_locked'] = 0
    S['door_open_until'] = time.ticks_add(ms(), DOOR_OPEN_MS)
    S['ir_state'] = 'door_open'
    S['ir_typing'] = 0
    print('DOOR open')
    print('DOOR open until=', S['door_open_until'])

def close_door():
    was = S['door_open']
    S['door_open'] = 0
    S['door_locked'] = 1
    S['door_open_until'] = None
    if S['ir_state'] == 'door_open':
        S['ir_state'] = 'idle'
    if was:
        print('DOOR close')

def mode_short():
    return {'auto': 'A', 'manual': 'M', 'sleep': 'S', 'away': 'W'}.get(S['mode'], '?')

def update_lcd():
    t = '--' if S['nhiet_do'] is None else str(round(S['nhiet_do'], 1))
    h = '--' if S['do_am'] is None else str(round(S['do_am'], 1))
    l = '--' if S['shine'] is None else str(S['shine'])
    l1 = fit16('T:%s H:%s' % (t, h))
    sen = visible_sensor_error()
    srv = server_error()
    if S['ir_state'] == 'password_input':
        l2 = fit16('PASS:' + '*' * len(S['ir_pass']))
    elif sen and srv:
        l2 = fit16('L:%s E:SEN+SRV' % l)
    elif sen:
        l2 = fit16('L:%s E:SENSOR' % l)
    elif srv:
        l2 = fit16('FB %s P%s D%s' % (mode_short(), 1 if S['someone'] else 0, 1 if S['door_open'] else 0))
    else:
        l2 = fit16('L:%s %s P%s D%s' % (l, mode_short(), 1 if S['someone'] else 0, 1 if S['door_open'] else 0))
    try:
        if l1 != S['last_l1']:
            lcd.move_to(0, 0)
            lcd.putstr(l1)
            S['last_l1'] = l1
        if l2 != S['last_l2']:
            lcd.move_to(0, 1)
            lcd.putstr(l2)
            S['last_l2'] = l2
    except:
        pass

def update_leds():
    try:
        blink = ms() // 250 % 2 == 0
        a = S['security_alert_active'] or (LED4_SHOW_ENV_ALERT and S['alert_active'])
        sen = visible_sensor_error()
        srv = server_error()
        rgb.show(1, (5, 0, 0) if sen and blink else (0, 0, 0))
        rgb.show(2, (5, 0, 0) if srv and blink else (0, 0, 0))
        rgb.show(3, (5, 5, 0) if (sen or srv) and blink else (0, 5, 0) if active_light_status() == 'on' or S['door_open'] else (0, 0, 0))
        rgb.show(4, (5, 0, 0) if a and blink else (0, 0, 0))
    except:
        pass

def device_key_of(d):
    if not isinstance(d, dict):
        return ''
    return norm(d.get('deviceKey') or d.get('device_key') or d.get('key') or d.get('runtimeKey') or d.get('deviceKeyName') or d.get('name'))

def device_id_of(d):
    if not isinstance(d, dict):
        return None
    return d.get('id') or d.get('deviceId') or d.get('device_id')

def registry_demo_fallback():
    if KEY['runtime_id'] is None:
        KEY['runtime_id'] = 1
    if KEY['fan_id'] is None:
        KEY['fan_id'] = 2
    if KEY['light_id'] is None:
        KEY['light_id'] = 3

def load_registry():
    ds = GET('/gw/devices/home/%s' % HOME)
    if isinstance(ds, dict):
        ds = ds.get('devices') or ds.get('items') or ds.get('data') or ds.get('results')
    if not isinstance(ds, list) or not ds:
        registry_demo_fallback()
        S['registry_error'] = 0 if KEY['runtime_id'] and KEY['fan_id'] and KEY['light_id'] else 1
        return
    KEY['runtime_id'] = KEY['fan_id'] = KEY['light_id'] = None
    for d in ds:
        try:
            k = device_key_of(d)
            did = device_id_of(d)
            if k == KEY['runtime_key']:
                KEY['runtime_id'] = did
            elif k == KEY['fan_key']:
                KEY['fan_id'] = did
            elif k == KEY['light_key']:
                KEY['light_id'] = did
        except:
            pass
    if not (KEY['runtime_id'] and KEY['fan_id'] and KEY['light_id']):
        registry_demo_fallback()
    S['registry_error'] = 0 if KEY['runtime_id'] and KEY['fan_id'] and KEY['light_id'] else 1
    if S['registry_error'] and time.ticks_diff(ms(), S.get('last_registry_debug_ms', 0)) > 5000:
        print('REGISTRY missing ids runtime/fan/light=', KEY['runtime_id'], KEY['fan_id'], KEY['light_id'])
        S['last_registry_debug_ms'] = ms()
CMD_SYNC_GRACE_MS = 8000

def state_from_map(d, k):
    if not isinstance(d, dict):
        return None
    v = d.get(k)
    if isinstance(v, dict):
        return v
    return None

def fetch_all_states():
    skip = time.ticks_diff(ms(), S['last_cmd_apply_ms']) < CMD_SYNC_GRACE_MS
    okc = 0
    if not (KEY['runtime_id'] and KEY['fan_id'] and KEY['light_id']):
        load_registry()
        if not (KEY['runtime_id'] and KEY['fan_id'] and KEY['light_id']):
            S['state_error'] = 1
            return
    d = GET('/gw/devices/states?ids=%s,%s,%s' % (KEY['runtime_id'], KEY['fan_id'], KEY['light_id']))
    if not isinstance(d, dict):
        S['state_error'] = 1
        return
    a = state_from_map(d, str(KEY['runtime_id']))
    b = state_from_map(d, str(KEY['fan_id']))
    c = state_from_map(d, str(KEY['light_id']))
    if isinstance(a, dict):
        if not skip and a.get('mode') is not None:
            mv = norm(a.get('mode'))
            if mv in ('auto', 'manual', 'sleep', 'away'):
                S['mode'] = mv
        S['hold_until'] = a.get('holdUntil')
        S['prev_mode'] = a.get('prevMode')
        okc += 1
    if isinstance(b, dict):
        if not skip:
            if b.get('fanStatus') is not None:
                S['fan_status'] = norm_switch(b.get('fanStatus'), S['fan_status'])
            if b.get('fanSpeed') is not None:
                S['fan_speed'] = clamp(to_int(b.get('fanSpeed'), S['fan_speed']), 0, 100)
        okc += 1
    if isinstance(c, dict):
        if not skip and c.get('lightStatus') is not None:
            S['light_status'] = norm_switch(c.get('lightStatus'), S['light_status'])
        okc += 1
    S['state_error'] = 0 if okc == 3 else 1

def fetch_config():
    d = GET('/gw/homes/%s/configs' % HOME)
    if d is None:
        S['config_error'] = 1
        return
    if isinstance(d, dict) and 'configs' in d and isinstance(d.get('configs'), dict):
        d = d.get('configs')
    if isinstance(d, list):
        try:
            m = {}
            for i in d:
                if isinstance(i, dict) and i.get('key') is not None:
                    m[str(i.get('key'))] = i.get('value')
            d = m
        except:
            S['config_error'] = 1
            return
    if not isinstance(d, dict):
        S['config_error'] = 1
        return
    mp = {'thigh': ('Thigh', to_float), 'tlow': ('Tlow', to_float), 'lhigh': ('Lhigh', to_int), 'llow': ('Llow', to_int), 'tsleepHigh': ('Tsleep_high', to_float), 'tsleepLow': ('Tsleep_low', to_float), 'tawayHigh': ('Taway_high', to_float), 'tcritical': ('Tcritical', to_float), 'autoFanSpeed': ('auto_fan_speed', to_int), 'sleepFanSpeed': ('sleep_fan_speed', to_int), 'awayFanSpeed': ('away_fan_speed', to_int)}
    for k in mp:
        if k in d and d[k] is not None:
            n, c = mp[k]
            v = c(d[k], CFG[n])
            CFG[n] = clamp(v, 0, 100) if 'speed' in n.lower() else v
    S['config_error'] = 0

def queue_alert(tp, msg):
    global PENDING_ALERT
    if KEY['runtime_id'] and PENDING_ALERT is None:
        PENDING_ALERT = (tp, msg)

def send_pending_alert():
    global PENDING_ALERT
    if PENDING_ALERT is None:
        return
    if ir_http_pause():
        return
    tp, msg = PENDING_ALERT
    if POST('/gw/homes/%s/alerts' % HOME, {'deviceId': KEY['runtime_id'], 'sensorId': None, 'type': tp, 'message': msg}):
        PENDING_ALERT = None

def send_alert(tp, msg):
    queue_alert(tp, msg)

def trigger_security_alert(tp, msg):
    S['security_alert_active'] = 1
    S['last_security_alert_ms'] = ms()
    send_alert(tp, msg)

def read_fast_sensor():
    try:
        S['shine'] = clamp(round(translate(pin2.read_analog(), 0, 4095, 0, 100)), 0, 100)
    except:
        S['shine'] = None
    try:
        S['pir_motion'] = 1 if pin16.read_digital() else 0
    except:
        S['pir_motion'] = 0

def read_sensor():
    try:
        dht.read_dht20()
        S['nhiet_do'] = dht.dht20_temperature()
        S['do_am'] = dht.dht20_humidity()
    except:
        S['nhiet_do'] = None
        S['do_am'] = None
    read_fast_sensor()

def sensor_valid():
    t, h, l = (S['nhiet_do'], S['do_am'], S['shine'])
    return not (t is None or h is None or l is None or (t < -10) or (t > 80) or (h < 0) or (h > 100) or (l < 0) or (l > 100))

def update_yolo():
    if not S['server_ready']:
        S['yolo_error'] = 1
        return
    d = GET('/gw/yolo/check_human')
    if not isinstance(d, dict) or d.get('status') != 'success':
        S['yolo_error'] = 1
        S['camera_human_detected'] = 0
        S['camera_human_count'] = 0
        S['camera_motion_detected'] = 0
        S['camera_confidence'] = 0.0
        S['camera_motion_score'] = 0.0
        return
    S['yolo_error'] = 0
    S['camera_human_detected'] = 1 if d.get('human_detected', 0) else 0
    S['camera_human_count'] = to_int(d.get('human_count', 0), 0)
    S['camera_confidence'] = to_float(d.get('max_confidence', 0.0), 0.0)
    S['camera_motion_detected'] = 1 if d.get('motion_detected', 0) else 0
    S['camera_motion_score'] = to_float(d.get('movement_score', 0.0), 0.0)

def combine_motion():
    a = S['camera_motion_detected'] or (S['pir_motion'] and S['camera_human_detected'] and (S['camera_confidence'] >= 0.5))
    if a:
        S['someone'] = 1
        S['last_human_seen_ms'] = ms()
    elif time.ticks_diff(ms(), S['last_human_seen_ms']) > 1800:
        S['someone'] = 0

def set_ir_state(st):
    S['ir_state'] = st
    S['ir_typing'] = 1 if st == 'password_input' else 0
    mark_ir_activity()
    print('IR state=' + st)

def ir_start(now):
    mark_ir_activity()
    S['ir_pass'] = ''
    S['ir_last_input_ms'] = now
    print('IR start password input')
    set_ir_state('password_input')

def ir_cancel():
    mark_ir_activity(1000)
    S['ir_pass'] = ''
    S['ir_last_input_ms'] = 0
    set_ir_state('idle')
    print('IR cancel')

def ir_backspace(now):
    if S['ir_state'] != 'password_input':
        return
    mark_ir_activity()
    if len(S['ir_pass']):
        S['ir_pass'] = S['ir_pass'][:-1]
    S['ir_last_input_ms'] = now
    print('IR backspace buffer=' + '*' * len(S['ir_pass']))

def ir_timeout_check(now):
    if S['ir_state'] == 'password_input' and S['ir_last_input_ms'] and (time.ticks_diff(now, S['ir_last_input_ms']) >= IR_TIMEOUT_MS):
        S['ir_pass'] = ''
        S['ir_last_input_ms'] = 0
        set_ir_state('idle')
        print('IR password timeout')
    if S['ir_state'] == 'locked_out' and S['ir_last_input_ms'] and (time.ticks_diff(now, S['ir_last_input_ms']) >= IR_LOCKOUT_MS):
        S['ir_pass'] = ''
        S['ir_last_input_ms'] = 0
        set_ir_state('idle')

def ir_ok(now):
    if S['ir_state'] != 'password_input':
        return
    mark_ir_activity()
    if len(S['ir_pass']) < len(DOOR_PASS):
        print('IR password incomplete len=%s' % len(S['ir_pass']))
        S['ir_last_input_ms'] = now
        return
    set_ir_state('verifying')
    if S['ir_pass'] == DOOR_PASS:
        S['ir_pass'] = ''
        S['ir_last_input_ms'] = 0
        S['failed_attempts'] = 0
        S['security_alert_active'] = 0
        print('IR password ok')
        open_door()
    else:
        S['failed_attempts'] += 1
        print('IR password wrong failed_attempts=%s' % S['failed_attempts'])
        S['ir_pass'] = ''
        S['ir_last_input_ms'] = now
        if S['failed_attempts'] >= MAX_FAIL:
            trigger_security_alert('WRONG_PASSWORD', 'Nhap sai mat khau %s lan' % S['failed_attempts'])
            set_ir_state('locked_out')
        else:
            set_ir_state('password_input')

def ir_append(ch, now):
    if S['ir_state'] == 'password_input' and len(S['ir_pass']) < len(DOOR_PASS):
        mark_ir_activity()
        S['ir_pass'] += str(ch)
        S['ir_last_input_ms'] = now
        print('IR digit=%s buffer=%s' % (ch, '*' * len(S['ir_pass'])))
        if len(S['ir_pass']) >= len(DOOR_PASS):
            ir_ok(now)

def get_ir_digit(code):
    if code in IR_DIGIT_MAP:
        return IR_DIGIT_MAP[code]
    if code in RAW_IR_DIGIT_MAP:
        return RAW_IR_DIGIT_MAP[code]
    return None

def is_ir_setup(code):
    return code == IR_KEY_SETUP or code == RAW_IR_KEY_SETUP

def is_ir_ok(code):
    return code == IR_KEY_OK or code == RAW_IR_KEY_OK

def is_ir_cancel(code):
    return code == IR_KEY_CANCEL or code == RAW_IR_KEY_CANCEL

def is_ir_back(code):
    return code == IR_KEY_BACK or code == RAW_IR_KEY_BACK

def ir_log(*args):
    if not IR_DEBUG:
        return
    try:
        print(*args)
    except:
        pass

def ir_to_int(c):
    if c is None:
        return None
    try:
        if isinstance(c, int):
            return c
    except:
        pass
    try:
        s = str(c).strip().lower()
        if not s:
            return None
        if s.startswith('0x'):
            return int(s, 16)
        if s.startswith('data:'):
            s = s.split(':', 1)[1].split(',', 1)[0].strip()
            return int(s)
        return int(s)
    except:
        return None

def ir_is_known(c):
    if c is None:
        return False
    known_keys = (IR_KEY_SETUP, RAW_IR_KEY_SETUP, IR_KEY_OK, RAW_IR_KEY_OK, IR_KEY_CANCEL, RAW_IR_KEY_CANCEL, IR_KEY_BACK, RAW_IR_KEY_BACK)
    try:
        if c in IR_DIGIT_MAP or c in RAW_IR_DIGIT_MAP or c in known_keys:
            return True
    except:
        pass
    return False

def normalize_ir_code(token, addr=None, ext=None):
    # Restore the callback-driven IR path from the older working build.
    # Some IR_RX builds return 0 / "Unsupported code" from get_code() when idle,
    # so this version does NOT poll get_code() in the thread.
    for c in (token, ext):
        if c is None:
            continue
        if ir_is_known(c):
            return c
        ic = ir_to_int(c)
        if ir_is_known(ic):
            return ic
    return None

def enqueue_ir_code(code):
    if code is None:
        return
    locked = 0
    try:
        if IR_LOCK:
            IR_LOCK.acquire()
            locked = 1
        if len(IR_Q) >= 12:
            IR_Q.pop(0)
        IR_Q.append(code)
        S['ir_pending'] = code
    except Exception as e:
        try:
            print('IR enqueue err', e)
        except:
            pass
    finally:
        try:
            if locked:
                IR_LOCK.release()
        except:
            pass
    mark_ir_activity()

def pop_ir_code():
    code = None
    locked = 0
    try:
        if IR_LOCK:
            IR_LOCK.acquire()
            locked = 1

        if len(IR_Q):
            # Restore old working behavior: pop the queue first, but keep
            # ir_pending for one extra cycle. The duplicate is filtered by
            # IR_DEBOUNCE_MS, and it keeps HTTP paused long enough for the
            # IR receiver to remain responsive around app fan/light commands.
            code = IR_Q.pop(0)
        elif S.get('ir_pending', None) is not None:
            code = S['ir_pending']
            S['ir_pending'] = None

    except Exception as e:
        try:
            print('IR pop err', e)
        except:
            pass
    finally:
        try:
            if locked:
                IR_LOCK.release()
        except:
            pass
    return code

def on_ir_received(token, addr, ext):
    code = normalize_ir_code(token, addr, ext)
    if code is not None:
        ir_log('IR callback code=', code)
        enqueue_ir_code(code)

def ir_rebind_callback(reason=''):
    # Some YoloBit/IR_RX builds can become quiet after WiFi/HTTP command bursts.
    # Re-binding the callback is cheap and keeps the old queue-driven IR path alive.
    try:
        obj = globals().get('ir', None)
        if obj is None:
            return
        obj.on_received(on_ir_received)
        S['last_ir_bind_ms'] = ms()
        if reason == 'cmd':
            ir_log('IR callback rebound after command')
    except Exception as e:
        try:
            print('IR callback rebind err', e)
        except:
            pass

def run_ir_task(now):
    code = pop_ir_code()
    if code is None:
        return
    mark_ir_activity()
    ir_log('IR code accepted=', code)
    if S['last_ir_code'] == code and time.ticks_diff(now, S['last_ir_ms']) < IR_DEBOUNCE_MS:
        ir_log('IR duplicate ignored code=', code)
        return
    S['last_ir_code'] = code
    S['last_ir_ms'] = now

    d = get_ir_digit(code)

    if S['ir_state'] == 'locked_out':
        if is_ir_cancel(code):
            S['failed_attempts'] = 0
            S['security_alert_active'] = 0
            ir_cancel()
            return
        if is_ir_setup(code) or is_ir_ok(code) or d is not None:
            S['failed_attempts'] = 0
            S['security_alert_active'] = 0
            ir_start(now)
            if d is not None:
                ir_append(d, now)
            return
        print('IR locked_out')
        return

    # Accept several entry paths because some remotes send OK=13 instead of SETUP=21.
    if S['ir_state'] != 'password_input':
        if is_ir_setup(code) or is_ir_ok(code):
            ir_start(now)
            return
        if d is not None:
            ir_start(now)
            ir_append(d, now)
            return
        return

    if is_ir_cancel(code):
        ir_cancel()
        return
    if is_ir_back(code):
        ir_backspace(now)
        return
    if is_ir_ok(code):
        ir_ok(now)
        return
    if d is not None:
        ir_append(d, now)

def run_door_task(now):
    # Safe door handler for MicroPython _thread.
    # Do not put time.ticks_diff() directly in the if condition because
    # some YoloBit builds can throw when door_open_until is not an int yet.
    if 'last_door_hw' not in S:
        S['last_door_hw'] = None
    if 'door_open' not in S:
        S['door_open'] = 0
    if 'door_open_until' not in S:
        S['door_open_until'] = None

    door_open = 1 if S.get('door_open', 0) else 0
    door_until = S.get('door_open_until', None)

    if door_open and door_until is not None:
        expired = False
        try:
            expired = time.ticks_diff(now, door_until) >= 0
        except Exception as e:
            # If the timeout value is invalid, close for safety instead of
            # crashing the IR thread forever.
            try:
                print('DOOR timeout err', e, 'until=', door_until)
            except:
                pass
            expired = True

        if expired:
            close_door()
            door_open = 1 if S.get('door_open', 0) else 0

    last_door_hw = S.get('last_door_hw', None)
    if door_open != last_door_hw:
        door_hw(door_open)
        S['last_door_hw'] = door_open

def ir_thread_loop():
    print('IR thread loop started')
    last_thread_err = ''
    last_thread_err_ms = 0
    while True:
        now = ms()
        try:
            run_ir_task(now)
            ir_timeout_check(now)
            run_door_task(now)
        except Exception as e:
            # Do not spam serial every 12 ms; print the same error at most once per second.
            try:
                se = str(e)
                if se != last_thread_err or time.ticks_diff(now, last_thread_err_ms) >= 1000:
                    print('IR thread err', e)
                    last_thread_err = se
                    last_thread_err_ms = now
            except:
                pass
        try:
            time.sleep_ms(IR_THREAD_SLEEP_MS)
        except:
            time.sleep(0.012)

def start_ir_thread():
    global IR_THREAD_STARTED
    if IR_THREAD_STARTED:
        return True
    if not THREAD_OK:
        print('IR thread unavailable: _thread import failed')
        return False
    try:
        _thread.start_new_thread(ir_thread_loop, ())
        IR_THREAD_STARTED = True
        print('IR thread started')
        return True
    except Exception as e:
        print('IR thread start failed', e)
        IR_THREAD_STARTED = False
        return False

def compute_local_fallback():
    fs, sp, ls = ('off', 0, 'off')
    if not sensor_valid():
        S['sensor_error'] = 1
        S['local_fan_status'] = 'off'
        S['local_fan_speed'] = 0
        S['local_light_status'] = 'off'
        return
    S['sensor_error'] = 0
    t = S['nhiet_do']
    l = S['shine']
    m = S['mode']
    if m == 'sleep':
        if t is not None and t >= CFG['Tsleep_high']:
            fs, sp = ('on', CFG['sleep_fan_speed'])
        elif t is not None and t <= CFG['Tsleep_low']:
            fs, sp = ('off', 0)
    elif m == 'away':
        if t is not None and t >= CFG['Taway_high']:
            fs, sp = ('on', CFG['away_fan_speed'])
    elif m == 'auto':
        if t is not None and t >= CFG['Thigh']:
            fs, sp = ('on', CFG['auto_fan_speed'])
        elif t is not None and t <= CFG['Tlow']:
            fs, sp = ('off', 0)
        if l is not None and l <= CFG['Llow']:
            ls = 'on'
        elif l is not None and l >= CFG['Lhigh']:
            ls = 'off'
    elif m == 'manual':
        fs, sp, ls = (S['fan_status'], S['fan_speed'], S['light_status'])
    S['local_fan_status'] = fs
    S['local_fan_speed'] = clamp(to_int(sp, 0), 0, 100)
    S['local_light_status'] = ls

def alert_debounced(raw, since_key, grace_ms):
    if raw:
        if not S.get(since_key):
            S[since_key] = ms()
            return 0
        return 1 if time.ticks_diff(ms(), S.get(since_key)) >= grace_ms else 0
    S[since_key] = 0
    return 0

def apply_logic():
    compute_local_fallback()
    fs = active_fan_status()
    sp = active_fan_speed()
    ls = active_light_status()
    if fs not in ('on', 'off'):
        fs = 'off'
    if ls not in ('on', 'off'):
        ls = 'off'
    sp = clamp(to_int(sp, 0), 0, 100)
    if fs == 'off':
        sp = 0
    elif sp <= 0:
        sp = CFG['sleep_fan_speed'] if S['mode'] == 'sleep' else CFG['away_fan_speed'] if S['mode'] == 'away' else CFG['auto_fan_speed']
    fh = (fs, sp)
    if fh != S['last_fan_hw']:
        fan_hw(fs == 'on', sp)
        S['last_fan_hw'] = fh
    if ls != S['last_light_hw']:
        try:
            pin8.write_digital(1 if ls == 'on' else 0)
        except:
            try:
                pin1.write_digital(1 if ls == 'on' else 0)
            except:
                pass
        S['last_light_hw'] = ls
    temp_raw = S['nhiet_do'] is not None and S['nhiet_do'] > CFG['Tcritical']
    away_raw = S['mode'] == 'away' and S['someone']
    temp_alert = alert_debounced(temp_raw, 'temp_alert_since', ALERT_TEMP_GRACE_MS)
    away_alert = alert_debounced(away_raw, 'away_alert_since', ALERT_AWAY_GRACE_MS)
    S['alert_active'] = 1 if temp_alert or away_alert or S['security_alert_active'] else 0

def send_one_telemetry():
    global tel_i
    if not net_ready():
        return
    for _ in range(len(TEL)):
        kn, tp, sn = TEL[tel_i]
        tel_i = (tel_i + 1) % len(TEL)
        dk = KEY.get(kn)
        v = 1 if sn == 'someone' and S[sn] else S.get(sn)
        if dk is None or v is None:
            continue
        S['telemetry_error'] = 0 if POST('/gw/device-telemetry', {'deviceKey': dk, 'sensorType': tp, 'value': v}) else 1
        return
    S['telemetry_error'] = 1

def run_telemetry_task(now):
    if ir_http_pause():
        return False
    if sensor_valid() and due('telemetry'):
        send_one_telemetry()
        done('telemetry')
        return True
    return False

def normalize_target(x):
    return {'mode': 'mode', 'power': 'power', 'fan': 'fan', 'fanstatus': 'fan', 'fan_status': 'fan', 'fanspeed': 'fan_speed', 'fan_speed': 'fan_speed', 'speed': 'fan_speed', 'light': 'light', 'lightstatus': 'light', 'light_status': 'light', 'brightness': 'brightness', 'lightlevel': 'brightness', 'light_level': 'brightness'}.get(norm(x).replace('-', '_'), norm(x).replace('-', '_'))

def mark_cmd_applied():
    S['last_cmd_apply_ms'] = ms()

    # After fan/light/mode commands from the app, give IR a short local-only
    # recovery window and re-bind the callback. This prevents the first password
    # key from being missed while the board is busy with HTTP/ACK/state sync.
    try:
        S['net_block_until'] = time.ticks_add(ms(), 2500)
    except:
        pass
    try:
        S['ir_quiet_until'] = time.ticks_add(ms(), 2500)
    except:
        pass
    try:
        ir_rebind_callback('cmd')
    except:
        pass

def queue_ack(device_key, cid):
    if not (device_key and cid):
        return
    for k, i in ACKQ:
        if k == device_key and i == cid:
            return
    if len(ACKQ) >= 6:
        ACKQ.pop(0)
    ACKQ.append((device_key, cid))

def flush_acks():
    if not ACKQ:
        return
    if ir_http_pause():
        return
    t0 = ms() if PERF_LOG else 0
    if POST('/gw/commands/ack', {'acks': [{'deviceKey': k, 'id': i} for k, i in ACKQ]}):
        ACKQ[:] = []
        if PERF_LOG:
            PERF['ack_ms'] = time.ticks_diff(ms(), t0)

def ack(device_key, cid):
    queue_ack(device_key, cid)
    return True

def cmd_runtime(c):
    t0 = ms() if PERF_LOG else 0
    if not isinstance(c, dict):
        return
    cid = c.get('id')
    if cid is None:
        return
    try:
        if normalize_target(c.get('target')) == 'mode' and c.get('value') is not None:
            m = norm(c.get('value'))
            if m in ('auto', 'manual', 'sleep', 'away'):
                S['prev_mode'] = S['mode']
                S['mode'] = m
                mark_cmd_applied()
                apply_logic()
                if PERF_LOG:
                    PERF['cmd_apply_ms'] = time.ticks_diff(ms(), t0)
    except Exception as e:
        try:
            print('cmd_runtime err', e)
        except:
            pass
    ack(KEY['runtime_key'], cid)

def cmd_fan(c):
    t0 = ms() if PERF_LOG else 0
    if not isinstance(c, dict):
        return
    cid = c.get('id')
    if cid is None:
        return
    try:
        t = normalize_target(c.get('target'))
        v = c.get('value')
        applied = 0
        if t in ('fan', 'power'):
            if v is not None:
                S['fan_status'] = norm_switch(v, S['fan_status'])
                if S['fan_status'] == 'off':
                    S['fan_speed'] = 0
                mark_cmd_applied()
                applied = 1
        elif t in ('fan_speed', 'speed'):
            sp = clamp(to_int(v, S['fan_speed']), 0, 100)
            S['fan_speed'] = sp
            S['fan_status'] = 'on' if sp > 0 else 'off'
            mark_cmd_applied()
            applied = 1
        if applied:
            apply_logic()
            if PERF_LOG:
                PERF['cmd_apply_ms'] = time.ticks_diff(ms(), t0)
    except Exception as e:
        try:
            print('cmd_fan err', e)
        except:
            pass
    ack(KEY['fan_key'], cid)

def cmd_light(c):
    t0 = ms() if PERF_LOG else 0
    if not isinstance(c, dict):
        return
    cid = c.get('id')
    if cid is None:
        return
    try:
        t = normalize_target(c.get('target'))
        v = c.get('value')
        applied = 0
        if t in ('light', 'power'):
            if v is not None:
                S['light_status'] = norm_switch(v, S['light_status'])
                mark_cmd_applied()
                applied = 1
        elif t == 'brightness':
            lv = clamp(to_int(v, 0), 0, 100)
            S['light_status'] = 'on' if lv > 0 else 'off'
            mark_cmd_applied()
            applied = 1
        if applied:
            apply_logic()
            if PERF_LOG:
                PERF['cmd_apply_ms'] = time.ticks_diff(ms(), t0)
    except Exception as e:
        try:
            print('cmd_light err', e)
        except:
            pass
    ack(KEY['light_key'], cid)

def fetch_all_cmds():
    had = 0
    d = GET('/gw/commands/next?keys=%s,%s,%s' % (KEY['runtime_key'], KEY['fan_key'], KEY['light_key']))
    if not isinstance(d, dict):
        S['command_error'] = 1
        return
    for k, f in ((KEY['runtime_key'], cmd_runtime), (KEY['fan_key'], cmd_fan), (KEY['light_key'], cmd_light)):
        c = d.get(k)
        if c is None:
            continue
        if not isinstance(c, dict):
            if DEBUG:
                print('cmd ignored non-dict', k, c)
            continue
        try:
            f(c)
        except Exception as e:
            had = 1
            try:
                print('cmd apply err', k, e)
            except:
                pass
    S['command_error'] = had

def print_status():
    if not STATUS_LOG:
        return
    print('mode=', S['mode'], 'T=', S['nhiet_do'], 'H=', S['do_am'], 'L=', S['shine'], 'someone=', S['someone'], 'fan=', active_fan_status(), active_fan_speed(), 'light=', active_light_status(), 'door=', S['door_open'], 'ir=', S['ir_state'], 'err state/config/cmd/reg/sen=', S['state_error'], S['config_error'], S['command_error'], S['registry_error'], S['sensor_error'], 'raw_srv=', raw_server_error(), 'srv_led=', server_error(), 'ids=', KEY['runtime_id'], KEY['fan_id'], KEY['light_id'], 'http=', S['consecutive_http_fail'])

def perf_tick(d):
    if not PERF_LOG:
        return
    PERF['loop_sum'] += d
    PERF['loop_n'] += 1
    if d > PERF['loop_max']:
        PERF['loop_max'] = d
    now = ms()
    if time.ticks_diff(now, PERF['last']) >= 3000:
        try:
            PERF['mem'] = gc.mem_free()
        except:
            PERF['mem'] = 0
        avg = PERF['loop_sum'] // PERF['loop_n'] if PERF['loop_n'] else 0
        print('PERF loop', avg, PERF['loop_max'], 'http', PERF['http_ms'], PERF['http_max'], PERF['http_path'], 'cmd', PERF['cmd_apply_ms'], 'ack', PERF['ack_ms'], 'mem', PERF['mem'])
        PERF['loop_sum'] = 0
        PERF['loop_n'] = 0
        PERF['loop_max'] = 0
        PERF['http_max'] = 0
        PERF['last'] = now

def due(k):
    return time.ticks_diff(ms(), LAST[k]) >= ITV[k]

def done(k):
    LAST[k] = ms()

def run_backend_task(now):
    if ACKQ and due('ack'):
        flush_acks()
        done('ack')
        return True
    if PENDING_ALERT is not None and due('alert'):
        send_pending_alert()
        done('alert')
        return True
    if due('command'):
        fetch_all_cmds()
        done('command')
        return True
    if due('state'):
        fetch_all_states()
        done('state')
        return True
    if due('yolo'):
        if S['server_ready']:
            try:
                update_yolo()
            except:
                S['yolo_error'] = 1
        done('yolo')
        return True
    if due('config'):
        fetch_config()
        done('config')
        return True
    if due('registry'):
        load_registry()
        done('registry')
        return True
    return False
ir = IR_RX(Pin(pin10.pin, Pin.IN))
ir.start()
try:
    ir.on_received(on_ir_received)
except Exception as e:
    try:
        print('IR callback setup err', e)
    except:
        pass
start_ir_thread()
display.scroll('IoT')
try:
    print('GATEWAY BASE =', BASE)
except:
    pass
lcd.backlight_on()
lcd.clear()
lcd.move_to(0, 0)
lcd.putstr('Smart House')
lcd.move_to(0, 1)
lcd.putstr('Booting...')
S['boot_ms'] = ms()
close_door()
update_leds()
update_lcd()
try:
    mqtt.connect_wifi(WIFI_SSID, WIFI_PASS)
except Exception as e:
    if DEBUG:
        print('wifi err', e)
time.sleep_ms(400)
try:
    load_registry()
except:
    S['registry_error'] = 1
try:
    fetch_config()
except:
    S['config_error'] = 1
for _ in range(1):
    try:
        fetch_all_states()
    except:
        S['state_error'] = 1
try:
    yh = GET('/gw/yolo/health')
    if yh is not None:
        S['server_ready'] = 1
except:
    pass
lcd.clear()
lcd.move_to(0, 0)
lcd.putstr('Smart House')
lcd.move_to(0, 1)
lcd.putstr('Running...')
update_lcd()
while True:
    loop_start = ms()
    now = loop_start
    if time.ticks_diff(now, S['last_gc']) > 12000:
        try:
            gc.collect()
            S['last_gc'] = now
        except:
            pass
    if time.ticks_diff(now, LAST['fast_sensor']) >= ITV['fast_sensor']:
        LAST['fast_sensor'] = now
        read_fast_sensor()
    if time.ticks_diff(now, LAST['sensor']) >= ITV['sensor']:
        LAST['sensor'] = now
        read_sensor()

    # Periodic silent callback rebind. It is a safeguard only; normal IR
    # processing still uses callback -> queue -> IR thread.
    try:
        if time.ticks_diff(now, S.get('last_ir_bind_ms', 0)) >= 20000:
            ir_rebind_callback('periodic')
    except:
        pass

    if not IR_THREAD_STARTED:
        run_ir_task(now)
        ir_timeout_check(now)
        run_door_task(now)
    combine_motion()
    if S['security_alert_active'] and time.ticks_diff(now, S['last_security_alert_ms']) >= 15000:
        S['security_alert_active'] = 0
    apply_logic()
    if time.ticks_diff(now, LAST['led']) >= ITV['led']:
        LAST['led'] = now
        update_leds()
    if time.ticks_diff(now, LAST['display']) >= ITV['display']:
        LAST['display'] = now
        update_lcd()
    if S['nhiet_do'] is not None and S['nhiet_do'] > CFG['Tcritical'] and (time.ticks_diff(now, temp_alert_last) >= 15000):
        send_alert('HIGH_TEMPERATURE', 'Nhiet do cao: %s' % round(S['nhiet_do'], 1))
        temp_alert_last = now
    if server_error():
        ITV['state'], ITV['command'], ITV['telemetry'], ITV['yolo'] = (60000, 4000, 15000, 45000)
    else:
        ITV['state'], ITV['command'], ITV['telemetry'], ITV['yolo'] = (45000, 2500, 10000, 30000)
    if net_ready():
        if not run_backend_task(now):
            run_telemetry_task(now)
    if time.ticks_diff(now, LAST['status']) >= ITV['status']:
        print_status()
        LAST['status'] = now
    perf_tick(time.ticks_diff(ms(), loop_start))
    time.sleep_ms(4)