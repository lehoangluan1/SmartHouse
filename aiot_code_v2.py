from event_manager import *
import urequests, gc, ujson, music, time
from yolobit import *
from aiot_rgbled import RGBLed
from mqtt import *
from aiot_lcd1602 import LCD1602
from aiot_dht20 import DHT20
from aiot_ir_receiver import *

# ===== INIT =====
button_a.on_pressed = None
button_b.on_pressed = None
button_a.on_pressed_ab = button_b.on_pressed_ab = -1
event_manager.reset()

tiny_rgb = RGBLed(pin1.pin, 4)
lcd = LCD1602()
dht = DHT20()
ir_rx = IR_RX(Pin(pin10.pin, Pin.IN))
ir_rx.start()

SERVER_HOST = '10.128.12.87'
SERVER_PORT = 8080
BASE = 'http://{}:{}'.format(SERVER_HOST, SERVER_PORT)

HOME_ID = 1
DEVICE_NAME = 'OhStem Living Room'

COLOR_RED = '#ff0000'
COLOR_GREEN = '#00ff00'
COLOR_OFF = '#000000'
COLOR_YELLOW = '#ffff00'

DOOR_PASSWORD = '123456'
MAX_FAILED_ATTEMPTS = 3
DOOR_OPEN_MS = 5000
DOOR_RELAY_PIN = pin14

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

IR_DIGIT_MAP = {
  IR_REMOTE_0: '0', IR_REMOTE_1: '1', IR_REMOTE_2: '2', IR_REMOTE_3: '3', IR_REMOTE_4: '4',
  IR_REMOTE_5: '5', IR_REMOTE_6: '6', IR_REMOTE_7: '7', IR_REMOTE_8: '8', IR_REMOTE_9: '9'
}

