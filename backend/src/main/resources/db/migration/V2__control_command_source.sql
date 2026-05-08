ALTER TABLE public.control_commands
    ADD COLUMN IF NOT EXISTS source varchar(32) DEFAULT 'system';

UPDATE public.control_commands
SET source = CASE
    WHEN actor_id IS NOT NULL THEN 'manual'
    WHEN upper(coalesce(actor_name, '')) = 'SYSTEM' THEN 'system'
    ELSE 'manual'
END
WHERE source IS NULL OR source = '';
