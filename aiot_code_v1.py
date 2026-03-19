from event_manager import *
import urequests
import gc
import ujson
from yolobit import *
from aiot_rgbled import RGBLed
from mqtt import *
from aiot_lcd1602 import LCD1602
from aiot_dht20 import DHT20
from aiot_ir_receiver import *
import music
import time

# =========================
# KHOI TAO
# =========================
button_a.on_pressed = None
button_b.on_pressed = None
button_a.on_pressed_ab = button_b.on_pressed_ab = -1

event_manager.reset()

tiny_rgb = RGBLed(pin1.pin, 4)
aiot_lcd1602 = LCD1602()
aiot_dht20 = DHT20()
aiot_ir_rx = IR_RX(Pin(pin10.pin, Pin.IN))
aiot_ir_rx.start()

SERVER_HOST = '10.229.90.208'
SERVER_PORT = 8080
server_ip = 'http://{}:{}'.format(SERVER_HOST, SERVER_PORT)

# =========================================================
# THONG TIN HE THONG
# chi giu HOME_ID, con lai tu load dong tu backend
# =========================================================
HOME_ID = 1
DEVICE_NAME = 'OhStem Living Room'

device_registry = {}

controller_device_id = None
controller_device_key = None

fan_control_device_id = None
fan_control_device_key = None

light_control_device_id = None
light_control_device_key = None

temp_device_key = None
humidity_device_key = None
light_sensor_device_key = None
motion_device_key = None

# =========================
# MAU SAC
# =========================
COLOR_RED = '#ff0000'
COLOR_GREEN = '#00ff00'
COLOR_OFF = '#000000'
COLOR_YELLOW = '#ffff00'

# =========================
# TRANG THAI THIET BI
# =========================
current_mode = 'away'
prev_mode = None
fan_status = 'off'
fan_speed = 0
light_status = 'off'
hold_until = None

# =========================
# TRANG THAI HE THONG
# =========================
sensor_error = False
state_error = False
config_error = False
telemetry_error = False
command_error = False
registry_error = False
alert_active = False

# =========================
# CAU HINH MAC DINH TAM THOI
# =========================
Thigh = 30.0
Tlow = 27.0
Lhigh = 55
Llow = 35
Tsleep_high = 32.0
Tsleep_low = 26.0
Taway_high = 33.0
Tcritical = 35.0
N_minutes = 2
M_minutes = 2
Thold_minutes = 5
auto_fan_speed = 70
sleep_fan_speed = 30
away_fan_speed = 60

# =========================
# DU LIEU CAM BIEN
# =========================
nhiet_do = 0
do_am = 0
shine = 0
someone = False

# =========================
# CAU HINH KHOA CUA IR
# =========================
DOOR_PASSWORD = '123456'
MAX_FAILED_ATTEMPTS = 3
DOOR_OPEN_MS = 5000

DOOR_RELAY_PIN = pin14

# =========================
# TRANG THAI KHOA CUA
# =========================
door_locked = True
door_open = False
door_open_until = None

ir_typing = False
ir_pass = ''
failed_attempts = 0
security_alert_active = False
last_security_alert_ms = 0

last_lcd_line1 = ''
last_lcd_line2 = ''

# =========================
# HAM TIEN ICH
# =========================
def safe_json_text(resp):
  try:
    return ujson.loads(resp.text)
  except Exception as e:
    print('JSON loi:', e)
    try:
      print(resp.text)
    except:
      pass
    return None


def has_server_error():
  return state_error or config_error or telemetry_error or command_error or registry_error


def safe_close(resp):
  try:
    if resp is not None:
      resp.close()
  except:
    pass


def unwrap_data(payload):
  if payload is None:
    return None
  if isinstance(payload, dict) and 'data' in payload:
    return payload.get('data')
  return payload


def get_message(payload):
  if isinstance(payload, dict):
    return payload.get('message')
  return None


def is_success_http(resp):
  try:
    return resp is not None and resp.status_code >= 200 and resp.status_code < 300
  except:
    return False


def clamp(value, min_value, max_value):
  if value < min_value:
    return min_value
  if value > max_value:
    return max_value
  return value


def to_int(value, default_value=0):
  try:
    return int(value)
  except:
    try:
      return int(float(value))
    except:
      return default_value


def to_float(value, default_value=0.0):
  try:
    return float(value)
  except:
    return default_value


def equals_ignore_case(a, b):
  if a is None or b is None:
    return False
  return str(a).strip().lower() == str(b).strip().lower()


