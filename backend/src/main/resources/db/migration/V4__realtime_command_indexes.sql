CREATE INDEX IF NOT EXISTS idx_control_commands_deliverable
ON public.control_commands (device_id, created_at ASC)
WHERE status IN ('PENDING', 'SENT');

CREATE UNIQUE INDEX IF NOT EXISTS uq_devices_device_key
ON public.devices(device_key);

CREATE INDEX IF NOT EXISTS idx_sensors_device_kind
ON public.sensors(device_id, sensor_kind);

CREATE INDEX IF NOT EXISTS idx_runtime_state_device_capability
ON public.device_runtime_state(device_id, capability_code);