DEVICE_RULES = {
  'runtime': {
    'exact': ['yolobit-01'],
    'type': ['SENSOR_NODE', 'HUB', 'OTHER'],
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
def j(resp):
  try:
    return ujson.loads(resp.text)
  except Exception as e:
    print('JSON loi:', e)
    try: print(resp.text)
    except: pass
    return None

def close_resp(resp):
  try:
    if resp: resp.close()
  except: pass

def unwrap(x):
  return x.get('data') if isinstance(x, dict) and 'data' in x else x

def ok(resp):
  try: return resp and 200 <= resp.status_code < 300
  except: return False

def clamp(v, a, b):
  return a if v < a else b if v > b else v

def to_int(v, d=0):
  try: return int(v)
  except:
    try: return int(float(v))
    except: return d

def to_float(v, d=0.0):
  try: return float(v)
  except: return d

def norm(v): return '' if v is None else str(v).strip().lower()
def norm_type(v): return '' if v is None else str(v).strip().upper()
def contains(text, keys):
  t = norm(text)
  for k in keys:
    if norm(k) in t: return True
  return False

def has_server_error():
  return SYS['state_error'] or SYS['config_error'] or SYS['telemetry_error'] or SYS['command_error'] or SYS['registry_error']

def http_get(path):
  resp = None
  try:
    resp = urequests.get(BASE + path)
    return unwrap(j(resp))
  except Exception as e:
    print('GET loi', path, e)
    return None
  finally:
    close_resp(resp)

def http_post(path, payload):
  resp = None
  try:
    resp = urequests.post(BASE + path, json=payload, data=None, headers={})
    return ok(resp), resp.text if resp else None
  except Exception as e:
    print('POST loi', path, e)
    return False, None
  finally:
    close_resp(resp)

def batquat(on, speed):
  pin0.write_analog(round(translate(clamp(to_int(speed), 0, 100), 0, 100, 0, 1023)) if on else 0)

def batden(on):
  pin8.write_digital(1 if on else 0)

def mode_short():
  return {'auto': 'A', 'manual': 'M', 'sleep': 'S', 'away': 'W'}.get(SYS['mode'], '?')

def fit16(s):
  s = str(s)
  return s[:16] if len(s) > 16 else s + (' ' * (16 - len(s)))

def mask_pass(s):
  return '*' * len(s)

# ===== DOOR =====
def set_door_relay(is_open):
  try:
    DOOR_RELAY_PIN.write_digital(1 if is_open else 0)
  except Exception as e:
    print('Loi relay cua:', e)

def open_door():
  SYS['door_open'] = True
  SYS['door_locked'] = False
  SYS['door_open_until'] = time.ticks_add(time.ticks_ms(), DOOR_OPEN_MS)
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
    if time.ticks_diff(time.ticks_ms(), SYS['door_open_until']) >= 0:
      close_door()

# ===== DEVICE REGISTRY =====
def device_match(d, rule):
  if d is None or rule is None: return False
  if 'type' in rule and norm_type(d.get('type')) in rule['type']: return True
  if 'key' in rule and contains(d.get('deviceKey'), rule['key']): return True
  if 'name' in rule and contains(d.get('name'), rule['name']): return True
  return False

def find_exact(devices, keys):
  keys = [norm(k) for k in keys]
  for d in devices:
    if norm(d.get('deviceKey')) in keys: return d
  return None

def find_by_rule(devices, name):
  rule = DEVICE_RULES.get(name, {})
  d = find_exact(devices, rule.get('exact', []))
  if d is not None: return d
  for x in devices:
    if device_match(x, rule):
      if name == 'light_sensor' and norm(x.get('deviceKey')) == 'ohstem-light-01':
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
  KEYS['runtime_key'] = DEV['runtime'].get('deviceKey')
  KEYS['fan_id'] = DEV['fan'].get('id')
  KEYS['fan_key'] = DEV['fan'].get('deviceKey')
  KEYS['light_id'] = DEV['light'].get('id')
  KEYS['light_key'] = DEV['light'].get('deviceKey')

  if DEV['temp']: KEYS['temp_key'] = DEV['temp'].get('deviceKey')
  if DEV['humidity']: KEYS['humidity_key'] = DEV['humidity'].get('deviceKey')
  if DEV['light_sensor']: KEYS['light_sensor_key'] = DEV['light_sensor'].get('deviceKey')
  if DEV['motion']: KEYS['motion_key'] = DEV['motion'].get('deviceKey')

  SYS['registry_error'] = False
  print('registry ok', KEYS)
  return True

# ===== ALERT =====
def send_security_alert(reason, detail):
  if KEYS['runtime_id'] is None:
    print('Bo qua security alert do chua co registry')
    return
  payload = {"deviceId": KEYS['runtime_id'], "sensorId": None, "type": reason, "message": detail}
  success, text = http_post('/api/homes/' + str(HOME_ID) + '/alerts', payload)
  print('security alert', 'ok' if success else 'fail', text)

def trigger_security_alert(reason, detail):
  SYS['security_alert_active'] = True
  SYS['last_security_alert_ms'] = time.ticks_ms()
  print('SECURITY ALERT:', reason, detail)
  send_security_alert(reason, detail)

def clear_security_alert_if_needed():
  if SYS['security_alert_active'] and time.ticks_diff(time.ticks_ms(), SYS['last_security_alert_ms']) >= 15000:
    SYS['security_alert_active'] = False

# ===== IR =====
def update_lcd():
  t = '--' if SYS['nhiet_do'] is None else str(round(SYS['nhiet_do'], 1))
  h = '--' if SYS['do_am'] is None else str(round(SYS['do_am'], 1))
  l = '--' if SYS['shine'] is None else str(SYS['shine'])
  line1 = fit16('T:' + t + ' H:' + h)

  if SYS['sensor_error'] and has_server_error():
    st = 'E:SEN+SRV'
  elif SYS['sensor_error']:
    st = 'E:SENSOR'
  elif has_server_error():
    st = 'E:SERVER'
  else:
    door = 'D1' if SYS['door_open'] else 'D0'
    lock = 'K1' if SYS['door_locked'] else 'K0'
    if SYS['ir_typing']:
      p = mask_pass(SYS['ir_pass'])
      st = lock + ' ' + door + ' ' + (p[-4:] if len(p) > 4 else p)
    else:
      motion = 'P1' if SYS['someone'] else 'P0'
      st = mode_short() + ' ' + motion + ' ' + door + ' ' + lock

  line2 = fit16('L:' + l + ' ' + st)

  if line1 != SYS['last_lcd_1']:
    lcd.move_to(0, 0); lcd.putstr(line1); SYS['last_lcd_1'] = line1
  if line2 != SYS['last_lcd_2']:
    lcd.move_to(0, 1); lcd.putstr(line2); SYS['last_lcd_2'] = line2

def append_ir_digit(d):
  if len(SYS['ir_pass']) < 12:
    SYS['ir_pass'] += str(d)
    music.play(['C4:0.1'], wait=False)
    update_lcd()

def handle_wrong_password():
  SYS['failed_attempts'] += 1
  music.play(['C5:0.2', 'C4:0.2'], wait=False)
  SYS['ir_pass'] = ''
  SYS['ir_typing'] = False
  if SYS['failed_attempts'] == MAX_FAILED_ATTEMPTS:
    trigger_security_alert('WRONG_PASSWORD', 'Nhap sai mat khau qua 3 lan')
  update_lcd()

def handle_correct_password():
  music.play(['C4:0.2', 'E4:0.2', 'G4:0.2'], wait=False)
  SYS['ir_pass'] = ''
  SYS['ir_typing'] = False
  SYS['failed_attempts'] = 0
  SYS['security_alert_active'] = False
  open_door()
  update_lcd()

def process_ir():
  code = ir_rx.get_code()
  if code is None: return
  try:
    if code == IR_REMOTE_SETUP:
      if SYS['door_locked']:
        SYS['ir_typing'] = not SYS['ir_typing']
        SYS['ir_pass'] = ''
        music.play(['G3:0.2'], wait=False)
        update_lcd()
      else:
        music.play(['G3:0.1'], wait=False)
    elif SYS['ir_typing']:
      if code in IR_DIGIT_MAP:
        append_ir_digit(IR_DIGIT_MAP[code])
      elif code == IR_REMOTE_F:
        handle_correct_password() if SYS['ir_pass'] == DOOR_PASSWORD else handle_wrong_password()
    elif code == IR_REMOTE_F and not SYS['door_locked']:
      music.play(['C4:0.2'], wait=False)
      close_door()
      update_lcd()
  except Exception as e:
    print('Loi process_ir:', e)
  finally:
    ir_rx.clear_code()

# ===== LED =====
def update_status_leds():
  blink = ((time.ticks_ms() // 300) % 2) == 0
  srv = has_server_error()
  tiny_rgb.show(1, hex_to_rgb(COLOR_RED if SYS['sensor_error'] and blink else COLOR_OFF))
  tiny_rgb.show(2, hex_to_rgb(COLOR_RED if srv and blink else COLOR_OFF))
  tiny_rgb.show(3, hex_to_rgb((COLOR_YELLOW if blink else COLOR_OFF) if (SYS['sensor_error'] or srv) else (COLOR_GREEN if (SYS['light_status'] == 'on' or SYS['door_open']) else COLOR_OFF)))
  tiny_rgb.show(4, hex_to_rgb(COLOR_RED if (SYS['alert_active'] or SYS['security_alert_active']) and blink else COLOR_OFF))

# ===== SENSOR =====
def read_sensor():
  try:
    dht.read_dht20()
    SYS['nhiet_do'] = dht.dht20_temperature()
    SYS['do_am'] = dht.dht20_humidity()
  except Exception as e:
    print('Loi DHT20:', e)
    SYS['nhiet_do'] = None
    SYS['do_am'] = None

  try:
    SYS['shine'] = clamp(round(translate(pin2.read_analog(), 0, 4095, 0, 100)), 0, 100)
  except Exception as e:
    print('Loi cam bien anh sang:', e)
    SYS['shine'] = None

  try:
    SYS['someone'] = (pin16.read_digital() == 1)
  except Exception as e:
    print('Loi cam bien hien dien:', e)
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
    if s.get('mode') is not None: SYS['mode'] = str(s.get('mode')).lower()
    SYS['hold_until'] = s.get('holdUntil')
    SYS['prev_mode'] = s.get('prevMode')
  else:
    ok_all = False

  s = fetch_state_by_device_id(KEYS['fan_id'])
  if isinstance(s, dict):
    if s.get('fanStatus') is not None: SYS['fan_status'] = str(s.get('fanStatus')).lower()
    if s.get('fanSpeed') is not None: SYS['fan_speed'] = clamp(to_int(s.get('fanSpeed'), SYS['fan_speed']), 0, 100)
  else:
    ok_all = False

  s = fetch_state_by_device_id(KEYS['light_id'])
  if isinstance(s, dict):
    if s.get('lightStatus') is not None: SYS['light_status'] = str(s.get('lightStatus')).lower()
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
  if SYS['fan_status'] not in ['on', 'off']: SYS['fan_status'] = 'off'
  if SYS['light_status'] not in ['on', 'off']: SYS['light_status'] = 'off'
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
    SYS['fan_speed'] = CFG['sleep_fan_speed'] if SYS['mode'] == 'sleep' else CFG['away_fan_speed'] if SYS['mode'] == 'away' else CFG['auto_fan_speed']

  batquat(SYS['fan_status'] == 'on', SYS['fan_speed'] if SYS['fan_status'] == 'on' else 0)
  batden(SYS['light_status'] == 'on')

  temp_critical = SYS['nhiet_do'] is not None and SYS['nhiet_do'] > CFG['Tcritical']
  away_motion = SYS['mode'] == 'away' and SYS['someone']
  SYS['alert_active'] = temp_critical or away_motion or SYS['security_alert_active']

# ===== TELEMETRY =====
TELEMETRY_ITEMS = [
  ('temp_key', 'temperature', 'nhiet_do'),
  ('humidity_key', 'humidity', 'do_am'),
  ('light_sensor_key', 'light', 'shine'),
  ('motion_key', 'motion', 'someone')
]

telemetry_cursor = 0

def send_one_telemetry(device_key, sensor_type, value):
  payload = {"deviceKey": device_key, "sensorType": sensor_type, "value": value}
  success, text = http_post('/api/device-telemetry', payload)
  SYS['telemetry_error'] = not success
  print('telemetry', sensor_type, 'ok' if success else 'fail', text)
  return success

def send_next_telemetry():
  global telemetry_cursor

  total = len(TELEMETRY_ITEMS)
  for _ in range(total):
    key_name, sensor_type, sys_key = TELEMETRY_ITEMS[telemetry_cursor]
    telemetry_cursor = (telemetry_cursor + 1) % total

    device_key = KEYS.get(key_name)
    value = SYS.get(sys_key)

    if sys_key == 'someone':
      value = bool(value)

    if device_key is not None and value is not None:
      return send_one_telemetry(device_key, sensor_type, value)

  return False

# ===== COMMAND =====
def fetch_next_command(device_key):
  if device_key is None: return None
  data = http_get('/api/v1/device/' + device_key + '/commands/next')
  return data if isinstance(data, dict) and data.get('id') is not None else None


def ack_command(device_key, command_id):
  if device_key is None or command_id is None: return False
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
  if cid is None: return False
  if target == 'mode' and value is not None:
    SYS['prev_mode'] = SYS['mode']
    SYS['mode'] = str(value).lower()
  return ack_command(KEYS['runtime_key'], cid)

def process_fan_command(cmd):
  cid, target, value = cmd.get('id'), normalize_target(cmd.get('target')), cmd.get('value')
  if cid is None: return False
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
  if cid is None: return False
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
display.scroll('IoT')
mqtt.connect_wifi('Test', '12345678')
lcd.backlight_on()
lcd.clear()
lcd.move_to(0, 0); lcd.putstr('Smart House')
lcd.move_to(0, 1); lcd.putstr('Starting...')
update_status_leds()
close_door()

load_device_registry()
fetch_config()
fetch_device_state()
fetch_all_commands()

last = {'state': 0, 'config': 0, 'telemetry': 0, 'command': 0, 'registry': 0}

while True:
  gc.collect()
  read_sensor()
  process_ir()
  update_door_auto_close()
  clear_security_alert_if_needed()
  apply_mode_logic()
  update_status_leds()
  update_lcd()

  now = time.ticks_ms()
  server_error_now = has_server_error()

  if time.ticks_diff(now, last['registry']) >= intervals['registry']:
    load_device_registry()
    last['registry'] = now
    intervals['registry'] = interval_with_jitter(120000 if not has_server_error() else 180000, 5000)

  if time.ticks_diff(now, last['state']) >= intervals['state']:
    fetch_device_state()
    last['state'] = now
    if has_server_error():
      intervals['state'] = interval_with_jitter(10000, 1000)
    else:
      intervals['state'] = interval_with_jitter(5000, 500)

  if time.ticks_diff(now, last['config']) >= intervals['config']:
    fetch_config()
    last['config'] = now
    if has_server_error():
      intervals['config'] = interval_with_jitter(120000, 5000)
    else:
      intervals['config'] = interval_with_jitter(60000, 5000)

  if time.ticks_diff(now, last['telemetry']) >= intervals['telemetry']:
    if (not has_server_error()) and sensor_is_valid():
      send_next_telemetry()
    last['telemetry'] = now
    if has_server_error():
      intervals['telemetry'] = interval_with_jitter(30000, 2000)
    else:
      intervals['telemetry'] = interval_with_jitter(15000, 1500)

  if time.ticks_diff(now, last['command']) >= intervals['command']:
    fetch_all_commands()
    last['command'] = now
    if has_server_error():
      intervals['command'] = interval_with_jitter(3000, 300)
    else:
      intervals['command'] = interval_with_jitter(1800, 200)

  print(
    'device=', DEVICE_NAME,
    'mode=', SYS['mode'],
    'fan_status=', SYS['fan_status'],
    'fan_speed=', SYS['fan_speed'],
    'light_status=', SYS['light_status'],
    'door_locked=', SYS['door_locked'],
    'door_open=', SYS['door_open'],
    'ir_typing=', SYS['ir_typing'],
    'failed_attempts=', SYS['failed_attempts'],
    'registry_error=', SYS['registry_error'],
    'state_error=', SYS['state_error'],
    'config_error=', SYS['config_error'],
    'telemetry_error=', SYS['telemetry_error'],
    'command_error=', SYS['command_error'],
    'server_error=', server_error_now,
    'sensor_error=', SYS['sensor_error'],
    'alert=', SYS['alert_active'],
    'security_alert=', SYS['security_alert_active']
  )

  time.sleep_ms(250)