def contains_ignore_case(text, keyword):
  if text is None or keyword is None:
    return False
  return str(keyword).strip().lower() in str(text).strip().lower()


def normalize_device_type(value):
  if value is None:
    return ''
  return str(value).strip().upper()


def normalize_text(value):
  if value is None:
    return ''
  return str(value).strip().lower()


def contains_any(text, keywords):
  text_norm = normalize_text(text)
  for keyword in keywords:
    if normalize_text(keyword) in text_norm:
      return True
  return False


def log_json(label, data):
  try:
    print(label + ': ' + ujson.dumps(data))
  except Exception as e:
    print(label + ' (json dump loi):', e)
    print(data)


def batquat(is_on, speed):
  speed = clamp(int(speed), 0, 100)
  if is_on:
    pin0.write_analog(round(translate(speed, 0, 100, 0, 1023)))
  else:
    pin0.write_analog(0)


def batden(is_on):
  if is_on:
    pin8.write_digital(1)
  else:
    pin8.write_digital(0)


def mode_text_short():
  if current_mode == 'auto':
    return 'A'
  if current_mode == 'manual':
    return 'M'
  if current_mode == 'sleep':
    return 'S'
  if current_mode == 'away':
    return 'W'
  return '?'


def set_door_relay(is_open):
  try:
    if is_open:
      DOOR_RELAY_PIN.write_digital(1)
    else:
      DOOR_RELAY_PIN.write_digital(0)
  except Exception as e:
    print('Loi relay cua:', e)


def open_door():
  global door_open, door_locked, door_open_until
  door_open = True
  door_locked = False
  door_open_until = time.ticks_add(time.ticks_ms(), DOOR_OPEN_MS)
  set_door_relay(True)


def close_door():
  global door_open, door_locked, door_open_until, ir_typing, ir_pass
  door_open = False
  door_locked = True
  door_open_until = None
  ir_typing = False
  ir_pass = ''
  set_door_relay(False)


def update_door_auto_close():
  global door_open, door_open_until
  if door_open and door_open_until is not None:
    if time.ticks_diff(time.ticks_ms(), door_open_until) >= 0:
      close_door()


def mask_pass(text):
  masked = ''
  for _ in text:
    masked = masked + '*'
  return masked


def device_match(device, type_name=None, key_words=None, name_words=None):
  if type_name is not None:
    if normalize_device_type(device.get('type')) == normalize_device_type(type_name):
      return True

  if key_words is not None:
    if contains_any(device.get('deviceKey'), key_words):
      return True

  if name_words is not None:
    if contains_any(device.get('name'), name_words):
      return True

  return False


def find_first(devices, predicate):
  for d in devices:
    try:
      if predicate(d):
        return d
    except:
      pass
  return None


def choose_runtime_device(devices):
  for t in ['SENSOR_NODE', 'HUB', 'OTHER']:
    d = find_first(devices, lambda x: device_match(x, type_name=t))
    if d is not None:
      return d
  if len(devices) > 0:
    return devices[0]
  return None


def is_runtime_device(device):
  if device.get('mode') is not None:
    return True
  if device_match(device, type_name='SENSOR_NODE'):
    return True
  if device_match(device, type_name='HUB'):
    return True
  if device_match(device, key_words=['yolobit', 'controller', 'hub']):
    return True
  if device_match(device, name_words=['controller', 'hub', 'trung tam', 'điều khiển', 'dieu khien']):
    return True
  return False


def is_fan_device(device):
  return device_match(
    device,
    type_name='FAN',
    key_words=['fan', 'quat'],
    name_words=['fan', 'quạt', 'quat']
  )


def is_light_device(device):
  return device_match(
    device,
    type_name='LIGHT',
    key_words=['light', 'den'],
    name_words=['light', 'đèn', 'den']
  )


def is_temp_device(device):
  return device_match(
    device,
    key_words=['temp', 'temperature', 'nhiet'],
    name_words=['temp', 'temperature', 'nhiệt', 'nhiet']
  )


def is_humidity_device(device):
  return device_match(
    device,
    key_words=['humidity', 'humid', 'do-am'],
    name_words=['humidity', 'humid', 'độ ẩm', 'do am']
  )


def is_light_sensor_device(device):
  if device is None:
    return False

  if is_light_device(device):
    return False

  key = normalize_text(device.get('deviceKey'))
  if key == 'ohstem-light-01':
    return True

  return False


def is_motion_device(device):
  return device_match(
    device,
    key_words=['motion', 'pir', 'presence'],
    name_words=['motion', 'pir', 'hiện diện', 'hien dien', 'chuyển động', 'chuyen dong']
  )


