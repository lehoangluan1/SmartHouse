from event_manager import *
import urequests, ujson, gc, time
from yolobit import *
from aiot_rgbled import RGBLed
from mqtt import *
from aiot_lcd1602 import LCD1602
from aiot_dht20 import DHT20
from aiot_ir_receiver import *

try:
    import usocket
    usocket.setdefaulttimeout(0.6)
except:
    pass

button_a.on_pressed = None
button_b.on_pressed = None
button_a.on_pressed_ab = button_b.on_pressed_ab = -1


rgb = RGBLed(pin1.pin, 4)
lcd = LCD1602()
dht = DHT20()

ir = IR_RX(Pin(pin10.pin, Pin.IN))
ir.start()

HOST, PORT, HOME = '10.229.90.208', 9000, 1
BASE = 'http://%s:%s' % (HOST, PORT)
HEAD = {'X-Device-Token': 'ohstem-demo-token', 'Content-Type': 'application/json'}
WIFI_SSID, WIFI_PASS = 'Test', '12345678'

DOOR_PASS = '123456'
DOOR_OPEN_MS = 5000
IR_TIMEOUT_MS = 15000
MAX_FAIL = 3
BOOT_GRACE_MS = 10000

COL_RED = '#050000'
COL_GREEN = '#000500'
COL_YELLOW = '#050500'
COL_OFF = '#000000'

CFG = {
    'Thigh': 30.0,
    'Tlow': 27.0,
    'Lhigh': 55,
    'Llow': 35,
    'Tsleep_high': 32.0,
    'Tsleep_low': 26.0,
    'Taway_high': 33.0,
    'Tcritical': 35.0,
    'auto_fan_speed': 70,
    'sleep_fan_speed': 30,
    'away_fan_speed': 60
}

KEY = {
    'runtime_key': 'yolobit-01',
    'fan_key': 'ohstem-fan-ctrl-01',
    'light_key': 'ohstem-light-ctrl-01',
    'temp_key': 'ohstem-temp-01',
    'humidity_key': 'ohstem-humidity-01',
    'light_sensor_key': 'ohstem-light-01',
    'motion_key': 'ohstem-motion-01',
    'runtime_id': None,
    'fan_id': None,
    'light_id': None
}

S = {
    'boot_ms': 0,
    'mode': 'away',
    'prev_mode': None,
    'hold_until': None,
    'fan_status': 'off',
    'fan_speed': 0,
    'light_status': 'off',

    'sensor_error': 0,
    'state_error': 0,
    'config_error': 0,
    'telemetry_error': 0,
    'command_error': 0,
    'registry_error': 0,
    'yolo_error': 0,

    'alert_active': 0,
    'security_alert_active': 0,

    'nhiet_do': None,
    'do_am': None,
    'shine': None,

    'pir_motion': 0,
    'camera_human_detected': 0,
    'camera_human_count': 0,
    'camera_confidence': 0.0,
    'camera_motion_detected': 0,
    'camera_motion_score': 0.0,

    'someone': 0,
    'last_human_seen_ms': 0,

    'door_locked': 1,
    'door_open': 0,
    'door_open_until': None,

    'ir_typing': 0,
    'ir_pass': '',
    'ir_timeout': None,
    'ir_last_input_ms': 0,
    'failed_attempts': 0,

    'last_setup_press_ms': 0,
    'last_ir_code': None,
    'last_ir_ms': 0,

    'last_security_alert_ms': 0,
    'manual_override': 0,
    'last_gc': 0,
    'last_led': None,
    'last_l1': '',
    'last_l2': '',

    'last_fan_hw': None,
    'last_light_hw': None,
    'last_door_hw': None,

    'net_block_until': 0,
    'last_net_fail_ms': 0,
    'consecutive_http_fail': 0,
    'last_http_path': '',
    'last_http_err': '',
    'debug_seq': 0,
    'server_ready': 0
}

