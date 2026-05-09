import urequests,ujson,gc,time
from yolobit import *;from aiot_rgbled import RGBLed;from mqtt import *;from aiot_lcd1602 import LCD1602;from aiot_dht20 import DHT20;from aiot_ir_receiver import *

try:
    import usocket
    usocket.setdefaulttimeout(1.2)
except:
    pass

DEBUG=False;PERF_LOG=False;STATUS_LOG=False
button_a.on_pressed=button_b.on_pressed=None;button_a.on_pressed_ab=button_b.on_pressed_ab=-1
rgb=RGBLed(pin1.pin,4);lcd=LCD1602();dht=DHT20()

try:
    from aiot_local_config import GATEWAY_HOST,GATEWAY_PORT,HOME_ID,DEVICE_TOKEN,WIFI_SSID as CFG_WIFI_SSID,WIFI_PASS as CFG_WIFI_PASS
except:
    # IMPORTANT: 127.0.0.1 is the YoloBit itself, not the computer.
    # Put a real LAN IP in aiot_local_config.py.
    GATEWAY_HOST='192.168.1.114';GATEWAY_PORT=9000;HOME_ID=1;DEVICE_TOKEN='ohstem-demo-token';CFG_WIFI_SSID='Test';CFG_WIFI_PASS='12345678'

HOST,PORT,HOME=GATEWAY_HOST,GATEWAY_PORT,HOME_ID
BASE='http://%s:%s'%(HOST,PORT)
HEAD={'X-Device-Token':DEVICE_TOKEN,'Content-Type':'application/json','Connection':'close'}

WIFI_SSID,WIFI_PASS=CFG_WIFI_SSID,CFG_WIFI_PASS
DOOR_PASS='123456';DOOR_OPEN_MS=5000;IR_TIMEOUT_MS=15000;MAX_FAIL=3;BOOT_GRACE_MS=8000
CFG={'Thigh':30.0,'Tlow':27.0,'Lhigh':55,'Llow':35,'Tsleep_high':32.0,'Tsleep_low':26.0,'Taway_high':33.0,'Tcritical':35.0,'auto_fan_speed':70,'sleep_fan_speed':30,'away_fan_speed':60}
KEY={'runtime_key':'yolobit-01','fan_key':'ohstem-fan-ctrl-01','light_key':'ohstem-light-ctrl-01','temp_key':'ohstem-temp-01','humidity_key':'ohstem-humidity-01','light_sensor_key':'ohstem-light-01','motion_key':'ohstem-motion-01','runtime_id':None,'fan_id':None,'light_id':None}
S={'boot_ms':0,'mode':'away','prev_mode':None,'hold_until':None,'fan_status':'off','fan_speed':0,'light_status':'off','local_fan_status':'off','local_fan_speed':0,'local_light_status':'off','sensor_error':0,'state_error':0,'config_error':0,'telemetry_error':0,'command_error':0,'registry_error':0,'yolo_error':0,'alert_active':0,'security_alert_active':0,'nhiet_do':None,'do_am':None,'shine':None,'pir_motion':0,'camera_human_detected':0,'camera_human_count':0,'camera_confidence':0.0,'camera_motion_detected':0,'camera_motion_score':0.0,'someone':0,'last_human_seen_ms':0,'door_locked':1,'door_open':0,'door_open_until':None,'ir_typing':0,'ir_pass':'','ir_last_input_ms':0,'failed_attempts':0,'last_ir_code':None,'last_ir_ms':0,'ir_pending':None,'last_security_alert_ms':0,'last_gc':0,'server_ready':0,'net_block_until':0,'consecutive_http_fail':0,'last_cmd_apply_ms':0,'last_l1':'','last_l2':'','last_fan_hw':None,'last_light_hw':None,'last_door_hw':None}
IR_DIGIT_MAP={IR_REMOTE_0:'0',IR_REMOTE_1:'1',IR_REMOTE_2:'2',IR_REMOTE_3:'3',IR_REMOTE_4:'4',IR_REMOTE_5:'5',IR_REMOTE_6:'6',IR_REMOTE_7:'7',IR_REMOTE_8:'8',IR_REMOTE_9:'9'};RAW_IR_DIGIT_MAP={22:'0',12:'1',24:'2',94:'3',8:'4',28:'5',90:'6',66:'7',82:'8',74:'9'};RAW_IR_KEY_SETUP=21;RAW_IR_KEY_OK=13;IR_KEY_SETUP=IR_REMOTE_SETUP;IR_KEY_OK=IR_REMOTE_F;IR_DEBOUNCE_MS=180

