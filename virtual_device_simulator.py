# virtual_device_simulator.py
# Mô phỏng YoLoBit/OhStem nhưng không dùng phần cứng.
# Registry, config, state, command, telemetry vẫn gọi backend y như file gốc.

import requests
import random
import time
import math
from datetime import datetime

# ===== SERVER =====
SERVER_HOST = '192.168.1.175'
SERVER_PORT = 8080
BASE = 'http://{}:{}'.format(SERVER_HOST, SERVER_PORT)

HOME_ID = 1
DEVICE_NAME = 'OhStem Living Room'

# ===== CONFIG =====
DOOR_PASSWORD = '123456'
MAX_FAILED_ATTEMPTS = 3
DOOR_OPEN_MS = 5000

CFG = {
    'Thigh': 30.0, 'Tlow': 27.0, 'Lhigh': 55, 'Llow': 35,
    'Tsleep_high': 32.0, 'Tsleep_low': 26.0, 'Taway_high': 33.0,
    'Tcritical': 35.0, 'N_minutes': 2, 'M_minutes': 2, 'Thold_minutes': 5,
    'auto_fan_speed': 70, 'sleep_fan_speed': 30, 'away_fan_speed': 60
}

SYS = {
    'mode': 'away', 'prev_mode': None, 'hold_until': None,
    'fan_status': 'off', 'fan_speed': 0, 'light_status': 'off',
    'sensor_error': False, 'state_error': False, 'config_error': False,
    'telemetry_error': False, 'command_error': False, 'registry_error': False,
    'alert_active': False, 'security_alert_active': False,
    'nhiet_do': 0, 'do_am': 0, 'shine': 0, 'someone': False,
    'door_locked': True, 'door_open': False, 'door_open_until': None,
    'ir_typing': False, 'ir_pass': '', 'failed_attempts': 0,
    'last_security_alert_ms': 0, 'last_lcd_1': '', 'last_lcd_2': ''
}

DEV = {
    'runtime': None, 'fan': None, 'light': None, 'temp': None,
    'humidity': None, 'light_sensor': None, 'motion': None
}

KEYS = {
    'runtime_id': None, 'runtime_key': None,
    'fan_id': None, 'fan_key': None,
    'light_id': None, 'light_key': None,
    'temp_key': 'ohstem-temp-01',
    'humidity_key': 'ohstem-humidity-01',
    'light_sensor_key': 'ohstem-light-01',
    'motion_key': 'ohstem-motion-01'
}

DEVICE_RULES = {
    'runtime': {
        'exact': ['yolobit-01'],
        'type': ['SENSOR_NODE', 'HUB', 'OTHER', 'CONTROLLER'],
        'key': ['yolobit', 'controller', 'hub'],
        'name': ['controller', 'hub', 'trung tam', 'điều khiển', 'dieu khien']
    },
    'fan': {
        'exact': ['ohstem-fan-ctrl-01'],
        'type': ['FAN'],
        'key': ['fan', 'quat'],
        'name': ['fan', 'quạt', 'quat']
    },
    'light': {
        'exact': ['ohstem-light-ctrl-01'],
        'type': ['LIGHT'],
        'key': ['light', 'den'],
        'name': ['light', 'đèn', 'den']
    },
    'temp': {
        'exact': ['ohstem-temp-01'],
        'key': ['temp', 'temperature', 'nhiet'],
        'name': ['temp', 'temperature', 'nhiệt', 'nhiet']
    },
    'humidity': {
        'exact': ['ohstem-humidity-01'],
        'key': ['humidity', 'humid', 'do-am'],
        'name': ['humidity', 'humid', 'độ ẩm', 'do am']
    },
    'light_sensor': {
        'exact': ['ohstem-light-01']
    },
    'motion': {
        'exact': ['ohstem-motion-01'],
        'key': ['motion', 'pir', 'presence'],
        'name': ['motion', 'pir', 'hiện diện', 'hien dien', 'chuyển động', 'chuyen dong']
    }
}