IR_DIGIT_MAP = {
    IR_REMOTE_0: '0',
    IR_REMOTE_1: '1',
    IR_REMOTE_2: '2',
    IR_REMOTE_3: '3',
    IR_REMOTE_4: '4',
    IR_REMOTE_5: '5',
    IR_REMOTE_6: '6',
    IR_REMOTE_7: '7',
    IR_REMOTE_8: '8',
    IR_REMOTE_9: '9'
}

RAW_IR_DIGIT_MAP = {
    22: '0',
    12: '1',
    24: '2',
    94: '3',
    8:  '4',
    28: '5',
    90: '6',
    66: '7',
    82: '8',
    74: '9'
}

RAW_IR_KEY_SETUP = 21
RAW_IR_KEY_OK = 13

IR_KEY_SETUP = IR_REMOTE_SETUP
IR_KEY_OK = IR_REMOTE_F

IR_DEBOUNCE_MS = 180

ITV = {
    'registry': 30000,
    'state': 4000,
    'config': 30000,
    'telemetry': 7000,
    'command': 3500,
    'yolo': 4000,
    'debug': 4000
}
LAST = {k: 0 for k in ITV}

TEL = [
    ('temp_key', 'temperature', 'nhiet_do'),
    ('humidity_key', 'humidity', 'do_am'),
    ('light_sensor_key', 'light', 'shine'),
    ('motion_key', 'motion', 'someone')
]
tel_i = 0
cmd_i = 0
temp_alert_last = 0

DEBUG_MAX = 12
DEBUG_LOGS = []

def dbg(t, m=''):
    try:
        S['debug_seq'] += 1
        x = '%s|%s' % (S['debug_seq'], t) + (('|' + str(m)) if m != '' else '')
        DEBUG_LOGS.append(x)
        if len(DEBUG_LOGS) > DEBUG_MAX:
            DEBUG_LOGS.pop(0)
    except:
        pass

def dump_debug():
    try:
        print('----- DEBUG -----')
        for x in DEBUG_LOGS:
            print(x)
        print('-----------------')
    except:
        pass

def j(r):
    try:
        return ujson.loads(r.text)
    except Exception as e:
        dbg('JSON_ERR', e)

def close_resp(r):
    try:
        if r:
            r.close()
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

def server_error():
    return 0 if in_boot_grace() else 1 if (S['state_error'] or S['config_error'] or S['command_error'] or S['registry_error']) else 0

def net_allowed():
    return time.ticks_diff(ms(), S['net_block_until']) >= 0

def mark_net_fail(wait=2000, path='', err=''):
    now = ms()
    S['net_block_until'] = time.ticks_add(now, wait)
    S['last_net_fail_ms'] = now
    S['consecutive_http_fail'] += 1
    S['last_http_path'] = path
    S['last_http_err'] = str(err)
    dbg('NET_FAIL', '%s|%s|cool=%s' % (path, err, wait))

def mark_net_ok():
    S['consecutive_http_fail'] = 0
    S['last_http_err'] = ''

def req(method, path, data=None):
    if not net_allowed():
        dbg(method + '_SKIP', path)
        return None if method == 'GET' else False

    r = None
    try:
        if method == 'GET':
            r = urequests.get(BASE + path, headers=HEAD)
        else:
            r = urequests.post(BASE + path, json=data, headers=HEAD)

        if ok(r):
            mark_net_ok()
            return unwrap(j(r)) if method == 'GET' else True

        code = '?'
        try:
            code = r.status_code
        except:
            pass
        mark_net_fail(1500, path, 'HTTP_' + str(code))
    except Exception as e:
        mark_net_fail(2000, path, e)
    finally:
        close_resp(r)

    return None if method == 'GET' else False

GET = lambda p: req('GET', p)
POST = lambda p, d: req('POST', p, d)

def fan_hw(on, speed):
    try:
        pin0.write_analog(round(translate(clamp(to_int(speed), 0, 100), 0, 100, 0, 1023)) if on else 0)
    except Exception as e:
        dbg('FAN_HW_ERR', e)

def light_hw(on):
    try:
        pin8.write_digital(1 if on else 0)
    except Exception as e:
        dbg('LIGHT_HW_ERR', e)