# Network intervals. Keep command/state moderate because YoloBit is single-threaded.
ITV={
    'registry':120000,
    'state':15000,
    'config':60000,
    'telemetry':4000,
    'command':500,
    'yolo':10000,
    'status':2500,
    'sensor':1000,
    'fast_sensor':100,
    'display':150,
    'led':100,
    'ack':1000,
    'alert':2000
}
LAST={k:0 for k in ITV}
TEL=(('temp_key','temperature','nhiet_do'),('humidity_key','humidity','do_am'),('light_sensor_key','light','shine'),('motion_key','motion','someone'))
tel_i=0;temp_alert_last=0;PENDING_ALERT=None;ACKQ=[]
PERF={'loop_sum':0,'loop_n':0,'loop_max':0,'http_ms':0,'http_max':0,'http_path':'','cmd_apply_ms':0,'ack_ms':0,'tel_delay':0,'last':0,'mem':0}

def j_text(txt):
    try:return ujson.loads(txt)
    except:return None
def close_resp(r):
    try:r and r.close()
    except:pass
def ok(r):
    try:return r and 200<=r.status_code<300
    except:return False
def unwrap(x):return x.get('data') if isinstance(x,dict) and 'data' in x else x
def clamp(v,a,b):return a if v<a else b if v>b else v
def to_int(v,d=0):
    try:return int(v)
    except:
        try:return int(float(v))
        except:return d
def to_float(v,d=0.0):
    try:return float(v)
    except:return d
def norm(v):return '' if v is None else str(v).strip().lower()
def fit16(s):s=str(s);return s[:16] if len(s)>16 else s+' '*(16-len(s))
def ms():return time.ticks_ms()
def in_boot_grace():return time.ticks_diff(ms(),S['boot_ms'])<BOOT_GRACE_MS
def state_unavailable():return 0 if in_boot_grace() else 1 if(S['state_error'] or S['registry_error']) else 0
def server_error():return 0 if in_boot_grace() else 1 if(S['state_error'] or S['config_error'] or S['command_error'] or S['registry_error']) else 0

def net_allowed():
    return time.ticks_diff(ms(),S['net_block_until'])>=0

def mark_net_fail(wait=0):
    S['consecutive_http_fail']+=1
    if wait:
        S['net_block_until']=time.ticks_add(ms(),wait)
    elif S['consecutive_http_fail']>=4:
        # Backoff so local sensor/control still has CPU time when gateway/backend is unstable.
        delay=1000*S['consecutive_http_fail']
        if delay>15000:delay=15000
        S['net_block_until']=time.ticks_add(ms(),delay)

def mark_net_ok():
    S['consecutive_http_fail']=0
    S['net_block_until']=0

def perf_http(path,d):
    if PERF_LOG:
        PERF['http_ms']=d;PERF['http_path']=path
        if d>PERF['http_max']:PERF['http_max']=d

def req(method,path,data=None):
    r=None;t0=ms() if PERF_LOG else 0
    try:
        url=BASE+path
        if DEBUG:print('HTTP',method,url)

        if method=='GET':
            r=urequests.get(url,headers=HEAD)
        else:
            r=urequests.post(url,json=data,headers=HEAD)

        status=r.status_code
        body=''
        if method=='GET':
            try:
                body=r.text
                if body and len(body)>1200:
                    body=body[:1200]
            except Exception as e:
                if DEBUG:print('HTTP body read err',path,e)

        if DEBUG:
            print('HTTP status',status,path)

        if 200<=status<300:
            mark_net_ok()
            if method=='GET':
                x=j_text(body) if body else None
                return unwrap(x)
            return True

        if DEBUG:
            print('HTTP bad',status,path,body[:80] if body else '')
        mark_net_fail()

    except Exception as e:
        if DEBUG:print('HTTP err',method,path,e)
        mark_net_fail()

    finally:
        if PERF_LOG:perf_http(path,time.ticks_diff(ms(),t0))
        close_resp(r)
        try:gc.collect()
        except:pass

    return None if method=='GET' else False