# ===== UTILS =====
def unwrap(x):
    return x.get('data') if isinstance(x, dict) and 'data' in x else x

def clamp(v, a, b):
    return a if v < a else b if v > b else v

def to_int(v, d=0):
    try:
        return int(v)
    except Exception:
        try:
            return int(float(v))
        except Exception:
            return d

def to_float(v, d=0.0):
    try:
        return float(v)
    except Exception:
        return d

def norm(v):
    return '' if v is None else str(v).strip().lower()

def norm_type(v):
    return '' if v is None else str(v).strip().upper()

def contains(text, keys):
    t = norm(text)
    for k in keys:
        if norm(k) in t:
            return True
    return False

def has_server_error():
    return (
        SYS['state_error']
        or SYS['config_error']
        or SYS['telemetry_error']
        or SYS['command_error']
        or SYS['registry_error']
    )

def http_get(path):
    try:
        resp = requests.get(BASE + path, timeout=10)
        try:
            data = resp.json()
        except Exception:
            print('GET JSON loi:', path, resp.text)
            return None
        return unwrap(data)
    except Exception as e:
        print('GET loi', path, e)
        return None

def http_post(path, payload):
    try:
        resp = requests.post(BASE + path, json=payload, timeout=10)
        ok = 200 <= resp.status_code < 300
        return ok, resp.text
    except Exception as e:
        print('POST loi', path, e)
        return False, None

# ===== HARDWARE MOCK =====
def batquat(on, speed):
    print('[MOCK FAN]', 'ON' if on else 'OFF', 'speed=', speed)

def batden(on):
    print('[MOCK LIGHT]', 'ON' if on else 'OFF')

def set_door_relay(is_open):
    print('[MOCK DOOR RELAY]', 'OPEN' if is_open else 'CLOSE')

def update_lcd():
    t = '--' if SYS['nhiet_do'] is None else str(round(SYS['nhiet_do'], 1))
    h = '--' if SYS['do_am'] is None else str(round(SYS['do_am'], 1))
    l = '--' if SYS['shine'] is None else str(SYS['shine'])
    print('[MOCK LCD]', 'T:', t, 'H:', h, 'L:', l,
          'mode=', SYS['mode'],
          'door_locked=', SYS['door_locked'],
          'door_open=', SYS['door_open'])

def update_status_leds():
    print('[MOCK LED]',
          'sensor_error=', SYS['sensor_error'],
          'server_error=', has_server_error(),
          'alert=', SYS['alert_active'],
          'security_alert=', SYS['security_alert_active'])

def process_ir():
    # Không xử lý IR thật trên simulator để tránh lỗi thiết bị
    return

# ===== DOOR =====
def open_door():
    SYS['door_open'] = True
    SYS['door_locked'] = False
    SYS['door_open_until'] = time.time() + (DOOR_OPEN_MS / 1000.0)
    set_door_relay(True)

def close_door():
    SYS['door_open'] = False
    SYS['door_locked'] = True
    SYS['door_open_until'] = None
    SYS['ir_typing'] = False
    SYS['ir_pass'] = ''
    set_door_relay(False)

def update_door_auto_close():
    if SYS['door_open'] and SYS['door_open_until'] is not None:
        if time.time() >= SYS['door_open_until']:
            close_door()

# ===== DEVICE REGISTRY =====
def device_match(d, rule):
    if d is None or rule is None:
        return False
    if 'type' in rule and norm_type(d.get('type') or d.get('class') or d.get('subtype')) in rule['type']:
        return True
    if 'key' in rule and contains(d.get('deviceKey') or d.get('device_key'), rule['key']):
        return True
    if 'name' in rule and contains(d.get('name'), rule['name']):
        return True
    return False

def get_device_key(d):
    return d.get('deviceKey') or d.get('device_key')

def find_exact(devices, keys):
    keys = [norm(k) for k in keys]
    for d in devices:
        if norm(get_device_key(d)) in keys:
            return d
    return None