def door_hw(op):
    try:
        pin4.servo_write(180 if op else 0)
    except Exception as e:
        dbg('SERVO_ERR', e)

def open_door():
    S['door_open'] = 1
    S['door_locked'] = 0
    S['door_open_until'] = time.ticks_add(ms(), DOOR_OPEN_MS)
    dbg('DOOR', 'OPEN')

def close_door():
    S['door_open'] = 0
    S['door_locked'] = 1
    S['door_open_until'] = None
    dbg('DOOR', 'CLOSE')

def mode_short():
    return {'auto': 'A', 'manual': 'M', 'sleep': 'S', 'away': 'W'}.get(S['mode'], '?')

def update_lcd():
    t = '--' if S['nhiet_do'] is None else str(round(S['nhiet_do'], 1))
    h = '--' if S['do_am'] is None else str(round(S['do_am'], 1))
    l = '--' if S['shine'] is None else str(S['shine'])
    l1 = fit16('T:%s H:%s' % (t, h))

    if S['ir_typing']:
        p = S['ir_pass']
        show = p[-8:] if len(p) > 8 else p
        l2 = fit16('P:' + show)
    elif S['sensor_error'] and server_error():
        l2 = fit16('L:%s E:SEN+SRV' % l)
    elif S['sensor_error']:
        l2 = fit16('L:%s E:SENSOR' % l)
    elif server_error():
        l2 = fit16('L:%s E:SERVER' % l)
    else:
        st = '%s P%s D%s K%s' % (
            mode_short(),
            1 if S['someone'] else 0,
            1 if S['door_open'] else 0,
            1 if S['door_locked'] else 0
        )
        l2 = fit16('L:%s %s' % (l, st))

    try:
        if l1 != S['last_l1']:
            lcd.move_to(0, 0)
            lcd.putstr(l1)
            S['last_l1'] = l1
        if l2 != S['last_l2']:
            lcd.move_to(0, 1)
            lcd.putstr(l2)
            S['last_l2'] = l2
    except Exception as e:
        dbg('LCD_ERR', e)