GET=lambda p:req('GET',p)
POST=lambda p,d:req('POST',p,d)

def norm_switch(v,d='off'):
    s=norm(v)
    if s in('on','1','true','yes'):return 'on'
    if s in('off','0','false','no'):return 'off'
    try:return 'on' if int(v) else 'off'
    except:return d

def active_fan_status():return S['local_fan_status'] if state_unavailable() else S['fan_status']
def active_fan_speed():return S['local_fan_speed'] if state_unavailable() else S['fan_speed']
def active_light_status():return S['local_light_status'] if state_unavailable() else S['light_status']

def fan_hw(on,speed):
    try:pin0.write_digital(1 if on else 0)
    except:pass
def door_hw(op):
    try:pin4.servo_write(180 if op else 0)
    except:pass
def open_door():S['door_open']=1;S['door_locked']=0;S['door_open_until']=time.ticks_add(ms(),DOOR_OPEN_MS)
def close_door():S['door_open']=0;S['door_locked']=1;S['door_open_until']=None
def mode_short():return {'auto':'A','manual':'M','sleep':'S','away':'W'}.get(S['mode'],'?')

def update_lcd():
    t='--' if S['nhiet_do'] is None else str(round(S['nhiet_do'],1));h='--' if S['do_am'] is None else str(round(S['do_am'],1));l='--' if S['shine'] is None else str(S['shine']);l1=fit16('T:%s H:%s'%(t,h))
    if S['ir_typing']:l2=fit16('PASS:'+'*'*len(S['ir_pass']))
    elif S['sensor_error'] and server_error():l2=fit16('L:%s E:SEN+SRV'%l)
    elif S['sensor_error']:l2=fit16('L:%s E:SENSOR'%l)
    elif server_error():l2=fit16('FB %s P%s D%s'%(mode_short(),1 if S['someone'] else 0,1 if S['door_open'] else 0))
    else:l2=fit16('L:%s %s P%s D%s'%(l,mode_short(),1 if S['someone'] else 0,1 if S['door_open'] else 0))
    try:
        if l1!=S['last_l1']:lcd.move_to(0,0);lcd.putstr(l1);S['last_l1']=l1
        if l2!=S['last_l2']:lcd.move_to(0,1);lcd.putstr(l2);S['last_l2']=l2
    except:pass

