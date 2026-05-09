# ULTRA OPTIMIZED - FULL LOGIC PRESERVED
from event_manager import *
import urequests,gc,ujson,time
from yolobit import *
from aiot_rgbled import RGBLed
from mqtt import *
from aiot_lcd1602 import LCD1602
from aiot_dht20 import DHT20
from aiot_ir_receiver import *

# ===== INIT =====
event_manager.reset()
button_a.on_pressed=button_b.on_pressed=None
button_a.on_pressed_ab=button_b.on_pressed_ab=-1

rgb=RGBLed(pin1.pin,4);lcd=LCD1602();dht=DHT20()
ir=IR_RX(Pin(pin10.pin,Pin.IN));ir.start()

try:
  from aiot_local_config import GATEWAY_HOST,GATEWAY_PORT,HOME_ID,DEVICE_TOKEN,WIFI_SSID,WIFI_PASS,RUNTIME_DEVICE_ID
except:
  GATEWAY_HOST='127.0.0.1';GATEWAY_PORT=9000;HOME_ID=1;DEVICE_TOKEN='ohstem-demo-token';WIFI_SSID='Test';WIFI_PASS='12345678';RUNTIME_DEVICE_ID=1
HOST=GATEWAY_HOST;PORT=GATEWAY_PORT
BASE='http://%s:%s'%(HOST,PORT)
HDR={'X-Device-Token':DEVICE_TOKEN,'Content-Type':'application/json'}

DOOR_PASS='123456';MAX_FAIL=3;DOOR_MS=5000

CFG={'Thigh':30.0,'Tlow':27.0,'Lhigh':55,'Llow':35,'Tsleep_high':32.0,'Tsleep_low':26.0,
'Taway_high':33.0,'Tcritical':35.0,'auto_fan_speed':70,'sleep_fan_speed':30,'away_fan_speed':60}

SYS={'mode':'away','prev_mode':None,'fan_status':'off','fan_speed':0,'light_status':'off',
'sensor_error':0,'state_error':0,'config_error':0,'telemetry_error':0,'command_error':0,'registry_error':0,
'alert_active':0,'security_alert_active':0,'nhiet_do':0,'do_am':0,'shine':0,'someone':0,
'pir_motion':0,'camera_human_detected':0,'camera_motion_detected':0,'camera_confidence':0.0,
'last_seen':0,'door_locked':1,'door_open':0,'door_until':None,'ir_typing':0,'ir_pass':'','failed':0,
'last_sec':0,'manual_override':0,'last_gc':0,'boot':0}

KEY={'runtime_id':None,'fan_id':None,'light_id':None,
'runtime_key':'yolobit-01','fan_key':'ohstem-fan-ctrl-01','light_key':'ohstem-light-ctrl-01'}

# ===== UTILS =====
def j(r):
  try:return ujson.loads(r.text)
  except:return None

def ok(r):
  try:return r and 200<=r.status_code<300
  except:return 0

def close(r):
  try:r and r.close()
  except:pass

def get(p):
  r=None
  try:
    url=BASE+p;print('[GET]',url)
    r=urequests.get(url,headers=HDR)
    print('[S]',r.status_code)
    d=j(r);print('[J]',d)
    return d.get('data') if isinstance(d,dict) else d
  except Exception as e:
    print('[GET ERR]',e);return None
  finally:close(r)

def post(p,d):
  r=None
  try:
    url=BASE+p;print('[POST]',url,d)
    r=urequests.post(url,json=d,headers=HDR)
    print('[S]',r.status_code)
    return ok(r)
  except Exception as e:
    print('[POST ERR]',e);return 0
  finally:close(r)

def clamp(v,a,b):return a if v<a else b if v>b else v

def has_err():
  return SYS['state_error'] or SYS['config_error'] or SYS['telemetry_error'] or SYS['command_error'] or SYS['registry_error']

# ===== HW =====
def fan(on,s): pin0.write_analog(round(translate(clamp(int(s),0,100),0,100,0,1023)) if on else 0)
def light(on): pin8.write_digital(1 if on else 0)
def door_hw(): pin4.servo_write(180 if SYS['door_open'] else 0)

# ===== SENSOR =====
def read():
  try:dht.read_dht20();SYS['nhiet_do']=dht.dht20_temperature();SYS['do_am']=dht.dht20_humidity()
  except:SYS['nhiet_do']=SYS['do_am']=None
  try:SYS['shine']=clamp(round(translate(pin2.read_analog(),0,4095,0,100)),0,100)
  except:SYS['shine']=None
  try:SYS['pir_motion']=pin16.read_digital()==1
  except:SYS['pir_motion']=0

