CREATE INDEX IF NOT EXISTS idx_activity_logs_home_created_id_desc
    ON public.activity_logs (home_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_activity_logs_created_id_desc
    ON public.activity_logs (created_at DESC, id DESC);

UPDATE public.configs cfg
SET monitoring_light_device_id = light_ctrl.id
FROM public.devices light_ctrl,
     public.devices light_sensor
WHERE light_ctrl.device_key = 'ohstem-light-ctrl-01'
  AND light_sensor.device_key = 'ohstem-light-01'
  AND cfg.home_id = light_ctrl.home_id
  AND cfg.home_id = light_sensor.home_id
  AND cfg.monitoring_light_device_id = light_sensor.id;