def update_leds():
    try:
        blink=((ms()//250)%2)==0;a=S['alert_active'] or S['security_alert_active'];srv=server_error()
        rgb.show(1,(5,0,0) if S['sensor_error'] and blink else (0,0,0));rgb.show(2,(5,0,0) if srv and blink else (0,0,0));rgb.show(3,(5,5,0) if (S['sensor_error'] or srv) and blink else ((0,5,0) if(active_light_status()=='on' or S['door_open']) else (0,0,0)));rgb.show(4,(5,0,0) if a and blink else (0,0,0))
    except:pass

def load_registry():
    ds=GET('/gw/devices/home/%s'%HOME)
    if not isinstance(ds,list) or not ds:S['registry_error']=1;return
    KEY['runtime_id']=KEY['fan_id']=KEY['light_id']=None
    for d in ds:
        try:
            k=norm(d.get('deviceKey'))
            if k==KEY['runtime_key']:KEY['runtime_id']=d.get('id')
            elif k==KEY['fan_key']:KEY['fan_id']=d.get('id')
            elif k==KEY['light_key']:KEY['light_id']=d.get('id')
        except:pass
    S['registry_error']=0 if(KEY['runtime_id'] and KEY['fan_id'] and KEY['light_id']) else 1

CMD_SYNC_GRACE_MS=8000

def state_from_map(d,k):
    if not isinstance(d,dict):return None
    v=d.get(k)
    if isinstance(v,dict):return v
    return None

def fetch_all_states():
    skip=time.ticks_diff(ms(),S['last_cmd_apply_ms'])<CMD_SYNC_GRACE_MS;okc=0
    if not(KEY['runtime_id'] and KEY['fan_id'] and KEY['light_id']):
        S['state_error']=1;return

    d=GET('/gw/devices/states?ids=%s,%s,%s'%(KEY['runtime_id'],KEY['fan_id'],KEY['light_id']))
    if not isinstance(d,dict):
        S['state_error']=1;return

    a=state_from_map(d,str(KEY['runtime_id']))
    b=state_from_map(d,str(KEY['fan_id']))
    c=state_from_map(d,str(KEY['light_id']))

    if isinstance(a,dict):
        if(not skip) and a.get('mode') is not None:
            mv=norm(a.get('mode'))
            if mv in('auto','manual','sleep','away'):S['mode']=mv
        S['hold_until']=a.get('holdUntil');S['prev_mode']=a.get('prevMode');okc+=1
    if isinstance(b,dict):
        if not skip:
            if b.get('fanStatus') is not None:S['fan_status']=norm_switch(b.get('fanStatus'),S['fan_status'])
            if b.get('fanSpeed') is not None:S['fan_speed']=clamp(to_int(b.get('fanSpeed'),S['fan_speed']),0,100)
        okc+=1
    if isinstance(c,dict):
        if not skip and c.get('lightStatus') is not None:S['light_status']=norm_switch(c.get('lightStatus'),S['light_status'])
        okc+=1
    S['state_error']=0 if okc==3 else 1

def fetch_config():
    d=GET('/gw/homes/%s/configs'%HOME)
    if d is None:S['config_error']=1;return
    if isinstance(d,dict) and 'configs' in d and isinstance(d.get('configs'),dict):d=d.get('configs')
    if isinstance(d,list):
        try:
            m={}
            for i in d:
                if isinstance(i,dict) and i.get('key') is not None:m[str(i.get('key'))]=i.get('value')
            d=m
        except:S['config_error']=1;return
    if not isinstance(d,dict):S['config_error']=1;return
    mp={'thigh':('Thigh',to_float),'tlow':('Tlow',to_float),'lhigh':('Lhigh',to_int),'llow':('Llow',to_int),'tsleepHigh':('Tsleep_high',to_float),'tsleepLow':('Tsleep_low',to_float),'tawayHigh':('Taway_high',to_float),'tcritical':('Tcritical',to_float),'autoFanSpeed':('auto_fan_speed',to_int),'sleepFanSpeed':('sleep_fan_speed',to_int),'awayFanSpeed':('away_fan_speed',to_int)}
    for k in mp:
        if k in d and d[k] is not None:
            n,c=mp[k];v=c(d[k],CFG[n]);CFG[n]=clamp(v,0,100) if 'speed' in n.lower() else v
    S['config_error']=0

def queue_alert(tp,msg):
    global PENDING_ALERT
    if KEY['runtime_id'] and PENDING_ALERT is None:PENDING_ALERT=(tp,msg)
def send_pending_alert():
    global PENDING_ALERT
    if PENDING_ALERT is None:return
    tp,msg=PENDING_ALERT
    if POST('/gw/homes/%s/alerts'%HOME,{'deviceId':KEY['runtime_id'],'sensorId':None,'type':tp,'message':msg}):PENDING_ALERT=None
def send_alert(tp,msg):queue_alert(tp,msg)
def trigger_security_alert(tp,msg):S['security_alert_active']=1;S['last_security_alert_ms']=ms();send_alert(tp,msg)

def read_fast_sensor():
    try:S['shine']=clamp(round(translate(pin2.read_analog(),0,4095,0,100)),0,100)
    except:S['shine']=None
    try:S['pir_motion']=1 if pin16.read_digital() else 0
    except:S['pir_motion']=0
def read_sensor():
    try:dht.read_dht20();S['nhiet_do']=dht.dht20_temperature();S['do_am']=dht.dht20_humidity()
    except:S['nhiet_do']=None;S['do_am']=None
    read_fast_sensor()
def sensor_valid():
    t,h,l=S['nhiet_do'],S['do_am'],S['shine']
    return not(t is None or h is None or l is None or t<-10 or t>80 or h<0 or h>100 or l<0 or l>100)

def update_yolo():
    if not S['server_ready']:S['yolo_error']=1;return
    d=GET('/gw/yolo/check_human')
    if not isinstance(d,dict) or d.get('status')!='success':
        S['yolo_error']=1;S['camera_human_detected']=0;S['camera_human_count']=0;S['camera_motion_detected']=0;S['camera_confidence']=0.0;S['camera_motion_score']=0.0;return
    S['yolo_error']=0;S['camera_human_detected']=1 if d.get('human_detected',0) else 0;S['camera_human_count']=to_int(d.get('human_count',0),0);S['camera_confidence']=to_float(d.get('max_confidence',0.0),0.0);S['camera_motion_detected']=1 if d.get('motion_detected',0) else 0;S['camera_motion_score']=to_float(d.get('movement_score',0.0),0.0)

def combine_motion():
    a=S['camera_motion_detected'] or (S['pir_motion'] and S['camera_human_detected'] and S['camera_confidence']>=0.5)
    if a:S['someone']=1;S['last_human_seen_ms']=ms()
    elif time.ticks_diff(ms(),S['last_human_seen_ms'])>1800:S['someone']=0

def ir_cancel():S['ir_typing']=0;S['ir_pass']='';S['ir_last_input_ms']=0
def ir_timeout_check():
    if S['ir_typing'] and S['ir_last_input_ms'] and time.ticks_diff(ms(),S['ir_last_input_ms'])>=IR_TIMEOUT_MS:ir_cancel()
def ir_ok():S['ir_pass']='';S['ir_typing']=0;S['ir_last_input_ms']=0;S['failed_attempts']=0;S['security_alert_active']=0;open_door()
def ir_fail():
    S['failed_attempts']+=1;S['ir_pass']='';S['ir_typing']=0;S['ir_last_input_ms']=0
    if S['failed_attempts']>=MAX_FAIL:trigger_security_alert('WRONG_PASSWORD','Nhap sai mat khau %s lan'%S['failed_attempts'])
def ir_append(ch):
    if S['ir_typing'] and len(S['ir_pass'])<len(DOOR_PASS):S['ir_pass']+=str(ch);S['ir_last_input_ms']=ms()
def get_ir_digit(code):
    if code in IR_DIGIT_MAP:return IR_DIGIT_MAP[code]
    if code in RAW_IR_DIGIT_MAP:return RAW_IR_DIGIT_MAP[code]
    return None
def is_ir_setup(code):return code==IR_KEY_SETUP or code==RAW_IR_KEY_SETUP
def is_ir_ok(code):return code==IR_KEY_OK or code==RAW_IR_KEY_OK
def normalize_ir_code(token,addr,ext):
    for c in(token,ext):
        if c is None:continue
        if c in IR_DIGIT_MAP or c in RAW_IR_DIGIT_MAP or c in(IR_KEY_SETUP,RAW_IR_KEY_SETUP,IR_KEY_OK,RAW_IR_KEY_OK):return c
        try:
            ic=int(c)
            if ic in IR_DIGIT_MAP or ic in RAW_IR_DIGIT_MAP or ic in(IR_KEY_SETUP,RAW_IR_KEY_SETUP,IR_KEY_OK,RAW_IR_KEY_OK):return ic
        except:pass
    return None
def on_ir_received(token,addr,ext):
    code=normalize_ir_code(token,addr,ext)
    if code is not None:S['ir_pending']=code
def process_ir():
    code=S['ir_pending']
    if code is None:return
    S['ir_pending']=None;now=ms()
    if S['last_ir_code']==code and time.ticks_diff(now,S['last_ir_ms'])<IR_DEBOUNCE_MS:return
    S['last_ir_code']=code;S['last_ir_ms']=now
    if is_ir_setup(code):S['ir_typing']=1;S['ir_pass']='';S['ir_last_input_ms']=now;return
    if not S['ir_typing']:return
    if is_ir_ok(code):ir_ok() if S['ir_pass']==DOOR_PASS else ir_fail();return
    d=get_ir_digit(code)
    if d is not None:ir_append(d)

def compute_local_fallback():
    fs,sp,ls='off',0,'off'
    if not sensor_valid():
        S['sensor_error']=1;S['local_fan_status']='off';S['local_fan_speed']=0;S['local_light_status']='off';return
    S['sensor_error']=0;t=S['nhiet_do'];l=S['shine'];m=S['mode']
    if m=='sleep':
        if t is not None and t>=CFG['Tsleep_high']:fs,sp='on',CFG['sleep_fan_speed']
        elif t is not None and t<=CFG['Tsleep_low']:fs,sp='off',0
    elif m=='away':
        if t is not None and t>=CFG['Taway_high']:fs,sp='on',CFG['away_fan_speed']
    elif m=='auto':
        if t is not None and t>=CFG['Thigh']:fs,sp='on',CFG['auto_fan_speed']
        elif t is not None and t<=CFG['Tlow']:fs,sp='off',0
        if l is not None and l<=CFG['Llow']:ls='on'
        elif l is not None and l>=CFG['Lhigh']:ls='off'
    elif m=='manual':
        fs,sp,ls=S['fan_status'],S['fan_speed'],S['light_status']
    S['local_fan_status']=fs;S['local_fan_speed']=clamp(to_int(sp,0),0,100);S['local_light_status']=ls

def apply_logic():
    compute_local_fallback();fs=active_fan_status();sp=active_fan_speed();ls=active_light_status()
    if fs not in('on','off'):fs='off'
    if ls not in('on','off'):ls='off'
    sp=clamp(to_int(sp,0),0,100)
    if fs=='off':sp=0
    elif sp<=0:sp=CFG['sleep_fan_speed'] if S['mode']=='sleep' else CFG['away_fan_speed'] if S['mode']=='away' else CFG['auto_fan_speed']
    fh=(fs,sp)
    if fh!=S['last_fan_hw']:fan_hw(fs=='on',sp);S['last_fan_hw']=fh
    if ls!=S['last_light_hw']:
        try:pin8.write_digital(1 if ls=='on' else 0)
        except:
            try:pin1.write_digital(1 if ls=='on' else 0)
            except:pass
        S['last_light_hw']=ls
    S['alert_active']=1 if((S['nhiet_do'] is not None and S['nhiet_do']>CFG['Tcritical']) or (S['mode']=='away' and S['someone']) or S['security_alert_active']) else 0

def send_one_telemetry():
    global tel_i
    for _ in range(len(TEL)):
        kn,tp,sn=TEL[tel_i];tel_i=(tel_i+1)%len(TEL);dk=KEY.get(kn);v=1 if sn=='someone' and S[sn] else S.get(sn)
        if dk is None or v is None:continue
        S['telemetry_error']=0 if POST('/gw/device-telemetry',{'deviceKey':dk,'sensorType':tp,'value':v}) else 1
        return
    S['telemetry_error']=1

def normalize_target(x):return {'mode':'mode','power':'power','fan':'fan','fanstatus':'fan','fan_status':'fan','fanspeed':'fan_speed','fan_speed':'fan_speed','speed':'fan_speed','light':'light','lightstatus':'light','light_status':'light','brightness':'brightness','lightlevel':'brightness','light_level':'brightness'}.get(norm(x).replace('-','_'),norm(x).replace('-','_'))
def mark_cmd_applied():S['last_cmd_apply_ms']=ms()

def queue_ack(device_key,cid):
    if not(device_key and cid):return
    for k,i in ACKQ:
        if k==device_key and i==cid:return
    if len(ACKQ)>=6:ACKQ.pop(0)
    ACKQ.append((device_key,cid))

def flush_acks():
    if not ACKQ:return
    t0=ms() if PERF_LOG else 0
    if POST('/gw/commands/ack',{'acks':[{'deviceKey':k,'id':i} for k,i in ACKQ]}):
        ACKQ[:] = []
        if PERF_LOG:PERF['ack_ms']=time.ticks_diff(ms(),t0)

def ack(device_key,cid):queue_ack(device_key,cid);return True

def cmd_runtime(c):
    t0=ms() if PERF_LOG else 0
    cid=c.get('id')
    if cid is None:return
    if normalize_target(c.get('target'))=='mode' and c.get('value') is not None:
        m=norm(c.get('value'))
        if m in('auto','manual','sleep','away'):
            S['prev_mode']=S['mode'];S['mode']=m;mark_cmd_applied();apply_logic()
            if PERF_LOG:PERF['cmd_apply_ms']=time.ticks_diff(ms(),t0)
    ack(KEY['runtime_key'],cid)

def cmd_fan(c):
    t0=ms() if PERF_LOG else 0
    cid=c.get('id')
    if cid is None:return
    t,v=normalize_target(c.get('target')),c.get('value');a=0
    if t in('fan','power'):
        if v is not None:S['fan_status']=norm_switch(v,S['fan_status']);S['fan_speed']=0 if S['fan_status']=='off' else S['fan_speed'];mark_cmd_applied();a=1
    elif t in('fan_speed','speed'):
        sp=clamp(to_int(v,S['fan_speed']),0,100);S['fan_speed']=sp;S['fan_status']='on' if sp>0 else 'off';mark_cmd_applied();a=1
    if a:
        apply_logic()
        if PERF_LOG:PERF['cmd_apply_ms']=time.ticks_diff(ms(),t0)
    ack(KEY['fan_key'],cid)

def cmd_light(c):
    t0=ms() if PERF_LOG else 0
    cid=c.get('id')
    if cid is None:return
    t,v=normalize_target(c.get('target')),c.get('value');a=0
    if t in('light','power'):
        if v is not None:S['light_status']=norm_switch(v,S['light_status']);mark_cmd_applied();a=1
    elif t=='brightness':
        lv=clamp(to_int(v,0),0,100);S['light_status']='on' if lv>0 else 'off';mark_cmd_applied();a=1
    if a:
        apply_logic()
        if PERF_LOG:PERF['cmd_apply_ms']=time.ticks_diff(ms(),t0)
    ack(KEY['light_key'],cid)

def fetch_all_cmds():
    had=0
    d=GET('/gw/commands/next?keys=%s,%s,%s'%(KEY['runtime_key'],KEY['fan_key'],KEY['light_key']))
    if not isinstance(d,dict):
        S['command_error']=1
        return
    for k,f in((KEY['runtime_key'],cmd_runtime),(KEY['fan_key'],cmd_fan),(KEY['light_key'],cmd_light)):
        c=d.get(k)
        if c is None:continue
        try:f(c)
        except Exception as e:
            had=1
            if DEBUG:print('cmd apply err',k,e)
    S['command_error']=had

def print_status():
    if not STATUS_LOG:return
    print('mode=',S['mode'],'T=',S['nhiet_do'],'H=',S['do_am'],'L=',S['shine'],'someone=',S['someone'],'fan=',active_fan_status(),active_fan_speed(),'light=',active_light_status(),'door=',S['door_open'],'err=',S['state_error'],S['config_error'],S['command_error'],S['registry_error'],'http=',S['consecutive_http_fail'])

def perf_tick(d):
    if not PERF_LOG:return
    PERF['loop_sum']+=d;PERF['loop_n']+=1
    if d>PERF['loop_max']:PERF['loop_max']=d
    now=ms()
    if time.ticks_diff(now,PERF['last'])>=3000:
        try:PERF['mem']=gc.mem_free()
        except:PERF['mem']=0
        avg=PERF['loop_sum']//PERF['loop_n'] if PERF['loop_n'] else 0
        print('PERF loop',avg,PERF['loop_max'],'http',PERF['http_ms'],PERF['http_max'],PERF['http_path'],'cmd',PERF['cmd_apply_ms'],'ack',PERF['ack_ms'],'mem',PERF['mem'])
        PERF['loop_sum']=0;PERF['loop_n']=0;PERF['loop_max']=0;PERF['http_max']=0;PERF['last']=now

def due(k):
    return time.ticks_diff(ms(),LAST[k])>=ITV[k]
def done(k):
    LAST[k]=ms()

def run_backend_task(now):
    if S['ir_typing']:return

    if ACKQ and due('ack'):
        flush_acks();done('ack');return

    if PENDING_ALERT is not None and due('alert'):
        send_pending_alert();done('alert');return

    # Fast control: gateway command route is now cache-based and should return quickly.
    if due('command'):
        fetch_all_cmds();done('command');return

    if sensor_valid() and due('telemetry'):
        send_one_telemetry();done('telemetry');return

    if due('state'):
        fetch_all_states();done('state');return

    if due('yolo'):
        if S['server_ready']:
            try:update_yolo()
            except:S['yolo_error']=1
        done('yolo');return

    if due('config'):
        fetch_config();done('config');return

    if due('registry'):
        load_registry();done('registry');return

ir=IR_RX(Pin(pin10.pin,Pin.IN));ir.start();ir.on_received(on_ir_received)

display.scroll('IoT')
try:print('GATEWAY BASE =',BASE)
except:pass
lcd.backlight_on();lcd.clear();lcd.move_to(0,0);lcd.putstr('Smart House');lcd.move_to(0,1);lcd.putstr('Booting...')
S['boot_ms']=ms();close_door();update_leds();update_lcd()

try:mqtt.connect_wifi(WIFI_SSID,WIFI_PASS)
except Exception as e:
    if DEBUG:print('wifi err',e)

time.sleep_ms(400)

try:load_registry()
except:S['registry_error']=1
try:fetch_config()
except:S['config_error']=1
for _ in range(1):
    try:fetch_all_states()
    except:S['state_error']=1
try:
    yh=GET('/gw/yolo/health')
    if yh is not None:S['server_ready']=1
except:pass

lcd.clear();lcd.move_to(0,0);lcd.putstr('Smart House');lcd.move_to(0,1);lcd.putstr('Running...');update_lcd()

while True:
    loop_start=ms();now=loop_start
    if time.ticks_diff(now,S['last_gc'])>12000:
        try:gc.collect();S['last_gc']=now
        except:pass

    if time.ticks_diff(now,LAST['fast_sensor'])>=ITV['fast_sensor']:
        LAST['fast_sensor']=now;read_fast_sensor()
    if time.ticks_diff(now,LAST['sensor'])>=ITV['sensor']:
        LAST['sensor']=now;read_sensor()

    process_ir();ir_timeout_check()

    if S['door_open'] and S['door_open_until'] is not None and time.ticks_diff(now,S['door_open_until'])>=0:close_door()
    if S['door_open']!=S['last_door_hw']:door_hw(S['door_open']);S['last_door_hw']=S['door_open']

    combine_motion()
    if S['security_alert_active'] and time.ticks_diff(now,S['last_security_alert_ms'])>=15000:S['security_alert_active']=0
    apply_logic()

    if time.ticks_diff(now,LAST['led'])>=ITV['led']:LAST['led']=now;update_leds()
    if time.ticks_diff(now,LAST['display'])>=ITV['display']:LAST['display']=now;update_lcd()

    if S['nhiet_do'] is not None and S['nhiet_do']>CFG['Tcritical'] and time.ticks_diff(now,temp_alert_last)>=15000:
        send_alert('HIGH_TEMPERATURE','Nhiet do cao: %s'%round(S['nhiet_do'],1));temp_alert_last=now

    if server_error():
        ITV['state'],ITV['command'],ITV['telemetry'],ITV['yolo']=20000,1500,6000,15000
    else:
        ITV['state'],ITV['command'],ITV['telemetry'],ITV['yolo']=15000,500,4000,10000

    if net_allowed():run_backend_task(now)

    if time.ticks_diff(now,LAST['status'])>=ITV['status']:print_status();LAST['status']=now
    perf_tick(time.ticks_diff(ms(),loop_start))
    time.sleep_ms(2 if S['ir_typing'] else 8)