# ===== YOLO =====
def yolo():
  d=get('/gw/yolo/check_human')
  if not isinstance(d,dict):SYS['camera_human_detected']=0;return
  SYS['camera_human_detected']=d.get('human_detected',0)
  SYS['camera_motion_detected']=d.get('motion_detected',0)
  SYS['camera_confidence']=d.get('max_confidence',0.0)

# ===== MOTION =====
def motion():
  a=SYS['camera_motion_detected'] or (SYS['pir_motion'] and SYS['camera_human_detected'] and SYS['camera_confidence']>=0.5)
  if a:SYS['someone']=1;SYS['last_seen']=time.ticks_ms()
  elif time.ticks_diff(time.ticks_ms(),SYS['last_seen'])>3000:SYS['someone']=0

# ===== MODE =====
def logic():
  if SYS['nhiet_do']==None or SYS['shine']==None:
    SYS['sensor_error']=1;SYS['fan_status']='off';SYS['light_status']='off';return
  SYS['sensor_error']=0
  t,l=SYS['nhiet_do'],SYS['shine']
  fs,sp,ls='off',0,'off'
  if SYS['mode']!='manual':
    if SYS['mode']=='sleep':
      if t>=CFG['Tsleep_high']:fs,sp='on',CFG['sleep_fan_speed']
    elif SYS['mode']=='away':
      if t>=CFG['Taway_high']:fs,sp='on',CFG['away_fan_speed']
    else:
      if t>=CFG['Thigh']:fs,sp='on',CFG['auto_fan_speed']
      elif t<=CFG['Tlow']:fs='off'
      if l<=CFG['Llow']:ls='on'
      elif l>=CFG['Lhigh']:ls='off'
    SYS['fan_status'],SYS['fan_speed'],SYS['light_status']=fs,sp,ls
  fan(SYS['fan_status']=='on',SYS['fan_speed']);light(SYS['light_status']=='on')

# ===== IR =====
nums=[IR_REMOTE_0,IR_REMOTE_1,IR_REMOTE_2,IR_REMOTE_3,IR_REMOTE_4,IR_REMOTE_5,IR_REMOTE_6,IR_REMOTE_7,IR_REMOTE_8,IR_REMOTE_9]

def ir_proc():
  try:c=ir.get_code()
  except:ir.start();return
  if not c:return
  if c in nums:
    SYS['ir_pass']+=str(nums.index(c))
  elif c==IR_REMOTE_F:
    if SYS['ir_pass']==DOOR_PASS:
      SYS['door_open']=1;SYS['door_locked']=0
      SYS['door_until']=time.ticks_add(time.ticks_ms(),DOOR_MS)
    else:
      SYS['failed']+=1
      if SYS['failed']>=MAX_FAIL:
        SYS['security_alert_active']=1
        post('/gw/homes/%s/alerts'%HOME_ID,{"deviceId":RUNTIME_DEVICE_ID,"sensorId":None,"type":"WRONG_PASSWORD","message":"Nhap sai mat khau"})
    SYS['ir_pass']=''
  ir.clear_code()

# ===== DOOR AUTO =====
def door_auto():
  if SYS['door_open'] and SYS['door_until']!=None:
    if time.ticks_diff(time.ticks_ms(),SYS['door_until'])>=0:
      SYS['door_open']=0;SYS['door_locked']=1;SYS['door_until']=None

# ===== DEBUG =====
def debug():
  print('[SYS]',SYS['mode'],SYS['nhiet_do'],SYS['do_am'],SYS['shine'],
  'pir',SYS['pir_motion'],'cam',SYS['camera_human_detected'],
  'move',SYS['someone'],'fan',SYS['fan_status'],SYS['fan_speed'],
  'light',SYS['light_status'],'door',SYS['door_open'],'err',has_err())

# ===== BOOT =====
mqtt.connect_wifi(WIFI_SSID,WIFI_PASS)
lcd.backlight_on();lcd.clear();lcd.putstr('Smart House')
SYS['boot']=time.ticks_ms()

last={'y':0,'dbg':0}

# ===== LOOP =====
while True:
  now=time.ticks_ms()
  if time.ticks_diff(now,SYS['last_gc'])>10000:gc.collect();SYS['last_gc']=now

  read();ir_proc();door_auto();door_hw()

  if time.ticks_diff(now,last['y'])>1500:
    if not SYS['ir_typing']:yolo()
    last['y']=now

  motion();logic()

  if SYS['nhiet_do']!=None and SYS['nhiet_do']>CFG['Tcritical']:
    post('/gw/homes/%s/alerts'%HOME_ID,{"deviceId":RUNTIME_DEVICE_ID,"sensorId":None,"type":"HIGH_TEMPERATURE","message":"Nhiet do cao"})

  if time.ticks_diff(now,last['dbg'])>5000:
    debug();last['dbg']=now

  time.sleep_ms(200)