def find_by_rule(devices, name):
    rule = DEVICE_RULES.get(name, {})
    d = find_exact(devices, rule.get('exact', []))
    if d is not None:
        return d
    for x in devices:
        if device_match(x, rule):
            if name == 'light_sensor' and norm(get_device_key(x)) == 'ohstem-light-01':
                return x
            if name != 'light_sensor':
                return x
    return devices[0] if name == 'runtime' and devices else None

def load_device_registry():
    data = http_get('/api/devices/home/' + str(HOME_ID))
    if not isinstance(data, list) or not data:
        SYS['registry_error'] = True
        print('Registry invalid:', data)
        return False

    DEV['runtime'] = find_by_rule(data, 'runtime')
    DEV['fan'] = find_by_rule(data, 'fan')
    DEV['light'] = find_by_rule(data, 'light')
    DEV['temp'] = find_by_rule(data, 'temp')
    DEV['humidity'] = find_by_rule(data, 'humidity')
    DEV['light_sensor'] = find_by_rule(data, 'light_sensor')
    DEV['motion'] = find_by_rule(data, 'motion')

    if DEV['runtime'] is None or DEV['fan'] is None or DEV['light'] is None:
        SYS['registry_error'] = True
        print('Registry thieu device bat buoc')
        return False

    KEYS['runtime_id'] = DEV['runtime'].get('id')
    KEYS['runtime_key'] = get_device_key(DEV['runtime'])

    KEYS['fan_id'] = DEV['fan'].get('id')
    KEYS['fan_key'] = get_device_key(DEV['fan'])

    KEYS['light_id'] = DEV['light'].get('id')
    KEYS['light_key'] = get_device_key(DEV['light'])

    if DEV['temp']:
        KEYS['temp_key'] = get_device_key(DEV['temp'])
    if DEV['humidity']:
        KEYS['humidity_key'] = get_device_key(DEV['humidity'])
    if DEV['light_sensor']:
        KEYS['light_sensor_key'] = get_device_key(DEV['light_sensor'])
    if DEV['motion']:
        KEYS['motion_key'] = get_device_key(DEV['motion'])

    SYS['registry_error'] = False
    print('registry ok', KEYS)
    return True

# ===== ALERT =====
def send_security_alert(reason, detail):
    if KEYS['runtime_id'] is None:
        print('Bo qua security alert do chua co registry')
        return
    payload = {
        "deviceId": KEYS['runtime_id'],
        "sensorId": None,
        "type": reason,
        "message": detail
    }
    success, text = http_post('/api/homes/' + str(HOME_ID) + '/alerts', payload)
    print('security alert', 'ok' if success else 'fail', text)

def trigger_security_alert(reason, detail):
    SYS['security_alert_active'] = True
    SYS['last_security_alert_ms'] = int(time.time() * 1000)
    print('SECURITY ALERT:', reason, detail)
    send_security_alert(reason, detail)

def clear_security_alert_if_needed():
    if SYS['security_alert_active']:
        now_ms = int(time.time() * 1000)
        if now_ms - SYS['last_security_alert_ms'] >= 15000:
            SYS['security_alert_active'] = False

# ===== SENSOR FAKE DATA =====
_start = time.time()
_last_motion_flip = time.time()

def fake_temperature():
    elapsed = time.time() - _start
    base = 29.0 + 2.7 * math.sin(elapsed / 25.0)
    noise = random.uniform(-0.3, 0.3)
    return round(base + noise, 2)

def fake_humidity():
    elapsed = time.time() - _start
    base = 68.0 + 7.0 * math.sin(elapsed / 35.0 + 1.2)
    noise = random.uniform(-1.0, 1.0)
    return round(clamp(base + noise, 40, 90), 2)

def fake_light():
    elapsed = time.time() - _start
    base = 48.0 + 30.0 * math.sin(elapsed / 20.0 - 0.7)
    noise = random.uniform(-4.0, 4.0)
    return int(clamp(round(base + noise), 0, 100))

def fake_motion():
    global _last_motion_flip
    if time.time() - _last_motion_flip > random.uniform(6, 15):
        _last_motion_flip = time.time()
        return random.choice([True, False, False, False, True])
    return SYS['someone']