def find_by_exact_key(devices, device_key):
  key_norm = normalize_text(device_key)
  return find_first(devices, lambda d: normalize_text(d.get('deviceKey')) == key_norm)


# =========================
# DEVICE REGISTRY
# =========================
def load_device_registry():
  global device_registry, registry_error
  global controller_device_id, controller_device_key
  global fan_control_device_id, fan_control_device_key
  global light_control_device_id, light_control_device_key
  global temp_device_key, humidity_device_key, light_sensor_device_key, motion_device_key

  resp = None
  try:
    resp = urequests.get(server_ip + '/api/devices/home/' + str(HOME_ID))
    payload = safe_json_text(resp)
    data = unwrap_data(payload)

    if data is None:
      registry_error = True
      print('Registry data = None')
      return False

    if not isinstance(data, list):
      registry_error = True
      print('Registry data khong phai list:', data)
      return False

    if len(data) == 0:
      registry_error = True
      print('Registry list rong')
      return False

    runtime_device = find_by_exact_key(data, 'yolobit-01')
    if runtime_device is None:
      runtime_device = find_first(data, is_runtime_device)
    if runtime_device is None:
      runtime_device = choose_runtime_device(data)

    fan_device = find_by_exact_key(data, 'ohstem-fan-ctrl-01')
    if fan_device is None:
      fan_device = find_first(data, is_fan_device)

    light_device = find_by_exact_key(data, 'ohstem-light-ctrl-01')
    if light_device is None:
      light_device = find_first(data, is_light_device)

    temp_device = find_by_exact_key(data, 'ohstem-temp-01')
    if temp_device is None:
      temp_device = find_first(data, is_temp_device)

    humidity_device = find_by_exact_key(data, 'ohstem-humidity-01')
    if humidity_device is None:
      humidity_device = find_first(data, is_humidity_device)

    lux_device = find_by_exact_key(data, 'ohstem-light-01')
    if lux_device is None:
      lux_device = find_first(data, is_light_sensor_device)

    motion_device = find_by_exact_key(data, 'ohstem-motion-01')
    if motion_device is None:
      motion_device = find_first(data, is_motion_device)

    if runtime_device is None:
      registry_error = True
      print('Khong tim thay runtime device')
      return False

    if fan_device is None:
      registry_error = True
      print('Khong tim thay fan device')
      return False

    if light_device is None:
      registry_error = True
      print('Khong tim thay light device')
      return False

    controller_device_id = runtime_device.get('id')
    controller_device_key = runtime_device.get('deviceKey')

    fan_control_device_id = fan_device.get('id')
    fan_control_device_key = fan_device.get('deviceKey')

    light_control_device_id = light_device.get('id')
    light_control_device_key = light_device.get('deviceKey')

    temp_device_key = temp_device.get('deviceKey') if temp_device is not None else 'ohstem-temp-01'
    humidity_device_key = humidity_device.get('deviceKey') if humidity_device is not None else 'ohstem-humidity-01'
    light_sensor_device_key = lux_device.get('deviceKey') if lux_device is not None else 'ohstem-light-01'
    motion_device_key = motion_device.get('deviceKey') if motion_device is not None else 'ohstem-motion-01'

    device_registry = {
      'runtime': runtime_device,
      'fan': fan_device,
      'light': light_device,
      'temp': temp_device,
      'humidity': humidity_device,
      'light_sensor': lux_device,
      'motion': motion_device
    }

    registry_error = False
    print(
      'registry ok:',
      'controller=', controller_device_key,
      'fan=', fan_control_device_key,
      'light=', light_control_device_key,
      'temp=', temp_device_key,
      'humidity=', humidity_device_key,
      'lux=', light_sensor_device_key,
      'motion=', motion_device_key
    )
    return True

  except Exception as e:
    registry_error = True
    print('Loi load_device_registry:', e)
    return False
  finally:
    safe_close(resp)


# =========================
# ALERT
# =========================
def trigger_security_alert(reason, detail):
  global security_alert_active, last_security_alert_ms
  security_alert_active = True
  last_security_alert_ms = time.ticks_ms()
  print('SECURITY ALERT:', reason, detail)
  send_security_alert(reason, detail)


def clear_security_alert_if_needed():
  global security_alert_active
  if security_alert_active and time.ticks_diff(time.ticks_ms(), last_security_alert_ms) >= 15000:
    security_alert_active = False