def update_leds():
    try:
        blink = ((ms() // 300) % 2) == 0
        srv = server_error()
        cur = [
            COL_RED if S['sensor_error'] and blink else COL_OFF,
            COL_RED if srv and blink else COL_OFF,
            (COL_YELLOW if blink else COL_OFF) if (S['sensor_error'] or srv) else (COL_GREEN if (S['light_status'] == 'on' or S['door_open']) else COL_OFF),
            COL_RED if (S['alert_active'] or S['security_alert_active']) and blink else COL_OFF
        ]
        if cur != S['last_led']:
            for i, c in enumerate(cur, 1):
                rgb.show(i, hex_to_rgb(c))
            S['last_led'] = cur
    except Exception as e:
        dbg('LED_ERR', e)

def load_registry():
    ds = GET('/gw/devices/home/%s' % HOME)
    if not isinstance(ds, list) or not ds:
        S['registry_error'] = 1
        dbg('REGISTRY', 'BAD_DATA')
        return

    for d in ds:
        try:
            k = norm(d.get('deviceKey'))
            if k == KEY['runtime_key']:
                KEY['runtime_id'] = d.get('id')
            elif k == KEY['fan_key']:
                KEY['fan_id'] = d.get('id')
            elif k == KEY['light_key']:
                KEY['light_id'] = d.get('id')
            elif 'temp' in k:
                KEY['temp_key'] = d.get('deviceKey')
            elif 'humidity' in k:
                KEY['humidity_key'] = d.get('deviceKey')
            elif k == 'ohstem-light-01':
                KEY['light_sensor_key'] = d.get('deviceKey')
            elif 'motion' in k or 'pir' in k:
                KEY['motion_key'] = d.get('deviceKey')
        except Exception as e:
            dbg('REG_ITEM_ERR', e)

    S['registry_error'] = 0 if (KEY['runtime_id'] and KEY['fan_id'] and KEY['light_id']) else 1
    dbg('REGISTRY', 'OK' if not S['registry_error'] else 'MISS_ID')

def get_state(i):
    return GET('/gw/devices/%s/state' % i) if i else None

def fetch_state():
    ok1 = ok2 = ok3 = 1

    a = get_state(KEY['runtime_id'])
    if isinstance(a, dict):
        if a.get('mode') is not None:
            S['mode'] = str(a.get('mode')).lower()
        S['hold_until'] = a.get('holdUntil')
        S['prev_mode'] = a.get('prevMode')
    else:
        ok1 = 0

    if not S['manual_override']:
        b = get_state(KEY['fan_id'])
        c = get_state(KEY['light_id'])

        if isinstance(b, dict):
            if b.get('fanStatus') is not None:
                S['fan_status'] = str(b.get('fanStatus')).lower()
            if b.get('fanSpeed') is not None:
                S['fan_speed'] = clamp(to_int(b.get('fanSpeed'), S['fan_speed']), 0, 100)
        else:
            ok2 = 0

        if isinstance(c, dict):
            if c.get('lightStatus') is not None:
                S['light_status'] = str(c.get('lightStatus')).lower()
        else:
            ok3 = 0

    S['state_error'] = 0 if (ok1 and ok2 and ok3) else 1
    dbg('STATE', 'OK' if not S['state_error'] else 'FAIL')

def fetch_config():
    d = GET('/gw/homes/%s/configs' % HOME)
    if d is None:
        S['config_error'] = 1
        dbg('CONFIG', 'NONE')
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
            dbg('CONFIG', 'LIST_TO_DICT')
        except Exception as e:
            S['config_error'] = 1
            dbg('CONFIG_LIST_ERR', e)
            return

    if not isinstance(d, dict):
        S['config_error'] = 1
        dbg('CONFIG_TYPE', type(d))
        dbg('CONFIG_RAW', d)
        return

    mp = {
        'thigh': ('Thigh', to_float),
        'tlow': ('Tlow', to_float),
        'lhigh': ('Lhigh', to_int),
        'llow': ('Llow', to_int),
        'tsleepHigh': ('Tsleep_high', to_float),
        'tsleepLow': ('Tsleep_low', to_float),
        'tawayHigh': ('Taway_high', to_float),
        'tcritical': ('Tcritical', to_float),
        'autoFanSpeed': ('auto_fan_speed', to_int),
        'sleepFanSpeed': ('sleep_fan_speed', to_int),
        'awayFanSpeed': ('away_fan_speed', to_int)
    }

    applied = 0
    for k in mp:
        if k in d and d[k] is not None:
            n, c = mp[k]
            v = c(d[k], CFG[n])
            CFG[n] = clamp(v, 0, 100) if 'speed' in n.lower() else v
            applied += 1

    for k, n in {
        'tsleep_high': 'Tsleep_high',
        'tsleep_low': 'Tsleep_low',
        'taway_high': 'Taway_high',
        'tcritical': 'Tcritical',
        'auto_fan_speed': 'auto_fan_speed',
        'sleep_fan_speed': 'sleep_fan_speed',
        'away_fan_speed': 'away_fan_speed'
    }.items():
        if k in d and d[k] is not None:
            CFG[n] = clamp(to_int(d[k], CFG[n]), 0, 100) if 'speed' in n.lower() else to_float(d[k], CFG[n])
            applied += 1

    S['config_error'] = 0
    dbg('CONFIG', 'OK applied=' + str(applied))

def send_alert(tp, msg):
    if KEY['runtime_id']:
        POST('/gw/homes/%s/alerts' % HOME, {
            'deviceId': KEY['runtime_id'],
            'sensorId': None,
            'type': tp,
            'message': msg
        })

def trigger_security_alert(tp, msg):
    S['security_alert_active'] = 1
    S['last_security_alert_ms'] = ms()
    send_alert(tp, msg)
    dbg('SEC_ALERT', tp + '|' + msg)

def read_sensor():
    try:
        dht.read_dht20()
        S['nhiet_do'] = dht.dht20_temperature()
        S['do_am'] = dht.dht20_humidity()
    except Exception as e:
        S['nhiet_do'] = None
        S['do_am'] = None
        dbg('DHT_ERR', e)

    try:
        S['shine'] = clamp(round(translate(pin2.read_analog(), 0, 4095, 0, 100)), 0, 100)
    except Exception as e:
        S['shine'] = None
        dbg('LIGHT_SENSOR_ERR', e)

    try:
        S['pir_motion'] = 1 if pin16.read_digital() else 0
    except Exception as e:
        S['pir_motion'] = 0
        dbg('PIR_ERR', e)

def sensor_valid():
    t, h, l = S['nhiet_do'], S['do_am'], S['shine']
    return not (t is None or h is None or l is None or t < -10 or t > 80 or h < 0 or h > 100 or l < 0 or l > 100)

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
        dbg('YOLO', 'FAIL')
        return

    S['yolo_error'] = 0
    S['camera_human_detected'] = 1 if d.get('human_detected', 0) else 0
    S['camera_human_count'] = to_int(d.get('human_count', 0), 0)
    S['camera_confidence'] = to_float(d.get('max_confidence', 0.0), 0.0)
    S['camera_motion_detected'] = 1 if d.get('motion_detected', 0) else 0
    S['camera_motion_score'] = to_float(d.get('movement_score', 0.0), 0.0)
    dbg('YOLO', 'OK hc=%s cf=%s' % (S['camera_human_count'], S['camera_confidence']))

def combine_motion():
    active = S['camera_motion_detected'] or (S['pir_motion'] and S['camera_human_detected'] and S['camera_confidence'] >= 0.5)
    if active:
        S['someone'] = 1
        S['last_human_seen_ms'] = ms()
    elif time.ticks_diff(ms(), S['last_human_seen_ms']) > 3000:
        S['someone'] = 0

def ir_cancel():
    S['ir_typing'] = 0
    S['ir_pass'] = ''
    S['ir_last_input_ms'] = 0
    S['ir_timeout'] = None
    update_lcd()
    dbg('IR', 'CANCEL')

def ir_timeout_check():
    if S['ir_typing'] and S['ir_last_input_ms']:
        if time.ticks_diff(ms(), S['ir_last_input_ms']) >= IR_TIMEOUT_MS:
            dbg('IR_TIMEOUT', 'TIMEOUT')
            ir_cancel()

def ir_ok():
    S['ir_pass'] = ''
    S['ir_typing'] = 0
    S['ir_last_input_ms'] = 0
    S['ir_timeout'] = None
    S['failed_attempts'] = 0
    S['security_alert_active'] = 0
    open_door()
    update_lcd()
    dbg('IR', 'PASS_OK')

def ir_fail():
    S['failed_attempts'] += 1
    S['ir_pass'] = ''
    S['ir_typing'] = 0
    S['ir_last_input_ms'] = 0
    S['ir_timeout'] = None

    if S['failed_attempts'] >= MAX_FAIL:
        trigger_security_alert('WRONG_PASSWORD', 'Nhap sai mat khau %s lan' % S['failed_attempts'])

    update_lcd()
    dbg('IR', 'PASS_FAIL_' + str(S['failed_attempts']))

def ir_append(ch):
    if not S['ir_typing']:
        return

    if len(S['ir_pass']) < len(DOOR_PASS):
        S['ir_pass'] += str(ch)
        S['ir_last_input_ms'] = ms()
        S['ir_timeout'] = time.ticks_add(ms(), IR_TIMEOUT_MS)
        update_lcd()
        dbg('IR_DIGIT', S['ir_pass'])

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

def on_ir_received(token, addr, ext):
    try:
        code = ir.get_code()
    except Exception as e:
        dbg('IR_READ_ERR', e)
        return

    if code is None:
        return

    now = ms()

    # chống dội phím / lặp mã IR
    if S['last_ir_code'] == code and time.ticks_diff(now, S['last_ir_ms']) < IR_DEBOUNCE_MS:
        return

    S['last_ir_code'] = code
    S['last_ir_ms'] = now
    dbg('IR_CODE', code)

    # nút SETUP: bắt đầu nhập mật khẩu
    if is_ir_setup(code):
        S['ir_typing'] = 1
        S['ir_pass'] = ''
        S['ir_last_input_ms'] = now
        S['ir_timeout'] = time.ticks_add(now, IR_TIMEOUT_MS)
        update_lcd()
        dbg('IR', 'START_TYPING')
        return

    # nếu chưa vào chế độ nhập thì bỏ qua các nút khác
    if not S['ir_typing']:
        return

    # nút OK: kiểm tra mật khẩu
    if is_ir_ok(code):
        dbg('IR_OK_PRESS', S['ir_pass'])
        if S['ir_pass'] == DOOR_PASS:
            ir_ok()
        else:
            ir_fail()
        return

    # nút số
    digit = get_ir_digit(code)
    if digit is not None:
        ir_append(digit)
        return

    dbg('IR_UNKNOWN', code)

def apply_logic():
    if not sensor_valid():
        S['sensor_error'] = 1
        S['alert_active'] = 0
        S['fan_status'] = 'off'
        S['fan_speed'] = 0
        S['light_status'] = 'off'
    else:
        S['sensor_error'] = 0

        if S['fan_status'] not in ['on', 'off']:
            S['fan_status'] = 'off'
        if S['light_status'] not in ['on', 'off']:
            S['light_status'] = 'off'

        S['fan_speed'] = clamp(to_int(S['fan_speed'], 0), 0, 100)

        if S['mode'] != 'manual':
            fs, sp, ls = 'off', 0, 'off'
            t, l = S['nhiet_do'], S['shine']

            if S['mode'] == 'sleep':
                if t is not None and t >= CFG['Tsleep_high']:
                    fs, sp = 'on', CFG['sleep_fan_speed']
                elif t is not None and t <= CFG['Tsleep_low']:
                    fs, sp = 'off', 0

            elif S['mode'] == 'away':
                if t is not None and t >= CFG['Taway_high']:
                    fs, sp = 'on', CFG['away_fan_speed']

            else:
                if t is not None and t >= CFG['Thigh']:
                    fs, sp = 'on', CFG['auto_fan_speed']
                elif t is not None and t <= CFG['Tlow']:
                    fs, sp = 'off', 0

                if l is not None and l <= CFG['Llow']:
                    ls = 'on'
                elif l is not None and l >= CFG['Lhigh']:
                    ls = 'off'

            S['fan_status'], S['fan_speed'], S['light_status'] = fs, sp, ls

    if S['fan_status'] == 'on' and S['fan_speed'] <= 0:
        S['fan_speed'] = CFG['sleep_fan_speed'] if S['mode'] == 'sleep' else CFG['away_fan_speed'] if S['mode'] == 'away' else CFG['auto_fan_speed']

    fh = (S['fan_status'], S['fan_speed'])
    if fh != S['last_fan_hw']:
        fan_hw(S['fan_status'] == 'on', S['fan_speed'])
        S['last_fan_hw'] = fh

    if S['light_status'] != S['last_light_hw']:
        light_hw(S['light_status'] == 'on')
        S['last_light_hw'] = S['light_status']

    S['alert_active'] = 1 if ((S['nhiet_do'] is not None and S['nhiet_do'] > CFG['Tcritical']) or (S['mode'] == 'away' and S['someone']) or S['security_alert_active']) else 0

def send_tel():
    global tel_i
    for _ in range(len(TEL)):
        kn, tp, sn = TEL[tel_i]
        tel_i = (tel_i + 1) % len(TEL)
        dk, v = KEY.get(kn), S.get(sn)
        if sn == 'someone':
            v = 1 if v else 0
        if dk is not None and v is not None:
            S['telemetry_error'] = 0 if POST('/gw/device-telemetry', {'deviceKey': dk, 'sensorType': tp, 'value': v}) else 1
            dbg('TEL', tp + '=' + str(v) + '|ok=' + str(0 if S['telemetry_error'] else 1))
            return
    S['telemetry_error'] = 1
    dbg('TEL', 'NO_VALID_DATA')

def next_cmd(device_key):
    d = GET('/gw/device/%s/commands/next' % device_key) if device_key else None
    return d if isinstance(d, dict) and d.get('id') is not None else None

def ack(device_key, cid):
    return POST('/gw/device/%s/commands/ack' % device_key, {'id': cid}) if device_key and cid else False

def normalize_target(x):
    x = norm(x).replace('-', '_')
    return {'fanstatus': 'fan', 'fanspeed': 'fan_speed', 'lightstatus': 'light', 'lightlevel': 'light_level'}.get(x, x)

def cmd_runtime(c):
    cid = c.get('id')
    if cid is None:
        return
    if normalize_target(c.get('target')) == 'mode' and c.get('value') is not None:
        S['prev_mode'] = S['mode']
        S['mode'] = str(c.get('value')).lower()
        if S['mode'] != 'manual':
            S['manual_override'] = 0
        dbg('CMD_RUNTIME', 'mode=' + str(S['mode']))
    ack(KEY['runtime_key'], cid)

def cmd_fan(c):
    cid = c.get('id')
    if cid is None:
        return
    S['prev_mode'] = S['mode']
    S['mode'] = 'manual'
    S['manual_override'] = 1
    t, v = normalize_target(c.get('target')), c.get('value')

    if t == 'fan':
        if v is not None:
            S['fan_status'] = str(v).lower()
            S['fan_speed'] = 0 if S['fan_status'] == 'off' else (50 if S['fan_speed'] <= 0 else S['fan_speed'])
    elif t == 'fan_speed':
        S['fan_speed'] = clamp(to_int(v, S['fan_speed']), 0, 100)
        S['fan_status'] = 'on' if S['fan_speed'] > 0 else 'off'

    dbg('CMD_FAN', t + '=' + str(v))
    ack(KEY['fan_key'], cid)

def cmd_light(c):
    cid = c.get('id')
    if cid is None:
        return
    S['prev_mode'] = S['mode']
    S['mode'] = 'manual'
    S['manual_override'] = 1
    if normalize_target(c.get('target')) == 'light' and c.get('value') is not None:
        S['light_status'] = str(c.get('value')).lower()
    dbg('CMD_LIGHT', str(c.get('value')))
    ack(KEY['light_key'], cid)

def fetch_cmd():
    global cmd_i
    arr = [
        (KEY['runtime_key'], cmd_runtime),
        (KEY['fan_key'], cmd_fan),
        (KEY['light_key'], cmd_light)
    ]
    k, f = arr[cmd_i]
    cmd_i = (cmd_i + 1) % len(arr)
    c = next_cmd(k)
    if c is not None:
        f(c)

def print_status():
    print(
        'mode=', S['mode'],
        'T=', S['nhiet_do'],
        'H=', S['do_am'],
        'L=', S['shine'],
        'pir=', S['pir_motion'],
        'cam=', S['camera_human_detected'],
        'someone=', S['someone'],
        'fan=', S['fan_status'], S['fan_speed'],
        'light=', S['light_status'],
        'door=', S['door_open'],
        'lock=', S['door_locked'],
        'ir_typing=', S['ir_typing'],
        'ir_pass=', S['ir_pass'],
        'srv=', server_error(),
        'yolo=', S['yolo_error'],
        'http_fail=', S['consecutive_http_fail'],
        'last_http=', S['last_http_path'],
        'err=', S['last_http_err']
    )
    dump_debug()

ir.on_received(on_ir_received)

display.scroll('IoT')
lcd.backlight_on()
lcd.clear()
lcd.move_to(0, 0)
lcd.putstr('Smart House')
lcd.move_to(0, 1)
lcd.putstr('Booting...')

S['boot_ms'] = ms()
dbg('BOOT', 'START')

close_door()
update_leds()
update_lcd()

try:
    mqtt.connect_wifi(WIFI_SSID, WIFI_PASS)
    dbg('WIFI', 'CONNECTED')
except Exception as e:
    dbg('WIFI_ERR', e)

time.sleep_ms(1200)

for i in range(2):
    try:
        load_registry()
    except Exception as e:
        S['registry_error'] = 1
        dbg('BOOT_REG_ERR', e)

    time.sleep_ms(120)

    try:
        fetch_config()
    except Exception as e:
        S['config_error'] = 1
        dbg('BOOT_CFG_ERR', e)

    time.sleep_ms(120)

    try:
        fetch_state()
    except Exception as e:
        S['state_error'] = 1
        dbg('BOOT_STATE_ERR', e)

    if not (S['registry_error'] or S['config_error'] or S['state_error']):
        S['server_ready'] = 1
        break

    time.sleep_ms(500)

try:
    yh = GET('/gw/yolo/health')
    if yh is not None:
        S['server_ready'] = 1
    dbg('YOLO_HEALTH', yh)
except Exception as e:
    dbg('YOLO_HEALTH_ERR', e)

for _ in range(2):
    try:
        fetch_cmd()
        S['command_error'] = 0
    except Exception as e:
        S['command_error'] = 1
        dbg('BOOT_CMD_ERR', e)
    time.sleep_ms(100)

lcd.clear()
lcd.move_to(0, 0)
lcd.putstr('Smart House')
lcd.move_to(0, 1)
lcd.putstr('Running...')
update_lcd()

while True:
    now = ms()

    if time.ticks_diff(now, S['last_gc']) > 10000:
        try:
            gc.collect()
            S['last_gc'] = now
        except Exception as e:
            dbg('GC_ERR', e)

    read_sensor()
    ir_timeout_check()

    if S['door_open'] and S['door_open_until'] is not None and time.ticks_diff(now, S['door_open_until']) >= 0:
        close_door()

    if S['door_open'] != S['last_door_hw']:
        door_hw(S['door_open'])
        S['last_door_hw'] = S['door_open']

    if time.ticks_diff(now, LAST['yolo']) >= ITV['yolo']:
        if not S['ir_typing'] and net_allowed():
            try:
                update_yolo()
            except Exception as e:
                S['yolo_error'] = 1
                dbg('YOLO_ERR', e)
        LAST['yolo'] = now

    combine_motion()

    if S['security_alert_active'] and time.ticks_diff(now, S['last_security_alert_ms']) >= 15000:
        S['security_alert_active'] = 0

    apply_logic()
    update_leds()
    update_lcd()

    if S['nhiet_do'] is not None and S['nhiet_do'] > CFG['Tcritical'] and time.ticks_diff(now, temp_alert_last) >= 15000:
        send_alert('HIGH_TEMPERATURE', 'Nhiet do cao: %s' % round(S['nhiet_do'], 1))
        temp_alert_last = now

    if S['ir_typing']:
        time.sleep_ms(500)
        continue

    if server_error():
        ITV['state'], ITV['command'], ITV['telemetry'], ITV['yolo'] = 6000, 4000, 5000, 6000
    else:
        ITV['state'], ITV['command'], ITV['telemetry'], ITV['yolo'] = 3000, 3500, 7000, 4000

    did_network = 0
    if net_allowed():
        for name, fn, errk in [
            ('command', fetch_cmd, 'command_error'),
            ('state', fetch_state, 'state_error'),
            ('telemetry', None, 'telemetry_error'),
            ('config', fetch_config, 'config_error'),
            ('registry', load_registry, 'registry_error')
        ]:
            if time.ticks_diff(now, LAST[name]) >= ITV[name]:
                try:
                    if name == 'telemetry':
                        if sensor_valid():
                            send_tel()
                            if S['telemetry_error']:
                                time.sleep_ms(80)
                                send_tel()
                    else:
                        fn()
                    S[errk] = 0 if name != 'telemetry' or not S['telemetry_error'] else 1
                except Exception as e:
                    S[errk] = 1
                    dbg(name.upper() + '_ERR', e)

                LAST[name] = now
                did_network = 1
                break
    else:
        dbg('NET_COOLDOWN', time.ticks_diff(S['net_block_until'], now))

    if did_network:
        time.sleep_ms(30)
    else:
        time.sleep_ms(35)

    if time.ticks_diff(now, LAST['debug']) >= ITV['debug']:
        print_status()
        LAST['debug'] = now