def read_sensor():
    try:
        SYS['nhiet_do'] = fake_temperature()
        SYS['do_am'] = fake_humidity()
        SYS['shine'] = fake_light()
        SYS['someone'] = fake_motion()
    except Exception as e:
        print('Loi fake sensor:', e)
        SYS['nhiet_do'] = None
        SYS['do_am'] = None
        SYS['shine'] = None
        SYS['someone'] = False

def sensor_is_valid():
    t, h, l = SYS['nhiet_do'], SYS['do_am'], SYS['shine']
    return not (t is None or h is None or l is None or t < -10 or t > 80 or h < 0 or h > 100 or l < 0 or l > 100)

# ===== STATE / CONFIG =====
def fetch_state_by_device_id(device_id):
    return None if device_id is None else http_get('/api/devices/' + str(device_id) + '/state')

def fetch_device_state():
    ok_all = True

    s = fetch_state_by_device_id(KEYS['runtime_id'])
    if isinstance(s, dict):
        if s.get('mode') is not None:
            SYS['mode'] = str(s.get('mode')).lower()
        SYS['hold_until'] = s.get('holdUntil')
        SYS['prev_mode'] = s.get('prevMode')
    else:
        ok_all = False

    s = fetch_state_by_device_id(KEYS['fan_id'])
    if isinstance(s, dict):
        if s.get('fanStatus') is not None:
            SYS['fan_status'] = str(s.get('fanStatus')).lower()
        if s.get('fanSpeed') is not None:
            SYS['fan_speed'] = clamp(to_int(s.get('fanSpeed'), SYS['fan_speed']), 0, 100)
    else:
        ok_all = False

    s = fetch_state_by_device_id(KEYS['light_id'])
    if isinstance(s, dict):
        if s.get('lightStatus') is not None:
            SYS['light_status'] = str(s.get('lightStatus')).lower()
    else:
        ok_all = False

    SYS['state_error'] = not ok_all

def fetch_config():
    data = http_get('/api/homes/' + str(HOME_ID) + '/configs')
    if not isinstance(data, dict):
        SYS['config_error'] = True
        print('config invalid:', data)
        return

    map_cfg = {
        'thigh': ('Thigh', to_float), 'tlow': ('Tlow', to_float),
        'lhigh': ('Lhigh', to_int), 'llow': ('Llow', to_int),
        'tsleepHigh': ('Tsleep_high', to_float), 'tsleepLow': ('Tsleep_low', to_float),
        'tawayHigh': ('Taway_high', to_float), 'tcritical': ('Tcritical', to_float),
        'nMinutes': ('N_minutes', to_int), 'mMinutes': ('M_minutes', to_int),
        'tholdMinutes': ('Thold_minutes', to_int),
        'autoFanSpeed': ('auto_fan_speed', to_int),
        'sleepFanSpeed': ('sleep_fan_speed', to_int),
        'awayFanSpeed': ('away_fan_speed', to_int)
    }

    for k, rule in map_cfg.items():
        name, caster = rule
        if k in data and data[k] is not None:
            v = caster(data[k], CFG[name])
            CFG[name] = clamp(v, 0, 100) if 'Speed' in name else v

    SYS['config_error'] = False