def send_security_alert(reason, detail):
  resp = None
  try:
    if controller_device_id is None:
      print('Bo qua send_security_alert vi chua co registry')
      return

    payload = {
      "deviceId": controller_device_id,
      "sensorId": None,
      "type": reason,
      "message": detail
    }

    resp = urequests.post(
      server_ip + '/api/homes/' + str(HOME_ID) + '/alerts',
      json=payload,
      data=None,
      headers={}
    )

    if is_success_http(resp):
      print('send_security_alert ok:', resp.text)
    else:
      try:
        print('send_security_alert fail:', resp.status_code, resp.text)
      except:
        print('send_security_alert fail')

  except Exception as e:
    print('Khong gui duoc alert len server:', e)
  finally:
    safe_close(resp)


# =========================
# IR
# =========================
def append_ir_digit(digit):
  global ir_pass
  if len(ir_pass) < 12:
    ir_pass = ir_pass + str(digit)
    print('ir_pass =', ir_pass)
    music.play(['C4:0.1'], wait=False)
    update_lcd()


def handle_wrong_password():
  global ir_pass, ir_typing, failed_attempts
  failed_attempts = failed_attempts + 1
  print('Sai mat khau, lan =', failed_attempts)
  music.play(['C5:0.2', 'C4:0.2'], wait=False)
  ir_pass = ''
  ir_typing = False

  if failed_attempts == MAX_FAILED_ATTEMPTS:
    trigger_security_alert('WRONG_PASSWORD', 'Nhap sai mat khau qua 3 lan')

  update_lcd()


def handle_correct_password():
  global ir_pass, ir_typing, failed_attempts, security_alert_active
  music.play(['C4:0.2', 'E4:0.2', 'G4:0.2'], wait=False)
  ir_pass = ''
  ir_typing = False
  failed_attempts = 0
  security_alert_active = False
  open_door()
  update_lcd()


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


def process_ir():
  global ir_pass, ir_typing

  code = aiot_ir_rx.get_code()
  if code is None:
    return

  print('IR code =', code)

  try:
    if code == IR_REMOTE_SETUP:
      print('Nhan SETUP')
      if door_locked:
        ir_typing = not ir_typing
        ir_pass = ''
        music.play(['G3:0.2'], wait=False)
        update_lcd()
      else:
        music.play(['G3:0.1'], wait=False)

    elif ir_typing:
      if code in IR_DIGIT_MAP:
        digit = IR_DIGIT_MAP[code]
        print('Nhan so =', digit)
        append_ir_digit(digit)

      elif code == IR_REMOTE_F:
        print('Nhan F de xac nhan')
        if ir_pass == DOOR_PASSWORD:
          handle_correct_password()
        else:
          handle_wrong_password()

    else:
      if code == IR_REMOTE_F and not door_locked:
        print('Nhan F de dong cua')
        music.play(['C4:0.2'], wait=False)
        close_door()
        update_lcd()

  except Exception as e:
    print('Loi process_ir:', e)
  finally:
    aiot_ir_rx.clear_code()


