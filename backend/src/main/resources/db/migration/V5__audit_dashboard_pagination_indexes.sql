CREATE INDEX IF NOT EXISTS idx_activity_logs_home_action_created_id_desc
    ON public.activity_logs (home_id, action, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_activity_logs_home_method_created_id_desc
    ON public.activity_logs (home_id, method, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_activity_logs_home_device_created_id_desc
    ON public.activity_logs (home_id, device_id, created_at DESC, id DESC);