# ===== MODE LOGIC =====
def apply_mode_logic():
    if not sensor_is_valid():
        SYS['sensor_error'] = True
        SYS['alert_active'] = False
        SYS['fan_status'], SYS['fan_speed'], SYS['light_status'] = 'off', 0, 'off'
        batquat(False, 0)
        batden(False)
        return

    SYS['sensor_error'] = False
    if SYS['fan_status'] not in ['on', 'off']:
        SYS['fan_status'] = 'off'
    if SYS['light_status'] not in ['on', 'off']:
        SYS['light_status'] = 'off'
    SYS['fan_speed'] = clamp(to_int(SYS['fan_speed'], 0), 0, 100)

    if SYS['mode'] != 'manual':
        fan_status, fan_speed, light_status = 'off', 0, 'off'
        t, l = SYS['nhiet_do'], SYS['shine']

        if SYS['mode'] == 'sleep':
            if t is not None and t >= CFG['Tsleep_high']:
                fan_status, fan_speed = 'on', CFG['sleep_fan_speed']
            elif t is not None and t <= CFG['Tsleep_low']:
                fan_status, fan_speed = 'off', 0

        elif SYS['mode'] == 'away':
            if t is not None and t >= CFG['Taway_high']:
                fan_status, fan_speed = 'on', CFG['away_fan_speed']

        else:
            if t is not None and t >= CFG['Thigh']:
                fan_status, fan_speed = 'on', CFG['auto_fan_speed']
            elif t is not None and t <= CFG['Tlow']:
                fan_status, fan_speed = 'off', 0

            if l is not None and l <= CFG['Llow']:
                light_status = 'on'
            elif l is not None and l >= CFG['Lhigh']:
                light_status = 'off'

        SYS['fan_status'], SYS['fan_speed'], SYS['light_status'] = fan_status, fan_speed, light_status

    if SYS['fan_status'] == 'on' and SYS['fan_speed'] <= 0:
        SYS['fan_speed'] = (
            CFG['sleep_fan_speed'] if SYS['mode'] == 'sleep'
            else CFG['away_fan_speed'] if SYS['mode'] == 'away'
            else CFG['auto_fan_speed']
        )

    batquat(SYS['fan_status'] == 'on', SYS['fan_speed'] if SYS['fan_status'] == 'on' else 0)
    batden(SYS['light_status'] == 'on')

    temp_critical = SYS['nhiet_do'] is not None and SYS['nhiet_do'] > CFG['Tcritical']
    away_motion = SYS['mode'] == 'away' and SYS['someone']
    SYS['alert_active'] = temp_critical or away_motion or SYS['security_alert_active']

# ===== TELEMETRY =====
def send_one_telemetry(device_key, sensor_type, value):
    payload = {"deviceKey": device_key, "sensorType": sensor_type, "value": value}
    success, text = http_post('/api/device-telemetry', payload)
    SYS['telemetry_error'] = not success
    print('telemetry', sensor_type, 'ok' if success else 'fail', text)

def send_telemetry():
    items = [
        (KEYS['temp_key'], 'temperature', SYS['nhiet_do']),
        (KEYS['humidity_key'], 'humidity', SYS['do_am']),
        (KEYS['light_sensor_key'], 'light', SYS['shine']),
        (KEYS['motion_key'], 'motion', bool(SYS['someone']))
    ]
    for device_key, sensor_type, value in items:
        if value is not None:
            send_one_telemetry(device_key, sensor_type, value)

# ===== COMMAND =====
def fetch_next_command(device_key):
    if device_key is None:
        return None
    data = http_get('/api/v1/device/' + device_key + '/commands/next')
    return data if isinstance(data, dict) and data.get('id') is not None else None

def ack_command(device_key, command_id):
    if device_key is None or command_id is None:
        return False
    success, text = http_post('/api/v1/device/' + device_key + '/commands/ack', {"id": command_id})
    print('ack', device_key, 'ok' if success else 'fail', text)
    return success

def normalize_target(t):
    t = norm(t).replace('-', '_')
    return {
        'fanstatus': 'fan', 'fan_status': 'fan',
        'fanspeed': 'fan_speed', 'fan_speed': 'fan_speed',
        'lightstatus': 'light', 'light_status': 'light',
        'lightlevel': 'light_level', 'light_level': 'light_level'
    }.get(t, t)

def process_runtime_command(cmd):
    cid, target, value = cmd.get('id'), normalize_target(cmd.get('target')), cmd.get('value')
    if cid is None:
        return False

    print('[MOCK COMMAND runtime]', cmd)

    if target == 'mode' and value is not None:
        SYS['prev_mode'] = SYS['mode']
        SYS['mode'] = str(value).lower()

    return ack_command(KEYS['runtime_key'], cid)