# =========================
# LED / LCD
# =========================
def update_status_leds():
  blink_on = ((time.ticks_ms() // 300) % 2) == 0
  server_error_now = has_server_error()

  tiny_rgb.show(1, hex_to_rgb(COLOR_RED if sensor_error and blink_on else COLOR_OFF))
  tiny_rgb.show(2, hex_to_rgb(COLOR_RED if server_error_now and blink_on else COLOR_OFF))

  if sensor_error or server_error_now:
    led3 = COLOR_YELLOW if blink_on else COLOR_OFF
  else:
    led3 = COLOR_GREEN if (light_status == 'on' or door_open) else COLOR_OFF
  tiny_rgb.show(3, hex_to_rgb(led3))

  tiny_rgb.show(4, hex_to_rgb(COLOR_RED if (alert_active or security_alert_active) and blink_on else COLOR_OFF))


def fit16(text):
  text = str(text)
  if len(text) > 16:
    return text[:16]
  return text + (' ' * (16 - len(text)))


def update_lcd():
  global last_lcd_line1, last_lcd_line2

  temp_text = '--' if nhiet_do is None else str(round(nhiet_do, 1))
  hum_text = '--' if do_am is None else str(round(do_am, 1))
  light_text = '--' if shine is None else str(shine)

  line1 = fit16('T:' + temp_text + ' H:' + hum_text)
  server_error_now = has_server_error()

  if sensor_error and server_error_now:
    status_text = 'E:SEN+SRV'
  elif sensor_error:
    status_text = 'E:SENSOR'
  elif server_error_now:
    status_text = 'E:SERVER'
  else:
    door_text = 'D1' if door_open else 'D0'
    lock_text = 'K1' if door_locked else 'K0'

    if ir_typing:
      input_text = mask_pass(ir_pass)
      if len(input_text) > 4:
        input_text = input_text[-4:]
      status_text = lock_text + ' ' + door_text + ' ' + input_text
    else:
      motion_text = 'P1' if someone else 'P0'
      status_text = mode_text_short() + ' ' + motion_text + ' ' + door_text + ' ' + lock_text

  line2 = fit16('L:' + light_text + ' ' + status_text)

  if line1 != last_lcd_line1:
    aiot_lcd1602.move_to(0, 0)
    aiot_lcd1602.putstr(line1)
    last_lcd_line1 = line1

  if line2 != last_lcd_line2:
    aiot_lcd1602.move_to(0, 1)
    aiot_lcd1602.putstr(line2)
    last_lcd_line2 = line2


# =========================
# SENSOR
# =========================
def read_sensor():
  global nhiet_do, do_am, shine, someone

  try:
    aiot_dht20.read_dht20()
    nhiet_do = aiot_dht20.dht20_temperature()
    do_am = aiot_dht20.dht20_humidity()
  except Exception as e:
    print('Loi DHT20:', e)
    nhiet_do = None
    do_am = None

  try:
    shine = round(translate(pin2.read_analog(), 0, 4095, 0, 100))
    shine = clamp(shine, 0, 100)
  except Exception as e:
    print('Loi cam bien anh sang:', e)
    shine = None

  try:
    someone = (pin16.read_digital() == 1)
  except Exception as e:
    print('Loi cam bien hien dien:', e)
    someone = False


def sensor_is_valid():
  if nhiet_do is None or do_am is None or shine is None:
    return False
  if nhiet_do < -10 or nhiet_do > 80:
    return False
  if do_am < 0 or do_am > 100:
    return False
  if shine < 0 or shine > 100:
    return False
  return True


def apply_mode_logic():
  global sensor_error, alert_active
  global fan_status, light_status, fan_speed

  if not sensor_is_valid():
    sensor_error = True
    alert_active = False
    fan_status = 'off'
    fan_speed = 0
    light_status = 'off'
    batquat(False, 0)
    batden(False)
    return

  sensor_error = False

  if fan_status not in ['on', 'off']:
    fan_status = 'off'
  if light_status not in ['on', 'off']:
    light_status = 'off'

  fan_speed = clamp(to_int(fan_speed, 0), 0, 100)

  if current_mode == 'manual':
    pass
  else:
    next_fan_status = 'off'
    next_fan_speed = 0
    next_light_status = 'off'

    if current_mode == 'sleep':
      if nhiet_do is not None and nhiet_do >= Tsleep_high:
        next_fan_status = 'on'
        next_fan_speed = sleep_fan_speed
      elif nhiet_do is not None and nhiet_do <= Tsleep_low:
        next_fan_status = 'off'
        next_fan_speed = 0

    elif current_mode == 'away':
      if nhiet_do is not None and nhiet_do >= Taway_high:
        next_fan_status = 'on'
        next_fan_speed = away_fan_speed
      else:
        next_fan_status = 'off'
        next_fan_speed = 0

    else:  # auto
      if nhiet_do is not None and nhiet_do >= Thigh:
        next_fan_status = 'on'
        next_fan_speed = auto_fan_speed
      elif nhiet_do is not None and nhiet_do <= Tlow:
        next_fan_status = 'off'
        next_fan_speed = 0

      if shine is not None and shine <= Llow:
        next_light_status = 'on'
      elif shine is not None and shine >= Lhigh:
        next_light_status = 'off'

    fan_status = next_fan_status
    fan_speed = next_fan_speed
    light_status = next_light_status

  if fan_status == 'on' and fan_speed <= 0:
    if current_mode == 'sleep':
      fan_speed = sleep_fan_speed
    elif current_mode == 'away':
      fan_speed = away_fan_speed
    else:
      fan_speed = auto_fan_speed

  batquat(fan_status == 'on', fan_speed if fan_status == 'on' else 0)
  batden(light_status == 'on')

  temp_critical = (nhiet_do is not None and nhiet_do > Tcritical)
  away_motion_alert = (current_mode == 'away' and someone)
  alert_active = temp_critical or away_motion_alert or security_alert_active


# =========================
# FETCH DEVICE STATE
# =========================
def fetch_state_by_device_id(device_id):
  resp = None
  try:
    if device_id is None:
      return None

    resp = urequests.get(server_ip + '/api/devices/' + str(device_id) + '/state')
    payload = safe_json_text(resp)
    data = unwrap_data(payload)

    if data is None:
      return None

    return data

  except Exception as e:
    print('Loi fetch_state_by_device_id:', device_id, e)
    return None
  finally:
    safe_close(resp)


def fetch_device_state():
  global current_mode, fan_status, fan_speed, light_status
  global hold_until, prev_mode, state_error

  ok = True

  controller_state = fetch_state_by_device_id(controller_device_id)
  if controller_state is not None:
    log_json('controller state', controller_state)

    if 'mode' in controller_state and controller_state['mode'] is not None:
      current_mode = str(controller_state['mode']).lower()

    if 'holdUntil' in controller_state:
      hold_until = controller_state.get('holdUntil')

    if 'prevMode' in controller_state:
      prev_mode = controller_state.get('prevMode')
  else:
    ok = False

  fan_state = fetch_state_by_device_id(fan_control_device_id)
  if fan_state is not None:
    log_json('fan state', fan_state)

    if 'fanStatus' in fan_state and fan_state['fanStatus'] is not None:
      fan_status = str(fan_state['fanStatus']).lower()

    if 'fanSpeed' in fan_state and fan_state['fanSpeed'] is not None:
      fan_speed = clamp(to_int(fan_state['fanSpeed'], fan_speed), 0, 100)
  else:
    ok = False

  light_state = fetch_state_by_device_id(light_control_device_id)
  if light_state is not None:
    log_json('light state', light_state)

    if 'lightStatus' in light_state and light_state['lightStatus'] is not None:
      light_status = str(light_state['lightStatus']).lower()
  else:
    ok = False

  state_error = not ok

  if not state_error:
    print(
      'device state ok:',
      'mode=', current_mode,
      'fanStatus=', fan_status,
      'fanSpeed=', fan_speed,
      'lightStatus=', light_status,
      'holdUntil=', hold_until,
      'prevMode=', prev_mode
    )


# =========================
# CONFIG
# =========================
def fetch_config():
  global Thigh, Tlow, Lhigh, Llow, Tsleep_high, Tsleep_low, Taway_high
  global Tcritical, N_minutes, M_minutes, Thold_minutes, config_error
  global auto_fan_speed, sleep_fan_speed, away_fan_speed

  resp = None
  try:
    resp = urequests.get(server_ip + '/api/homes/' + str(HOME_ID) + '/configs')
    payload = safe_json_text(resp)
    cfg = unwrap_data(payload)

    if cfg is None:
      config_error = True
      print('config data = None')
      return

    if not isinstance(cfg, dict):
      config_error = True
      print('config data khong hop le:', cfg)
      return

    if 'thigh' in cfg and cfg['thigh'] is not None:
      Thigh = to_float(cfg['thigh'], Thigh)
    if 'tlow' in cfg and cfg['tlow'] is not None:
      Tlow = to_float(cfg['tlow'], Tlow)
    if 'lhigh' in cfg and cfg['lhigh'] is not None:
      Lhigh = to_int(cfg['lhigh'], Lhigh)
    if 'llow' in cfg and cfg['llow'] is not None:
      Llow = to_int(cfg['llow'], Llow)
    if 'tsleepHigh' in cfg and cfg['tsleepHigh'] is not None:
      Tsleep_high = to_float(cfg['tsleepHigh'], Tsleep_high)
    if 'tsleepLow' in cfg and cfg['tsleepLow'] is not None:
      Tsleep_low = to_float(cfg['tsleepLow'], Tsleep_low)
    if 'tawayHigh' in cfg and cfg['tawayHigh'] is not None:
      Taway_high = to_float(cfg['tawayHigh'], Taway_high)
    if 'tcritical' in cfg and cfg['tcritical'] is not None:
      Tcritical = to_float(cfg['tcritical'], Tcritical)
    if 'nMinutes' in cfg and cfg['nMinutes'] is not None:
      N_minutes = to_int(cfg['nMinutes'], N_minutes)
    if 'mMinutes' in cfg and cfg['mMinutes'] is not None:
      M_minutes = to_int(cfg['mMinutes'], M_minutes)
    if 'tholdMinutes' in cfg and cfg['tholdMinutes'] is not None:
      Thold_minutes = to_int(cfg['tholdMinutes'], Thold_minutes)

    if 'autoFanSpeed' in cfg and cfg['autoFanSpeed'] is not None:
      auto_fan_speed = clamp(to_int(cfg['autoFanSpeed'], auto_fan_speed), 0, 100)
    if 'sleepFanSpeed' in cfg and cfg['sleepFanSpeed'] is not None:
      sleep_fan_speed = clamp(to_int(cfg['sleepFanSpeed'], sleep_fan_speed), 0, 100)
    if 'awayFanSpeed' in cfg and cfg['awayFanSpeed'] is not None:
      away_fan_speed = clamp(to_int(cfg['awayFanSpeed'], away_fan_speed), 0, 100)

    config_error = False
    print(
      'config ok:',
      'Thigh=', Thigh,
      'Tlow=', Tlow,
      'Lhigh=', Lhigh,
      'Llow=', Llow,
      'Tsleep_high=', Tsleep_high,
      'Tsleep_low=', Tsleep_low,
      'Taway_high=', Taway_high,
      'Tcritical=', Tcritical,
      'autoFanSpeed=', auto_fan_speed,
      'sleepFanSpeed=', sleep_fan_speed,
      'awayFanSpeed=', away_fan_speed
    )

  except Exception as e:
    config_error = True
    print('Loi fetch_config:', e)
  finally:
    safe_close(resp)

# =========================
# TELEMETRY
# =========================
def send_one_telemetry(device_key, sensor_type, value):
  global telemetry_error
  resp = None

  try:
    if device_key is None:
      telemetry_error = True
      print('Bo qua telemetry vi device_key None:', sensor_type)
      return

    payload = {
      "deviceKey": device_key,
      "sensorType": sensor_type,
      "value": value
    }

    resp = urequests.post(
      server_ip + '/api/device-telemetry',
      json=payload,
      data=None,
      headers={}
    )

    if is_success_http(resp):
      telemetry_error = False
      print('telemetry ok', device_key, sensor_type, ':', resp.text)
    else:
      telemetry_error = True
      try:
        print('telemetry fail', device_key, sensor_type, ':', resp.status_code, resp.text)
      except:
        print('telemetry fail', device_key, sensor_type)

  except Exception as e:
    telemetry_error = True
    print('Loi send_one_telemetry:', device_key, sensor_type, e)
  finally:
    safe_close(resp)


def send_telemetry():
  if nhiet_do is not None:
    print('TEMP KEY =', temp_device_key)
    send_one_telemetry(temp_device_key, 'temperature', nhiet_do)

  if do_am is not None:
    print('HUMIDITY KEY =', humidity_device_key)
    send_one_telemetry(humidity_device_key, 'humidity', do_am)

  if shine is not None:
    print('LIGHT KEY =', light_sensor_device_key, 'VALUE =', shine)
    send_one_telemetry(light_sensor_device_key, 'light', shine)

  print('MOTION KEY =', motion_device_key, 'VALUE =', bool(someone))
  send_one_telemetry(motion_device_key, 'motion', bool(someone))


# =========================
# COMMAND HANDLING
# =========================
def fetch_next_command_for_device(device_key):
  resp = None
  try:
    if device_key is None:
      return None

    resp = urequests.get(server_ip + '/api/v1/device/' + device_key + '/commands/next')

    payload = safe_json_text(resp)
    if payload is None:
      return None

    data = unwrap_data(payload)
    if data is None:
      return None

    if not isinstance(data, dict):
      return None

    if 'id' not in data or data['id'] is None:
      return None

    print('command ' + device_key + ':', data)
    return data

  except Exception as e:
    print('Loi fetch_next_command_for_device:', device_key, e)
    return None
  finally:
    safe_close(resp)

def ack_command(device_key, id_value):
  resp = None
  try:
    if device_key is None or id_value is None:
      return False

    resp = urequests.post(
      server_ip + '/api/v1/device/' + device_key + '/commands/ack',
      json={"id": id_value},
      data=None,
      headers={}
    )

    if is_success_http(resp):
      print('ack cmd ok', device_key, ':', resp.text)
      return True

    try:
      print('ack cmd fail', device_key, ':', resp.status_code, resp.text)
    except:
      print('ack cmd fail', device_key)
    return False

  except Exception as e:
    print('Loi ack_command:', device_key, e)
    return False
  finally:
    safe_close(resp)


def normalize_command_target(target):
  if target is None:
    return ''
  target = str(target).strip().lower().replace('-', '_')

  if target in ['fanstatus', 'fan_status']:
    return 'fan'
  if target in ['fanspeed', 'fan_speed']:
    return 'fan_speed'
  if target in ['lightstatus', 'light_status']:
    return 'light'
  if target in ['lightlevel', 'light_level']:
    return 'light_level'
  return target


def process_runtime_command(data):
  global current_mode, prev_mode

  command_id = data.get('id')
  target = normalize_command_target(data.get('target'))
  value = data.get('value')

  if command_id is None:
    return False

  if target == 'mode':
    if value is not None:
      prev_mode = current_mode
      current_mode = str(value).lower()
    return ack_command(controller_device_key, command_id)

  print('Runtime khong xu ly target:', target, value)
  return ack_command(controller_device_key, command_id)


def process_fan_command(data):
  global fan_status, fan_speed

  command_id = data.get('id')
  target = normalize_command_target(data.get('target'))
  value = data.get('value')

  if command_id is None:
    return False

  if target == 'fan':
    if value is not None:
      fan_status = str(value).lower()
      if fan_status == 'off':
        fan_speed = 0
      elif fan_status == 'on' and fan_speed <= 0:
        fan_speed = 50
    return ack_command(fan_control_device_key, command_id)

  if target == 'fan_speed':
    fan_speed = clamp(to_int(value, fan_speed), 0, 100)
    if fan_speed > 0:
      fan_status = 'on'
    else:
      fan_status = 'off'
    return ack_command(fan_control_device_key, command_id)

  print('Fan khong xu ly target:', target, value)
  return ack_command(fan_control_device_key, command_id)


def process_light_command(data):
  global light_status

  command_id = data.get('id')
  target = normalize_command_target(data.get('target'))
  value = data.get('value')

  if command_id is None:
    return False

  if target == 'light':
    if value is not None:
      light_status = str(value).lower()
    return ack_command(light_control_device_key, command_id)

  if target == 'light_level':
    return ack_command(light_control_device_key, command_id)

  print('Light khong xu ly target:', target, value)
  return ack_command(light_control_device_key, command_id)


def fetch_all_commands():
  global command_error

  ok = True

  data = fetch_next_command_for_device(controller_device_key)
  if data is not None:
    if not process_runtime_command(data):
      ok = False

  data = fetch_next_command_for_device(fan_control_device_key)
  if data is not None:
    if not process_fan_command(data):
      ok = False

  data = fetch_next_command_for_device(light_control_device_key)
  if data is not None:
    if not process_light_command(data):
      ok = False

  command_error = not ok


def control_device(target, value):
  # Firmware nay khong goi direct manual control API.
  # Dung luong moi:
  # Frontend/Backend -> tao command
  # Device poll /commands/next -> xu ly -> /commands/ack
  print('Bo qua control_device direct:', target, value)
  return False


# =========================
# BOOT
# =========================
display.scroll('IoT')
mqtt.connect_wifi('Test', '12345678')
aiot_lcd1602.backlight_on()
aiot_lcd1602.clear()
aiot_lcd1602.move_to(0, 0)
aiot_lcd1602.putstr('Smart House')
aiot_lcd1602.move_to(0, 1)
aiot_lcd1602.putstr('Starting...')

update_status_leds()
close_door()

load_device_registry()
fetch_config()
fetch_device_state()
fetch_all_commands()

last_state_ms = 0
last_config_ms = 0
last_telemetry_ms = 0
last_command_ms = 0
last_registry_ms = 0

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

  registry_interval = 60000
  state_interval = 5000 if server_error_now else 2000
  config_interval = 60000 if server_error_now else 20000
  command_interval = 700
  telemetry_interval = 30000 if server_error_now else 8000

  if time.ticks_diff(now, last_registry_ms) >= registry_interval:
    load_device_registry()
    last_registry_ms = now

  if time.ticks_diff(now, last_state_ms) >= state_interval:
    fetch_device_state()
    last_state_ms = now

  if time.ticks_diff(now, last_config_ms) >= config_interval:
    fetch_config()
    last_config_ms = now

  if (not server_error_now) and time.ticks_diff(now, last_telemetry_ms) >= telemetry_interval:
    if sensor_is_valid():
      send_telemetry()
    last_telemetry_ms = now

  if time.ticks_diff(now, last_command_ms) >= command_interval:
    fetch_all_commands()
    last_command_ms = now

  print(
    'device=', DEVICE_NAME,
    'mode=', current_mode,
    'fan_status=', fan_status,
    'fan_speed=', fan_speed,
    'light_status=', light_status,
    'door_locked=', door_locked,
    'door_open=', door_open,
    'ir_typing=', ir_typing,
    'failed_attempts=', failed_attempts,
    'registry_error=', registry_error,
    'state_error=', state_error,
    'config_error=', config_error,
    'telemetry_error=', telemetry_error,
    'command_error=', command_error,
    'server_error=', server_error_now,
    'sensor_error=', sensor_error,
    'alert=', alert_active,
    'security_alert=', security_alert_active
  )

  time.sleep_ms(200)