def process_fan_command(cmd):
    cid, target, value = cmd.get('id'), normalize_target(cmd.get('target')), cmd.get('value')
    if cid is None:
        return False

    print('[MOCK COMMAND fan]', cmd)

    if target == 'fan':
        if value is not None:
            SYS['fan_status'] = str(value).lower()
            SYS['fan_speed'] = 0 if SYS['fan_status'] == 'off' else (50 if SYS['fan_speed'] <= 0 else SYS['fan_speed'])
    elif target == 'fan_speed':
        SYS['fan_speed'] = clamp(to_int(value, SYS['fan_speed']), 0, 100)
        SYS['fan_status'] = 'on' if SYS['fan_speed'] > 0 else 'off'

    return ack_command(KEYS['fan_key'], cid)

def process_light_command(cmd):
    cid, target, value = cmd.get('id'), normalize_target(cmd.get('target')), cmd.get('value')
    if cid is None:
        return False

    print('[MOCK COMMAND light]', cmd)

    if target == 'light' and value is not None:
        SYS['light_status'] = str(value).lower()

    return ack_command(KEYS['light_key'], cid)

def fetch_all_commands():
    ok_all = True
    command_specs = [
        (KEYS['runtime_key'], process_runtime_command),
        (KEYS['fan_key'], process_fan_command),
        (KEYS['light_key'], process_light_command)
    ]
    for key, fn in command_specs:
        data = fetch_next_command(key)
        if data is not None and not fn(data):
            ok_all = False
    SYS['command_error'] = not ok_all

def control_device(target, value):
    print('Bo qua control_device direct:', target, value)
    return False

# ===== BOOT =====
def boot():
    print('Smart House')
    print('Starting...')
    update_status_leds()
    close_door()

    load_device_registry()
    fetch_config()
    fetch_device_state()
    fetch_all_commands()

def main():
    boot()
    last = {'state': 0, 'config': 0, 'telemetry': 0, 'command': 0, 'registry': 0}

    while True:
        read_sensor()
        process_ir()
        update_door_auto_close()
        clear_security_alert_if_needed()
        apply_mode_logic()
        update_status_leds()
        update_lcd()

        now = int(time.time() * 1000)
        server_error_now = has_server_error()
        intervals = {
            'registry': 60000,
            'state': 5000 if server_error_now else 2000,
            'config': 60000 if server_error_now else 20000,
            'command': 700,
            'telemetry': 30000 if server_error_now else 8000
        }

        if now - last['registry'] >= intervals['registry']:
            load_device_registry()
            last['registry'] = now

        if now - last['state'] >= intervals['state']:
            fetch_device_state()
            last['state'] = now

        if now - last['config'] >= intervals['config']:
            fetch_config()
            last['config'] = now

        if (not server_error_now) and now - last['telemetry'] >= intervals['telemetry']:
            if sensor_is_valid():
                send_telemetry()
            last['telemetry'] = now

        if now - last['command'] >= intervals['command']:
            fetch_all_commands()
            last['command'] = now

        print(
            'device=', DEVICE_NAME,
            'time=', datetime.now().isoformat(timespec='seconds'),
            'mode=', SYS['mode'],
            'fan_status=', SYS['fan_status'],
            'fan_speed=', SYS['fan_speed'],
            'light_status=', SYS['light_status'],
            'door_locked=', SYS['door_locked'],
            'door_open=', SYS['door_open'],
            'failed_attempts=', SYS['failed_attempts'],
            'registry_error=', SYS['registry_error'],
            'state_error=', SYS['state_error'],
            'config_error=', SYS['config_error'],
            'telemetry_error=', SYS['telemetry_error'],
            'command_error=', SYS['command_error'],
            'server_error=', server_error_now,
            'sensor_error=', SYS['sensor_error'],
            'alert=', SYS['alert_active'],
            'security_alert=', SYS['security_alert_active'],
            'temp=', SYS['nhiet_do'],
            'humidity=', SYS['do_am'],
            'light=', SYS['shine'],
            'motion=', SYS['someone']
        )

        time.sleep(0.2)

if __name__ == '__main__':
    main()