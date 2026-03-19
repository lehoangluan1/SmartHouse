-- =========================================
-- PostgreSQL - Smart Home Schema
-- =========================================

-- =========================
-- ENUM TYPES
-- =========================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
    CREATE TYPE user_role AS ENUM ('ADMIN', 'LANDLORD', 'INSTALLER');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
    CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'LOCKED');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'entity_status') THEN
    CREATE TYPE entity_status AS ENUM ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'RETIRED');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'device_class') THEN
    CREATE TYPE device_class AS ENUM ('CONTROLLER', 'ACTUATOR', 'SENSOR_NODE', 'HUB', 'OTHER');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'system_mode') THEN
    CREATE TYPE system_mode AS ENUM ('auto', 'manual', 'sleep', 'away');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'command_status') THEN
    CREATE TYPE command_status AS ENUM ('PENDING', 'SENT', 'ACKED', 'FAILED', 'CANCELLED');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'alert_type') THEN
    CREATE TYPE alert_type AS ENUM (
      'CRITICAL_TEMP',
      'DEVICE_OFFLINE',
      'SENSOR_ERROR',
      'HIGH_HUMIDITY',
      'LOW_HUMIDITY',
      'LOW_LIGHT',
      'HIGH_LIGHT',
      'MOTION_DETECTED',
      'CUSTOM',
      'TEMP_TOO_HIGH',
      'TEMP_TOO_LOW',
      'WRONG_PASSWORD'
    );
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'alert_status') THEN
    CREATE TYPE alert_status AS ENUM ('ACTIVE', 'ACK', 'RESOLVED');
  END IF;
END $$;


-- =========================
-- USERS
-- =========================
CREATE TABLE IF NOT EXISTS users (
  id              BIGSERIAL PRIMARY KEY,
  username        VARCHAR(64) NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  role            user_role NOT NULL,
  status          user_status NOT NULL DEFAULT 'ACTIVE',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_login      TIMESTAMPTZ,

  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT ck_users_username_not_blank CHECK (btrim(username) <> ''),
  CONSTRAINT ck_users_password_hash_not_blank CHECK (btrim(password_hash) <> '')
);


-- =========================
-- HOMES
-- =========================
CREATE TABLE IF NOT EXISTS homes (
  id              BIGSERIAL PRIMARY KEY,
  name            VARCHAR(128) NOT NULL,
  address         VARCHAR(255),
  owner_user_id   BIGINT REFERENCES users(id) ON DELETE SET NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT ck_homes_name_not_blank CHECK (btrim(name) <> '')
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_homes_owner_name
  ON homes(owner_user_id, lower(name))
  WHERE owner_user_id IS NOT NULL;


-- =========================
-- HOME MEMBERS
-- =========================
CREATE TABLE IF NOT EXISTS home_users (
  home_id         BIGINT NOT NULL REFERENCES homes(id) ON DELETE CASCADE,
  user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_in_home    user_role NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (home_id, user_id)
);


-- =========================
-- DEVICES
-- class = nhóm lớn
-- subtype = loại cụ thể mở rộng mềm dẻo hơn enum cứng
-- =========================
CREATE TABLE IF NOT EXISTS devices (
  id               BIGSERIAL PRIMARY KEY,
  home_id          BIGINT NOT NULL REFERENCES homes(id) ON DELETE CASCADE,
  device_key       VARCHAR(64) NOT NULL,
  name             VARCHAR(128) NOT NULL,
  class            device_class NOT NULL,
  subtype          VARCHAR(64),
  room_name        VARCHAR(64),
  status           entity_status NOT NULL DEFAULT 'ACTIVE',
  last_seen        TIMESTAMPTZ,
  is_online        BOOLEAN NOT NULL DEFAULT FALSE,
  installed_by     BIGINT REFERENCES users(id) ON DELETE SET NULL,
  firmware_version VARCHAR(64),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_devices_device_key UNIQUE (device_key),
  CONSTRAINT uq_devices_home_name UNIQUE (home_id, name),
  CONSTRAINT ck_devices_device_key_not_blank CHECK (btrim(device_key) <> ''),
  CONSTRAINT ck_devices_name_not_blank CHECK (btrim(name) <> ''),
  CONSTRAINT ck_devices_subtype_format CHECK (
    subtype IS NULL OR subtype ~ '^[A-Z][A-Z0-9_]*$'
  )
);


-- =========================
-- DEVICE CAPABILITIES
-- Thiết bị hỗ trợ những gì:
-- POWER / SPEED / BRIGHTNESS / MODE / TARGET_TEMPERATURE...
-- =========================
CREATE TABLE IF NOT EXISTS device_capabilities (
  device_id        BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  capability_code  VARCHAR(64) NOT NULL,
  value_type       VARCHAR(16) NOT NULL, -- BOOLEAN / NUMBER / TEXT / MODE
  is_writable      BOOLEAN NOT NULL DEFAULT TRUE,
  min_value        DOUBLE PRECISION,
  max_value        DOUBLE PRECISION,
  unit             VARCHAR(32),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (device_id, capability_code),

  CONSTRAINT ck_device_capability_code_format
    CHECK (capability_code ~ '^[A-Z][A-Z0-9_]*$'),

  CONSTRAINT ck_device_capabilities_value_type
    CHECK (value_type IN ('BOOLEAN', 'NUMBER', 'TEXT', 'MODE')),

  CONSTRAINT ck_device_capabilities_range
    CHECK (
      (value_type <> 'NUMBER')
      OR (min_value IS NULL AND max_value IS NULL)
      OR (min_value IS NOT NULL AND max_value IS NOT NULL AND min_value <= max_value)
    ),

  CONSTRAINT ck_device_capabilities_number_only_range
    CHECK (
      value_type = 'NUMBER'
      OR (min_value IS NULL AND max_value IS NULL)
    )
);


-- =========================
-- DEVICE RUNTIME STATE
-- Trạng thái thực tế hiện tại của từng capability
-- Không hard-code fan/light như schema cũ
-- =========================
CREATE TABLE IF NOT EXISTS device_runtime_state (
  device_id        BIGINT NOT NULL,
  capability_code  VARCHAR(64) NOT NULL,
  value_boolean    BOOLEAN,
  value_number     DOUBLE PRECISION,
  value_text       VARCHAR(255),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (device_id, capability_code),

  CONSTRAINT fk_runtime_state_capability
    FOREIGN KEY (device_id, capability_code)
    REFERENCES device_capabilities(device_id, capability_code)
    ON DELETE CASCADE,

  CONSTRAINT ck_device_runtime_state_exactly_one_value
    CHECK (
      ((value_boolean IS NOT NULL)::int +
       (value_number  IS NOT NULL)::int +
       (value_text    IS NOT NULL)::int) = 1
    )
);


-- =========================
-- SENSORS
-- Mỗi sensor thuộc một device SENSOR_NODE
-- sensor_kind dùng text để dễ mở rộng
-- =========================
CREATE TABLE IF NOT EXISTS sensors (
  id              BIGSERIAL PRIMARY KEY,
  device_id       BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  name            VARCHAR(128) NOT NULL,
  sensor_kind     VARCHAR(64) NOT NULL,
  unit            VARCHAR(32),
  status          entity_status NOT NULL DEFAULT 'ACTIVE',
  last_seen       TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_sensors_device_kind UNIQUE (device_id, sensor_kind),
  CONSTRAINT ck_sensors_name_not_blank CHECK (btrim(name) <> ''),
  CONSTRAINT ck_sensors_kind_format CHECK (sensor_kind ~ '^[A-Z][A-Z0-9_]*$')
);


-- =========================
-- SENSOR DATA
-- Phải đúng chính xác 1 loại giá trị
-- =========================
CREATE TABLE IF NOT EXISTS sensor_data (
  id              BIGSERIAL PRIMARY KEY,
  sensor_id       BIGINT NOT NULL REFERENCES sensors(id) ON DELETE CASCADE,
  value_numeric   DOUBLE PRECISION,
  value_text      VARCHAR(255),
  value_boolean   BOOLEAN,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT ck_sensor_data_exactly_one_value
    CHECK (
      ((value_numeric IS NOT NULL)::int +
       (value_text IS NOT NULL)::int +
       (value_boolean IS NOT NULL)::int) = 1
    )
);


-- =========================
-- CONFIGS
-- =========================
CREATE TABLE IF NOT EXISTS configs (
  id                BIGSERIAL PRIMARY KEY,
  home_id           BIGINT NOT NULL REFERENCES homes(id) ON DELETE CASCADE,
  name              VARCHAR(128) NOT NULL,
  created_by        BIGINT REFERENCES users(id) ON DELETE SET NULL,

  thigh             DOUBLE PRECISION NOT NULL,
  tlow              DOUBLE PRECISION NOT NULL,

  lhigh             INTEGER NOT NULL,
  llow              INTEGER NOT NULL,

  tsleep_high       DOUBLE PRECISION NOT NULL,
  tsleep_low        DOUBLE PRECISION NOT NULL,

  taway_high        DOUBLE PRECISION NOT NULL,
  tcritical         DOUBLE PRECISION NOT NULL,

  n_minutes         INTEGER NOT NULL,
  m_minutes         INTEGER NOT NULL,
  thold_minutes     INTEGER NOT NULL,
  dpresent          INTEGER,
  k_minutes         INTEGER,

  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_configs_home_name UNIQUE (home_id, name),
  CONSTRAINT ck_configs_name_not_blank CHECK (btrim(name) <> ''),
  CONSTRAINT ck_configs_temp_hysteresis CHECK (thigh > tlow),
  CONSTRAINT ck_configs_sleep_hysteresis CHECK (tsleep_high > tsleep_low),
  CONSTRAINT ck_configs_light_hysteresis CHECK (lhigh > llow),
  CONSTRAINT ck_configs_minutes_pos CHECK (n_minutes > 0 AND m_minutes > 0 AND thold_minutes > 0),
  CONSTRAINT ck_configs_optional_minutes_pos CHECK (
    (dpresent IS NULL OR dpresent > 0)
    AND (k_minutes IS NULL OR k_minutes > 0)
  ),
  CONSTRAINT ck_configs_tcritical CHECK (tcritical >= thigh),
  CONSTRAINT ck_configs_taway_high CHECK (taway_high >= tlow)
);


-- =========================
-- DEVICE CONFIGS
-- Mỗi device đang dùng config nào
-- =========================
CREATE TABLE IF NOT EXISTS device_configs (
  device_id        BIGINT PRIMARY KEY REFERENCES devices(id) ON DELETE CASCADE,
  config_id        BIGINT NOT NULL REFERENCES configs(id) ON DELETE CASCADE,
  changed_by       BIGINT REFERENCES users(id) ON DELETE SET NULL,
  changed_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  reason           TEXT
);


-- =========================
-- SCHEDULES
-- Chỉ lưu lịch áp dụng giá trị
-- Không lưu runtime state snapshot
-- =========================
CREATE TABLE IF NOT EXISTS schedules (
  id                BIGSERIAL PRIMARY KEY,
  device_id         BIGINT NOT NULL,
  capability_code   VARCHAR(64) NOT NULL,

  value_boolean     BOOLEAN,
  value_number      DOUBLE PRECISION,
  value_text        VARCHAR(255),

  start_time        TIME NOT NULL,
  end_time          TIME,
  days_mask         INTEGER NOT NULL DEFAULT 127, -- 7 bit
  enabled           BOOLEAN NOT NULL DEFAULT TRUE,
  priority          INTEGER NOT NULL DEFAULT 0,

  created_by        BIGINT REFERENCES users(id) ON DELETE SET NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_schedules_capability
    FOREIGN KEY (device_id, capability_code)
    REFERENCES device_capabilities(device_id, capability_code)
    ON DELETE CASCADE,

  CONSTRAINT ck_schedules_days_mask
    CHECK (days_mask >= 1 AND days_mask <= 127),

  CONSTRAINT ck_schedules_exactly_one_value
    CHECK (
      ((value_boolean IS NOT NULL)::int +
       (value_number  IS NOT NULL)::int +
       (value_text    IS NOT NULL)::int) = 1
    ),

  CONSTRAINT ck_schedules_time_range
    CHECK (end_time IS NULL OR end_time <> start_time)
);


-- =========================
-- CONTROL COMMANDS
-- target = capability_code
-- value_* giống runtime/schedule để nhất quán kiểu dữ liệu
-- =========================
CREATE TABLE IF NOT EXISTS control_commands (
  id              BIGSERIAL PRIMARY KEY,
  device_id       BIGINT NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  target          VARCHAR(64) NOT NULL,
  value_boolean   BOOLEAN,
  value_number    DOUBLE PRECISION,
  value_text      VARCHAR(255),
  actor_id        BIGINT REFERENCES users(id) ON DELETE SET NULL,
  actor_name      VARCHAR(64),
  status          command_status NOT NULL DEFAULT 'PENDING',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  sent_at         TIMESTAMPTZ,
  ack_at          TIMESTAMPTZ,

  CONSTRAINT ck_control_commands_target_format
    CHECK (target ~ '^[A-Z][A-Z0-9_]*$'),

  CONSTRAINT ck_control_commands_exactly_one_value
    CHECK (
      ((value_boolean IS NOT NULL)::int +
       (value_number  IS NOT NULL)::int +
       (value_text    IS NOT NULL)::int) = 1
    ),

  CONSTRAINT ck_control_commands_time_order
    CHECK (
      sent_at IS NULL OR sent_at >= created_at
    )
);


-- =========================
-- ALERTS
-- =========================
CREATE TABLE IF NOT EXISTS alerts (
  id                BIGSERIAL PRIMARY KEY,
  home_id           BIGINT NOT NULL REFERENCES homes(id) ON DELETE CASCADE,
  device_id         BIGINT REFERENCES devices(id) ON DELETE CASCADE,
  sensor_id         BIGINT REFERENCES sensors(id) ON DELETE CASCADE,
  type              alert_type NOT NULL,
  message           TEXT,
  status            alert_status NOT NULL DEFAULT 'ACTIVE',
  acknowledged_by   BIGINT REFERENCES users(id) ON DELETE SET NULL,
  acknowledged_at   TIMESTAMPTZ,
  resolved_by       BIGINT REFERENCES users(id) ON DELETE SET NULL,
  resolved_at       TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_triggered_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT ck_alerts_ack_pair
    CHECK (
      (acknowledged_by IS NULL AND acknowledged_at IS NULL)
      OR (acknowledged_by IS NOT NULL AND acknowledged_at IS NOT NULL)
    ),

  CONSTRAINT ck_alerts_resolved_pair
    CHECK (
      (resolved_by IS NULL AND resolved_at IS NULL)
      OR (resolved_by IS NOT NULL AND resolved_at IS NOT NULL)
    )
);


-- =========================
-- ACTIVITY LOGS
-- =========================
CREATE TABLE IF NOT EXISTS activity_logs (
  id              BIGSERIAL PRIMARY KEY,
  home_id         BIGINT REFERENCES homes(id) ON DELETE SET NULL,
  device_id       BIGINT REFERENCES devices(id) ON DELETE SET NULL,
  user_id         BIGINT REFERENCES users(id) ON DELETE SET NULL,
  action          VARCHAR(64) NOT NULL,
  method          VARCHAR(32),
  old_value       JSONB,
  new_value       JSONB,
  detail          JSONB,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT ck_activity_logs_action_not_blank CHECK (btrim(action) <> '')
);

CREATE TABLE IF NOT EXISTS device_state_history (
  id               BIGSERIAL PRIMARY KEY,
  device_id        BIGINT NOT NULL,
  capability_code  VARCHAR(64) NOT NULL,

  value_boolean    BOOLEAN,
  value_number     DOUBLE PRECISION,
  value_text       VARCHAR(255),

  source           VARCHAR(32) NOT NULL, -- COMMAND / DEVICE_ACK / AUTOMATION / SCHEDULE / INIT / SYSTEM
  source_ref_id    BIGINT,               -- id của command / activity / schedule nếu có
  changed_by       BIGINT REFERENCES users(id) ON DELETE SET NULL,

  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_device_state_history_capability
    FOREIGN KEY (device_id, capability_code)
    REFERENCES device_capabilities(device_id, capability_code)
    ON DELETE CASCADE,

  CONSTRAINT ck_device_state_history_exactly_one_value
    CHECK (
      ((value_boolean IS NOT NULL)::int +
       (value_number  IS NOT NULL)::int +
       (value_text    IS NOT NULL)::int) = 1
    ),

  CONSTRAINT ck_device_state_history_source_not_blank
    CHECK (btrim(source) <> '')
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON e.enumtypid = t.oid
        WHERE t.typname = 'user_status'
          AND e.enumlabel = 'INVITED'
    ) THEN
        ALTER TYPE user_status ADD VALUE 'INVITED';
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON e.enumtypid = t.oid
        WHERE t.typname = 'user_status'
          AND e.enumlabel = 'INACTIVE'
    ) THEN
        ALTER TYPE user_status ADD VALUE 'INACTIVE';
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON e.enumtypid = t.oid
        WHERE t.typname = 'user_status'
          AND e.enumlabel = 'LOCKED'
    ) THEN
        ALTER TYPE user_status ADD VALUE 'LOCKED';
    END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'auth_provider') THEN
    CREATE TYPE auth_provider AS ENUM ('LOCAL', 'GOOGLE');
  END IF;
END $$;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS invited_at TIMESTAMPTZ NULL;

ALTER TABLE users
    ADD CONSTRAINT chk_users_must_change_password_not_null
    CHECK (must_change_password IN (TRUE, FALSE));

ALTER TABLE users
  ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
  DROP CONSTRAINT IF EXISTS ck_users_password_hash_not_blank;

ALTER TABLE users
  ADD CONSTRAINT ck_users_password_hash_blank_safe
  CHECK (password_hash IS NULL OR btrim(password_hash) <> '');

CREATE TABLE IF NOT EXISTS user_auth_providers (
  id                BIGSERIAL PRIMARY KEY,
  user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider          auth_provider NOT NULL,
  provider_user_id  VARCHAR(191),
  provider_email    VARCHAR(191),
  linked_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_used_at      TIMESTAMPTZ,

  CONSTRAINT uq_user_auth_provider_user_provider
    UNIQUE (user_id, provider),

  CONSTRAINT uq_user_auth_provider_provider_user_id
    UNIQUE (provider, provider_user_id),

  CONSTRAINT ck_user_auth_provider_provider_user_id_not_blank
    CHECK (provider_user_id IS NULL OR btrim(provider_user_id) <> ''),

  CONSTRAINT ck_user_auth_provider_provider_email_not_blank
    CHECK (provider_email IS NULL OR btrim(provider_email) <> '')
);
CREATE INDEX IF NOT EXISTS idx_user_auth_providers_user
  ON user_auth_providers(user_id);

CREATE INDEX IF NOT EXISTS idx_user_auth_providers_provider_email
  ON user_auth_providers(provider, provider_email);
CREATE TABLE IF NOT EXISTS user_refresh_tokens (
  id              BIGSERIAL PRIMARY KEY,
  user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash      VARCHAR(64) NOT NULL,
  issued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at      TIMESTAMPTZ NOT NULL,
  revoked_at      TIMESTAMPTZ,
  created_by_ip   VARCHAR(64),
  user_agent      VARCHAR(500),

  CONSTRAINT uq_user_refresh_tokens_token_hash UNIQUE (token_hash),

  CONSTRAINT ck_user_refresh_tokens_token_hash_not_blank
    CHECK (btrim(token_hash) <> ''),

  CONSTRAINT ck_user_refresh_tokens_expiry_valid
    CHECK (expires_at > issued_at),

  CONSTRAINT ck_user_refresh_tokens_revoke_time_valid
    CHECK (revoked_at IS NULL OR revoked_at >= issued_at)
);
CREATE INDEX IF NOT EXISTS idx_user_refresh_tokens_user
  ON user_refresh_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_user_refresh_tokens_expires_at
  ON user_refresh_tokens(expires_at);

CREATE INDEX IF NOT EXISTS idx_user_refresh_tokens_user_revoked
  ON user_refresh_tokens(user_id, revoked_at);
  
INSERT INTO user_auth_providers (
  user_id,
  provider,
  provider_user_id,
  provider_email,
  linked_at
)
SELECT
  u.id,
  'LOCAL'::auth_provider,
  u.username,
  u.username,
  COALESCE(u.created_at, now())
FROM users u
WHERE u.password_hash IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM user_auth_providers p
    WHERE p.user_id = u.id
      AND p.provider = 'LOCAL'::auth_provider
  );


create table if not exists outbox_event (
    id bigserial primary key,
    aggregate_type varchar(100) not null,
    aggregate_id bigint not null,
    event_type varchar(100) not null,
    payload text not null,
    status varchar(20) not null,
    retry_count integer not null default 0,
    last_error text,
    created_at timestamptz not null,
    published_at timestamptz
);

-- =========================
-- INDEXES
-- =========================
CREATE INDEX IF NOT EXISTS idx_devices_home
  ON devices(home_id);

CREATE INDEX IF NOT EXISTS idx_devices_home_room
  ON devices(home_id, room_name);

CREATE INDEX IF NOT EXISTS idx_devices_class
  ON devices(class);

CREATE INDEX IF NOT EXISTS idx_sensors_device
  ON sensors(device_id);

CREATE INDEX IF NOT EXISTS idx_sensors_device_kind
  ON sensors(device_id, sensor_kind);

CREATE INDEX IF NOT EXISTS idx_sensor_data_sensor_time
  ON sensor_data(sensor_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_device_runtime_state_device
  ON device_runtime_state(device_id);

CREATE INDEX IF NOT EXISTS idx_schedules_device_enabled
  ON schedules(device_id, enabled, start_time);

CREATE INDEX IF NOT EXISTS idx_alerts_home_status
  ON alerts(home_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_alerts_device_status
  ON alerts(device_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_commands_device_status
  ON control_commands(device_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_logs_home_time
  ON activity_logs(home_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_logs_device_time
  ON activity_logs(device_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_device_state_history_device_capability_created_at
  ON device_state_history(device_id, capability_code, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_device_state_history_source_ref
  ON device_state_history(source, source_ref_id);

create index if not exists idx_outbox_event_status_created_at
    on outbox_event(status, created_at);

-- =========================================
-- 1) CREATE system_user_role
-- users.role: user_role -> system_user_role
-- =========================================
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type
    WHERE typname = 'system_user_role'
  ) THEN
    CREATE TYPE system_user_role AS ENUM (
      'ADMIN',
      'CUSTOMER',
      'INSTALLER',
      'SUPER_ADMIN'
    );
  END IF;
END $$;

ALTER TABLE users
  ALTER COLUMN role TYPE system_user_role
  USING (
    CASE role::text
      WHEN 'ADMIN' THEN 'ADMIN'::system_user_role
      WHEN 'LANDLORD' THEN 'CUSTOMER'::system_user_role
      WHEN 'INSTALLER' THEN 'INSTALLER'::system_user_role
      ELSE 'CUSTOMER'::system_user_role
    END
  );


-- =========================================
-- 2) CREATE home_user_role
-- home_users.role_in_home: user_role -> home_user_role
-- =========================================
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type
    WHERE typname = 'home_user_role'
  ) THEN
    CREATE TYPE home_user_role AS ENUM (
      'OWNER',
      'CO_OWNER',
      'RESIDENT',
      'GUEST',
      'TECHNICIAN',
      'VIEWER'
    );
  END IF;
END $$;

DROP INDEX IF EXISTS uq_home_users_one_landlord_per_home;
DROP INDEX IF EXISTS uq_home_users_one_owner_per_home;
DROP INDEX IF EXISTS idx_home_users_home_role;

ALTER TABLE home_users
  ALTER COLUMN role_in_home TYPE home_user_role
  USING (
    CASE role_in_home::text
      WHEN 'ADMIN' THEN 'OWNER'::home_user_role
      WHEN 'LANDLORD' THEN 'OWNER'::home_user_role
      WHEN 'INSTALLER' THEN 'TECHNICIAN'::home_user_role
      ELSE 'RESIDENT'::home_user_role
    END
  );


-- =========================================
-- 3) FIX duplicate OWNER per home
-- ưu tiên user = homes.owner_user_id
-- vì schema hiện tại KHÔNG có is_primary, updated_at
-- =========================================
WITH ranked_owner AS (
    SELECT
        hu.home_id,
        hu.user_id,
        ROW_NUMBER() OVER (
            PARTITION BY hu.home_id
            ORDER BY
                CASE
                    WHEN h.owner_user_id IS NOT NULL
                         AND hu.user_id = h.owner_user_id THEN 0
                    ELSE 1
                END,
                hu.created_at ASC,
                hu.user_id ASC
        ) AS rn
    FROM home_users hu
    JOIN homes h ON h.id = hu.home_id
    WHERE hu.role_in_home = 'OWNER'::home_user_role
)
UPDATE home_users hu
SET role_in_home = 'CO_OWNER'::home_user_role
FROM ranked_owner ro
WHERE hu.home_id = ro.home_id
  AND hu.user_id = ro.user_id
  AND ro.rn > 1;


-- =========================================
-- 4) RECREATE indexes for new home role
-- =========================================
CREATE INDEX IF NOT EXISTS idx_home_users_home_role
  ON home_users(home_id, role_in_home);

CREATE UNIQUE INDEX IF NOT EXISTS uq_home_users_one_owner_per_home
  ON home_users(home_id)
  WHERE role_in_home = 'OWNER'::home_user_role;


-- =========================================
-- 5) DROP old enum user_role only if unused
-- =========================================
DO $$
DECLARE
  v_count int;
BEGIN
  SELECT COUNT(*)
  INTO v_count
  FROM pg_attribute a
  JOIN pg_class c      ON a.attrelid = c.oid
  JOIN pg_type t       ON a.atttypid = t.oid
  JOIN pg_namespace n  ON c.relnamespace = n.oid
  WHERE t.typname = 'user_role'
    AND a.attnum > 0
    AND NOT a.attisdropped;

  IF v_count = 0 THEN
    DROP TYPE IF EXISTS user_role;
  END IF;
END $$;

-- =========================
-- TRIGGER FUNCTION
-- =========================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =========================
-- VALIDATION FUNCTIONS
-- =========================

-- sensor phải thuộc device class SENSOR_NODE
CREATE OR REPLACE FUNCTION validate_sensor_device_class()
RETURNS TRIGGER AS $$
DECLARE
  v_class device_class;
BEGIN
  SELECT d.class
  INTO v_class
  FROM devices d
  WHERE d.id = NEW.device_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Device % does not exist', NEW.device_id;
  END IF;

  IF v_class <> 'SENSOR_NODE' THEN
    RAISE EXCEPTION 'Sensor can only belong to device with class SENSOR_NODE, got %', v_class;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- runtime_state phải đúng kiểu capability và nằm trong range nếu là NUMBER
CREATE OR REPLACE FUNCTION validate_device_runtime_state()
RETURNS TRIGGER AS $$
DECLARE
  v_value_type VARCHAR(16);
  v_min DOUBLE PRECISION;
  v_max DOUBLE PRECISION;
BEGIN
  SELECT dc.value_type, dc.min_value, dc.max_value
  INTO v_value_type, v_min, v_max
  FROM device_capabilities dc
  WHERE dc.device_id = NEW.device_id
    AND dc.capability_code = NEW.capability_code;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Capability % of device % does not exist',
      NEW.capability_code, NEW.device_id;
  END IF;

  IF v_value_type = 'BOOLEAN' THEN
    IF NEW.value_boolean IS NULL OR NEW.value_number IS NOT NULL OR NEW.value_text IS NOT NULL THEN
      RAISE EXCEPTION 'Runtime state type mismatch: expected BOOLEAN';
    END IF;

  ELSIF v_value_type = 'NUMBER' THEN
    IF NEW.value_number IS NULL OR NEW.value_boolean IS NOT NULL OR NEW.value_text IS NOT NULL THEN
      RAISE EXCEPTION 'Runtime state type mismatch: expected NUMBER';
    END IF;

    IF v_min IS NOT NULL AND NEW.value_number < v_min THEN
      RAISE EXCEPTION 'Runtime numeric value % is below min %', NEW.value_number, v_min;
    END IF;

    IF v_max IS NOT NULL AND NEW.value_number > v_max THEN
      RAISE EXCEPTION 'Runtime numeric value % is above max %', NEW.value_number, v_max;
    END IF;

  ELSIF v_value_type IN ('TEXT', 'MODE') THEN
    IF NEW.value_text IS NULL OR NEW.value_boolean IS NOT NULL OR NEW.value_number IS NOT NULL THEN
      RAISE EXCEPTION 'Runtime state type mismatch: expected TEXT/MODE';
    END IF;

    IF v_value_type = 'MODE' AND NEW.value_text NOT IN ('auto', 'manual', 'sleep', 'away') THEN
      RAISE EXCEPTION 'Invalid MODE value: %', NEW.value_text;
    END IF;

  ELSE
    RAISE EXCEPTION 'Unsupported capability value_type: %', v_value_type;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- schedule phải đúng kiểu capability và nằm trong range nếu là NUMBER
CREATE OR REPLACE FUNCTION validate_schedule_value()
RETURNS TRIGGER AS $$
DECLARE
  v_value_type VARCHAR(16);
  v_min DOUBLE PRECISION;
  v_max DOUBLE PRECISION;
BEGIN
  SELECT dc.value_type, dc.min_value, dc.max_value
  INTO v_value_type, v_min, v_max
  FROM device_capabilities dc
  WHERE dc.device_id = NEW.device_id
    AND dc.capability_code = NEW.capability_code;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Capability % of device % does not exist',
      NEW.capability_code, NEW.device_id;
  END IF;

  IF v_value_type = 'BOOLEAN' THEN
    IF NEW.value_boolean IS NULL OR NEW.value_number IS NOT NULL OR NEW.value_text IS NOT NULL THEN
      RAISE EXCEPTION 'Schedule value type mismatch: expected BOOLEAN';
    END IF;

  ELSIF v_value_type = 'NUMBER' THEN
    IF NEW.value_number IS NULL OR NEW.value_boolean IS NOT NULL OR NEW.value_text IS NOT NULL THEN
      RAISE EXCEPTION 'Schedule value type mismatch: expected NUMBER';
    END IF;

    IF v_min IS NOT NULL AND NEW.value_number < v_min THEN
      RAISE EXCEPTION 'Schedule numeric value % is below min %', NEW.value_number, v_min;
    END IF;

    IF v_max IS NOT NULL AND NEW.value_number > v_max THEN
      RAISE EXCEPTION 'Schedule numeric value % is above max %', NEW.value_number, v_max;
    END IF;

  ELSIF v_value_type IN ('TEXT', 'MODE') THEN
    IF NEW.value_text IS NULL OR NEW.value_boolean IS NOT NULL OR NEW.value_number IS NOT NULL THEN
      RAISE EXCEPTION 'Schedule value type mismatch: expected TEXT/MODE';
    END IF;

    IF v_value_type = 'MODE' AND NEW.value_text NOT IN ('auto', 'manual', 'sleep', 'away') THEN
      RAISE EXCEPTION 'Invalid MODE value: %', NEW.value_text;
    END IF;

  ELSE
    RAISE EXCEPTION 'Unsupported capability value_type: %', v_value_type;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- command phải đúng kiểu capability và nằm trong range nếu là NUMBER
CREATE OR REPLACE FUNCTION validate_control_command_value()
RETURNS TRIGGER AS $$
DECLARE
  v_value_type VARCHAR(16);
  v_min DOUBLE PRECISION;
  v_max DOUBLE PRECISION;
BEGIN
  SELECT dc.value_type, dc.min_value, dc.max_value
  INTO v_value_type, v_min, v_max
  FROM device_capabilities dc
  WHERE dc.device_id = NEW.device_id
    AND dc.capability_code = NEW.target;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Capability % of device % does not exist',
      NEW.target, NEW.device_id;
  END IF;

  IF v_value_type = 'BOOLEAN' THEN
    IF NEW.value_boolean IS NULL OR NEW.value_number IS NOT NULL OR NEW.value_text IS NOT NULL THEN
      RAISE EXCEPTION 'Command value type mismatch: expected BOOLEAN';
    END IF;

  ELSIF v_value_type = 'NUMBER' THEN
    IF NEW.value_number IS NULL OR NEW.value_boolean IS NOT NULL OR NEW.value_text IS NOT NULL THEN
      RAISE EXCEPTION 'Command value type mismatch: expected NUMBER';
    END IF;

    IF v_min IS NOT NULL AND NEW.value_number < v_min THEN
      RAISE EXCEPTION 'Command numeric value % is below min %', NEW.value_number, v_min;
    END IF;

    IF v_max IS NOT NULL AND NEW.value_number > v_max THEN
      RAISE EXCEPTION 'Command numeric value % is above max %', NEW.value_number, v_max;
    END IF;

  ELSIF v_value_type IN ('TEXT', 'MODE') THEN
    IF NEW.value_text IS NULL OR NEW.value_boolean IS NOT NULL OR NEW.value_number IS NOT NULL THEN
      RAISE EXCEPTION 'Command value type mismatch: expected TEXT/MODE';
    END IF;

    IF v_value_type = 'MODE' AND NEW.value_text NOT IN ('auto', 'manual', 'sleep', 'away') THEN
      RAISE EXCEPTION 'Invalid MODE value: %', NEW.value_text;
    END IF;

  ELSE
    RAISE EXCEPTION 'Unsupported capability value_type: %', v_value_type;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- device_configs phải cùng home
CREATE OR REPLACE FUNCTION validate_device_config_same_home()
RETURNS TRIGGER AS $$
DECLARE
  v_device_home BIGINT;
  v_config_home BIGINT;
BEGIN
  SELECT home_id INTO v_device_home
  FROM devices
  WHERE id = NEW.device_id;

  SELECT home_id INTO v_config_home
  FROM configs
  WHERE id = NEW.config_id;

  IF v_device_home IS NULL OR v_config_home IS NULL THEN
    RAISE EXCEPTION 'Device or config not found';
  END IF;

  IF v_device_home <> v_config_home THEN
    RAISE EXCEPTION 'Config % does not belong to same home as device %', NEW.config_id, NEW.device_id;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- alerts phải nhất quán home với device/sensor
CREATE OR REPLACE FUNCTION validate_alert_references()
RETURNS TRIGGER AS $$
DECLARE
  v_device_home BIGINT;
  v_sensor_home BIGINT;
BEGIN
  IF NEW.device_id IS NOT NULL THEN
    SELECT d.home_id INTO v_device_home
    FROM devices d
    WHERE d.id = NEW.device_id;

    IF v_device_home IS NULL THEN
      RAISE EXCEPTION 'Device % not found', NEW.device_id;
    END IF;

    IF v_device_home <> NEW.home_id THEN
      RAISE EXCEPTION 'Alert home_id does not match device home';
    END IF;
  END IF;

  IF NEW.sensor_id IS NOT NULL THEN
    SELECT d.home_id INTO v_sensor_home
    FROM sensors s
    JOIN devices d ON d.id = s.device_id
    WHERE s.id = NEW.sensor_id;

    IF v_sensor_home IS NULL THEN
      RAISE EXCEPTION 'Sensor % not found', NEW.sensor_id;
    END IF;

    IF v_sensor_home <> NEW.home_id THEN
      RAISE EXCEPTION 'Alert home_id does not match sensor home';
    END IF;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- home owner nếu có thì owner phải là user role LANDLORD hoặc ADMIN
CREATE OR REPLACE FUNCTION validate_home_owner_role()
RETURNS TRIGGER AS $$
DECLARE
  v_role system_user_role;
BEGIN
  IF NEW.owner_user_id IS NULL THEN
    RETURN NEW;
  END IF;

  SELECT role INTO v_role
  FROM users
  WHERE id = NEW.owner_user_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Owner user % not found', NEW.owner_user_id;
  END IF;

  IF v_role NOT IN ('CUSTOMER', 'ADMIN', 'SUPER_ADMIN') THEN
    RAISE EXCEPTION 'Home owner must have system role CUSTOMER, ADMIN or SUPER_ADMIN';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =========================
-- TRIGGERS
-- =========================
DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
DROP TRIGGER IF EXISTS trg_homes_updated_at ON homes;
DROP TRIGGER IF EXISTS trg_devices_updated_at ON devices;
DROP TRIGGER IF EXISTS trg_sensors_updated_at ON sensors;
DROP TRIGGER IF EXISTS trg_configs_updated_at ON configs;
DROP TRIGGER IF EXISTS trg_runtime_state_updated_at ON device_runtime_state;
DROP TRIGGER IF EXISTS trg_schedules_updated_at ON schedules;

DROP TRIGGER IF EXISTS trg_validate_sensor_device_class ON sensors;
DROP TRIGGER IF EXISTS trg_validate_runtime_state ON device_runtime_state;
DROP TRIGGER IF EXISTS trg_validate_schedule_value ON schedules;
DROP TRIGGER IF EXISTS trg_validate_control_command_value ON control_commands;
DROP TRIGGER IF EXISTS trg_validate_device_config_same_home ON device_configs;
DROP TRIGGER IF EXISTS trg_validate_alert_references ON alerts;
DROP TRIGGER IF EXISTS trg_validate_home_owner_role ON homes;

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_homes_updated_at
BEFORE UPDATE ON homes
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_devices_updated_at
BEFORE UPDATE ON devices
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_sensors_updated_at
BEFORE UPDATE ON sensors
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_configs_updated_at
BEFORE UPDATE ON configs
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_runtime_state_updated_at
BEFORE UPDATE ON device_runtime_state
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_schedules_updated_at
BEFORE UPDATE ON schedules
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_validate_sensor_device_class
BEFORE INSERT OR UPDATE ON sensors
FOR EACH ROW
EXECUTE FUNCTION validate_sensor_device_class();

CREATE TRIGGER trg_validate_runtime_state
BEFORE INSERT OR UPDATE ON device_runtime_state
FOR EACH ROW
EXECUTE FUNCTION validate_device_runtime_state();

CREATE TRIGGER trg_validate_schedule_value
BEFORE INSERT OR UPDATE ON schedules
FOR EACH ROW
EXECUTE FUNCTION validate_schedule_value();

CREATE TRIGGER trg_validate_control_command_value
BEFORE INSERT OR UPDATE ON control_commands
FOR EACH ROW
EXECUTE FUNCTION validate_control_command_value();

CREATE TRIGGER trg_validate_device_config_same_home
BEFORE INSERT OR UPDATE ON device_configs
FOR EACH ROW
EXECUTE FUNCTION validate_device_config_same_home();

CREATE TRIGGER trg_validate_alert_references
BEFORE INSERT OR UPDATE ON alerts
FOR EACH ROW
EXECUTE FUNCTION validate_alert_references();

CREATE TRIGGER trg_validate_home_owner_role
BEFORE INSERT OR UPDATE ON homes
FOR EACH ROW
EXECUTE FUNCTION validate_home_owner_role();

Alter table configs add column auto_fan_speed INTEGER;
Alter table configs add column sleep_fan_speed INTEGER;
Alter table configs add column away_fan_speed INTEGER;

alter table configs
    add column if not exists is_active boolean not null default false,
    add column if not exists monitoring_temperature_device_id bigint,
    add column if not exists monitoring_humidity_device_id bigint,
    add column if not exists monitoring_light_device_id bigint,
    add column if not exists monitoring_motion_device_id bigint;

alter table configs
    add constraint fk_configs_monitoring_temperature_device
        foreign key (monitoring_temperature_device_id) references devices(id);

alter table configs
    add constraint fk_configs_monitoring_humidity_device
        foreign key (monitoring_humidity_device_id) references devices(id);

alter table configs
    add constraint fk_configs_monitoring_light_device
        foreign key (monitoring_light_device_id) references devices(id);

alter table configs
    add constraint fk_configs_monitoring_motion_device
        foreign key (monitoring_motion_device_id) references devices(id);

create index if not exists idx_configs_home_active
    on configs(home_id, is_active);

create unique index if not exists uq_configs_one_active_per_home
on configs(home_id)
where is_active = true;

ALTER TABLE home_users
  ADD COLUMN IF NOT EXISTS allow_profile_activation BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE home_users
  ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE home_users
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE home_users
  ADD CONSTRAINT ck_home_users_allow_profile_activation_bool
  CHECK (allow_profile_activation IN (TRUE, FALSE));

ALTER TABLE home_users
  ADD CONSTRAINT ck_home_users_is_primary_bool
  CHECK (is_primary IN (TRUE, FALSE));

CREATE UNIQUE INDEX IF NOT EXISTS uq_home_users_primary_per_user
  ON home_users(user_id)
  WHERE is_primary = TRUE;

CREATE INDEX IF NOT EXISTS idx_home_users_user_id
  ON home_users(user_id);

CREATE INDEX IF NOT EXISTS idx_home_users_home_role
  ON home_users(home_id, role_in_home);

CREATE UNIQUE INDEX IF NOT EXISTS uq_home_users_one_landlord_per_home
  ON home_users(home_id)
  WHERE role_in_home = 'LANDLORD';

-- =========================
-- OPTIONAL CLEANUP FOR OLD SCHEMA
-- =========================
ALTER TABLE devices DROP COLUMN IF EXISTS mode;
DROP TABLE IF EXISTS device_state CASCADE;


-- =========================
-- SEED DATA
-- =========================

-- Users
INSERT INTO users (username, password_hash, role, status)
VALUES
  ('admin', '$2b$12$example_admin_hash', 'ADMIN', 'ACTIVE'),
  ('landlord1', '$2b$12$example_landlord_hash', 'LANDLORD', 'ACTIVE'),
  ('installer1', '$2b$12$example_installer_hash', 'INSTALLER', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;


-- Home
INSERT INTO homes (name, address, owner_user_id)
SELECT
  'Nhà thông minh mẫu',
  'TP.HCM',
  u.id
FROM users u
WHERE u.username = 'landlord1'
  AND NOT EXISTS (
    SELECT 1
    FROM homes h
    WHERE h.name = 'Nhà thông minh mẫu'
  );


-- Home members
INSERT INTO home_users (home_id, user_id, role_in_home)
SELECT h.id, u.id, u.role
FROM homes h
JOIN users u ON u.username IN ('landlord1', 'admin', 'installer1')
WHERE h.name = 'Nhà thông minh mẫu'
ON CONFLICT (home_id, user_id) DO NOTHING;


-- Default config
INSERT INTO configs (
  home_id,
  name,
  created_by,
  thigh,
  tlow,
  lhigh,
  llow,
  tsleep_high,
  tsleep_low,
  taway_high,
  tcritical,
  n_minutes,
  m_minutes,
  thold_minutes,
  dpresent,
  k_minutes
)
SELECT
  h.id,
  'Default Smart House Config',
  u.id,
  30.0,
  27.0,
  55,
  35,
  32.0,
  26.0,
  33.0,
  35.0,
  2,
  2,
  5,
  20,
  5
FROM homes h
LEFT JOIN users u ON u.username = 'admin'
WHERE h.name = 'Nhà thông minh mẫu'
ON CONFLICT (home_id, name) DO NOTHING;


-- =========================================
-- DEVICES SEED
-- =========================================
BEGIN;

-- 1) Controller
INSERT INTO devices (home_id, device_key, name, class, subtype, room_name, installed_by)
SELECT h.id, 'yolobit-01', 'Living Room Controller', 'CONTROLLER', 'SMART_CONTROLLER', 'Phòng khách', u.id
FROM homes h
LEFT JOIN users u ON u.username = 'installer1'
WHERE h.name = 'Nhà thông minh mẫu'
ON CONFLICT (device_key) DO NOTHING;

-- 2) Fan actuator
INSERT INTO devices (home_id, device_key, name, class, subtype, room_name, installed_by)
SELECT h.id, 'ohstem-fan-ctrl-01', 'OhStem Fan Control', 'ACTUATOR', 'FAN', 'Phòng khách', u.id
FROM homes h
LEFT JOIN users u ON u.username = 'installer1'
WHERE h.name = 'Nhà thông minh mẫu'
ON CONFLICT (device_key) DO NOTHING;

-- 3) Light actuator
INSERT INTO devices (home_id, device_key, name, class, subtype, room_name, installed_by)
SELECT h.id, 'ohstem-light-ctrl-01', 'OhStem Light Control', 'ACTUATOR', 'LIGHT', 'Phòng khách', u.id
FROM homes h
LEFT JOIN users u ON u.username = 'installer1'
WHERE h.name = 'Nhà thông minh mẫu'
ON CONFLICT (device_key) DO NOTHING;

-- 4) Sensor nodes
INSERT INTO devices (home_id, device_key, name, class, subtype, room_name, installed_by)
SELECT h.id, 'ohstem-temp-01', 'OhStem Temperature Sender', 'SENSOR_NODE', 'TEMPERATURE_NODE', 'Phòng khách', u.id
FROM homes h
LEFT JOIN users u ON u.username = 'installer1'
WHERE h.name = 'Nhà thông minh mẫu'
ON CONFLICT (device_key) DO NOTHING;

INSERT INTO devices (home_id, device_key, name, class, subtype, room_name, installed_by)
SELECT h.id, 'ohstem-humidity-01', 'OhStem Humidity Sender', 'SENSOR_NODE', 'HUMIDITY_NODE', 'Phòng khách', u.id
FROM homes h
LEFT JOIN users u ON u.username = 'installer1'
WHERE h.name = 'Nhà thông minh mẫu'
ON CONFLICT (device_key) DO NOTHING;

INSERT INTO devices (home_id, device_key, name, class, subtype, room_name, installed_by)
SELECT h.id, 'ohstem-light-01', 'OhStem Light Sender', 'SENSOR_NODE', 'LIGHT_NODE', 'Phòng khách', u.id
FROM homes h
LEFT JOIN users u ON u.username = 'installer1'
WHERE h.name = 'Nhà thông minh mẫu'
ON CONFLICT (device_key) DO NOTHING;

INSERT INTO devices (home_id, device_key, name, class, subtype, room_name, installed_by)
SELECT h.id, 'ohstem-motion-01', 'OhStem Motion Sender', 'SENSOR_NODE', 'MOTION_NODE', 'Phòng khách', u.id
FROM homes h
LEFT JOIN users u ON u.username = 'installer1'
WHERE h.name = 'Nhà thông minh mẫu'
ON CONFLICT (device_key) DO NOTHING;


-- =========================================
-- CAPABILITIES
-- =========================================

-- Controller capabilities
INSERT INTO device_capabilities (device_id, capability_code, value_type, is_writable, min_value, max_value, unit)
SELECT d.id, 'MODE', 'MODE', TRUE, NULL, NULL, NULL
FROM devices d
WHERE d.device_key = 'yolobit-01'
ON CONFLICT (device_id, capability_code) DO NOTHING;

-- Fan capabilities
INSERT INTO device_capabilities (device_id, capability_code, value_type, is_writable, min_value, max_value, unit)
SELECT d.id, 'POWER', 'BOOLEAN', TRUE, NULL, NULL, NULL
FROM devices d
WHERE d.device_key = 'ohstem-fan-ctrl-01'
ON CONFLICT (device_id, capability_code) DO NOTHING;

INSERT INTO device_capabilities (device_id, capability_code, value_type, is_writable, min_value, max_value, unit)
SELECT d.id, 'SPEED', 'NUMBER', TRUE, 0, 100, '%'
FROM devices d
WHERE d.device_key = 'ohstem-fan-ctrl-01'
ON CONFLICT (device_id, capability_code) DO NOTHING;

-- Light capabilities
INSERT INTO device_capabilities (device_id, capability_code, value_type, is_writable, min_value, max_value, unit)
SELECT d.id, 'POWER', 'BOOLEAN', TRUE, NULL, NULL, NULL
FROM devices d
WHERE d.device_key = 'ohstem-light-ctrl-01'
ON CONFLICT (device_id, capability_code) DO NOTHING;

INSERT INTO device_capabilities (device_id, capability_code, value_type, is_writable, min_value, max_value, unit)
SELECT d.id, 'BRIGHTNESS', 'NUMBER', TRUE, 0, 100, '%'
FROM devices d
WHERE d.device_key = 'ohstem-light-ctrl-01'
ON CONFLICT (device_id, capability_code) DO NOTHING;


-- =========================================
-- RUNTIME STATE
-- =========================================

-- Controller mode
INSERT INTO device_runtime_state (device_id, capability_code, value_text)
SELECT d.id, 'MODE', 'auto'
FROM devices d
WHERE d.device_key = 'yolobit-01'
ON CONFLICT (device_id, capability_code) DO NOTHING;

-- Fan state
INSERT INTO device_runtime_state (device_id, capability_code, value_boolean)
SELECT d.id, 'POWER', FALSE
FROM devices d
WHERE d.device_key = 'ohstem-fan-ctrl-01'
ON CONFLICT (device_id, capability_code) DO NOTHING;

INSERT INTO device_runtime_state (device_id, capability_code, value_number)
SELECT d.id, 'SPEED', 0
FROM devices d
WHERE d.device_key = 'ohstem-fan-ctrl-01'
ON CONFLICT (device_id, capability_code) DO NOTHING;

-- Light state
INSERT INTO device_runtime_state (device_id, capability_code, value_boolean)
SELECT d.id, 'POWER', FALSE
FROM devices d
WHERE d.device_key = 'ohstem-light-ctrl-01'
ON CONFLICT (device_id, capability_code) DO NOTHING;

INSERT INTO device_runtime_state (device_id, capability_code, value_number)
SELECT d.id, 'BRIGHTNESS', 0
FROM devices d
WHERE d.device_key = 'ohstem-light-ctrl-01'
ON CONFLICT (device_id, capability_code) DO NOTHING;


-- =========================================
-- SENSORS
-- =========================================
INSERT INTO sensors (device_id, name, sensor_kind, unit, status, last_seen, created_at, updated_at)
SELECT d.id, 'Nhiệt độ OhStem', 'TEMPERATURE', '°C', 'ACTIVE', NULL, now(), now()
FROM devices d
WHERE d.device_key = 'ohstem-temp-01'
ON CONFLICT (device_id, sensor_kind) DO NOTHING;

INSERT INTO sensors (device_id, name, sensor_kind, unit, status, last_seen, created_at, updated_at)
SELECT d.id, 'Độ ẩm OhStem', 'HUMIDITY', '%', 'ACTIVE', NULL, now(), now()
FROM devices d
WHERE d.device_key = 'ohstem-humidity-01'
ON CONFLICT (device_id, sensor_kind) DO NOTHING;

INSERT INTO sensors (device_id, name, sensor_kind, unit, status, last_seen, created_at, updated_at)
SELECT d.id, 'Ánh sáng OhStem', 'LIGHT', '%', 'ACTIVE', NULL, now(), now()
FROM devices d
WHERE d.device_key = 'ohstem-light-01'
ON CONFLICT (device_id, sensor_kind) DO NOTHING;

INSERT INTO sensors (device_id, name, sensor_kind, unit, status, last_seen, created_at, updated_at)
SELECT d.id, 'Chuyển động OhStem', 'MOTION', 'bool', 'ACTIVE', NULL, now(), now()
FROM devices d
WHERE d.device_key = 'ohstem-motion-01'
ON CONFLICT (device_id, sensor_kind) DO NOTHING;


-- =========================================
-- DEVICE CONFIGS
-- =========================================
INSERT INTO device_configs (device_id, config_id, changed_by, changed_at, reason)
SELECT
  d.id,
  c.id,
  u.id,
  now(),
  'Initial default config'
FROM devices d
JOIN homes h ON h.id = d.home_id
JOIN configs c ON c.home_id = h.id AND c.name = 'Default Smart House Config'
LEFT JOIN users u ON u.username = 'admin'
WHERE d.device_key IN (
  'yolobit-01',
  'ohstem-fan-ctrl-01',
  'ohstem-light-ctrl-01',
  'ohstem-temp-01',
  'ohstem-humidity-01',
  'ohstem-light-01',
  'ohstem-motion-01'
)
ON CONFLICT (device_id) DO NOTHING;


-- =========================================
-- SCHEDULES SEED
-- Chỉ là lịch áp dụng giá trị, không phải state
-- =========================================

-- Fan: 18:00 bật quạt
INSERT INTO schedules (
  device_id, capability_code, value_boolean,
  start_time, end_time, days_mask, enabled, priority, created_by
)
SELECT d.id, 'POWER', TRUE, '18:00', NULL, 127, TRUE, 0, u.id
FROM devices d
LEFT JOIN users u ON u.username = 'admin'
WHERE d.device_key = 'ohstem-fan-ctrl-01'
  AND NOT EXISTS (
    SELECT 1
    FROM schedules s
    WHERE s.device_id = d.id
      AND s.capability_code = 'POWER'
      AND s.start_time = '18:00'
      AND s.value_boolean = TRUE
  );

-- Fan: 18:05 đặt tốc độ 60
INSERT INTO schedules (
  device_id, capability_code, value_number,
  start_time, end_time, days_mask, enabled, priority, created_by
)
SELECT d.id, 'SPEED', 60, '18:05', NULL, 127, TRUE, 0, u.id
FROM devices d
LEFT JOIN users u ON u.username = 'admin'
WHERE d.device_key = 'ohstem-fan-ctrl-01'
  AND NOT EXISTS (
    SELECT 1
    FROM schedules s
    WHERE s.device_id = d.id
      AND s.capability_code = 'SPEED'
      AND s.start_time = '18:05'
      AND s.value_number = 60
  );

-- Light: 18:00 bật đèn
INSERT INTO schedules (
  device_id, capability_code, value_boolean,
  start_time, end_time, days_mask, enabled, priority, created_by
)
SELECT d.id, 'POWER', TRUE, '18:00', NULL, 127, TRUE, 0, u.id
FROM devices d
LEFT JOIN users u ON u.username = 'admin'
WHERE d.device_key = 'ohstem-light-ctrl-01'
  AND NOT EXISTS (
    SELECT 1
    FROM schedules s
    WHERE s.device_id = d.id
      AND s.capability_code = 'POWER'
      AND s.start_time = '18:00'
      AND s.value_boolean = TRUE
  );

-- Light: 18:01 brightness 70
INSERT INTO schedules (
  device_id, capability_code, value_number,
  start_time, end_time, days_mask, enabled, priority, created_by
)
SELECT d.id, 'BRIGHTNESS', 70, '18:01', NULL, 127, TRUE, 0, u.id
FROM devices d
LEFT JOIN users u ON u.username = 'admin'
WHERE d.device_key = 'ohstem-light-ctrl-01'
  AND NOT EXISTS (
    SELECT 1
    FROM schedules s
    WHERE s.device_id = d.id
      AND s.capability_code = 'BRIGHTNESS'
      AND s.start_time = '18:01'
      AND s.value_number = 70
  );

-- Controller: 22:00 chuyển sang sleep
INSERT INTO schedules (
  device_id, capability_code, value_text,
  start_time, end_time, days_mask, enabled, priority, created_by
)
SELECT d.id, 'MODE', 'sleep', '22:00', NULL, 127, TRUE, 0, u.id
FROM devices d
LEFT JOIN users u ON u.username = 'admin'
WHERE d.device_key = 'yolobit-01'
  AND NOT EXISTS (
    SELECT 1
    FROM schedules s
    WHERE s.device_id = d.id
      AND s.capability_code = 'MODE'
      AND s.start_time = '22:00'
      AND s.value_text = 'sleep'
  );


-- =========================================
-- ACTIVITY LOGS
-- =========================================
INSERT INTO activity_logs (home_id, device_id, user_id, action, method, old_value, new_value, detail, created_at)
SELECT
  h.id,
  d.id,
  u.id,
  'INIT_DEVICE',
  'system',
  NULL,
  NULL,
  jsonb_build_object(
    'deviceKey', d.device_key,
    'deviceName', d.name,
    'home', h.name,
    'class', d.class,
    'subtype', d.subtype,
    'roomName', d.room_name,
    'purpose', 'controller'
  ),
  now()
FROM homes h
JOIN devices d ON d.home_id = h.id
LEFT JOIN users u ON u.username = 'admin'
WHERE h.name = 'Nhà thông minh mẫu'
  AND d.device_key = 'yolobit-01'
  AND NOT EXISTS (
    SELECT 1
    FROM activity_logs al
    WHERE al.device_id = d.id
      AND al.action = 'INIT_DEVICE'
      AND al.detail ->> 'purpose' = 'controller'
  );

INSERT INTO activity_logs (home_id, device_id, user_id, action, method, old_value, new_value, detail, created_at)
SELECT
  h.id,
  d.id,
  u.id,
  'INIT_DEVICE',
  'system',
  NULL,
  NULL,
  jsonb_build_object(
    'deviceKey', d.device_key,
    'deviceName', d.name,
    'home', h.name,
    'class', d.class,
    'subtype', d.subtype,
    'roomName', d.room_name,
    'purpose', 'actuator'
  ),
  now()
FROM homes h
JOIN devices d ON d.home_id = h.id
LEFT JOIN users u ON u.username = 'admin'
WHERE h.name = 'Nhà thông minh mẫu'
  AND d.device_key IN ('ohstem-fan-ctrl-01', 'ohstem-light-ctrl-01')
  AND NOT EXISTS (
    SELECT 1
    FROM activity_logs al
    WHERE al.device_id = d.id
      AND al.action = 'INIT_DEVICE'
      AND al.detail ->> 'purpose' = 'actuator'
  );

INSERT INTO activity_logs (home_id, device_id, user_id, action, method, old_value, new_value, detail, created_at)
SELECT
  h.id,
  d.id,
  u.id,
  'INIT_DEVICE',
  'system',
  NULL,
  NULL,
  jsonb_build_object(
    'deviceKey', d.device_key,
    'deviceName', d.name,
    'home', h.name,
    'class', d.class,
    'subtype', d.subtype,
    'roomName', d.room_name,
    'purpose', 'sensor node'
  ),
  now()
FROM homes h
JOIN devices d ON d.home_id = h.id
LEFT JOIN users u ON u.username = 'admin'
WHERE h.name = 'Nhà thông minh mẫu'
  AND d.device_key IN (
    'ohstem-temp-01',
    'ohstem-humidity-01',
    'ohstem-light-01',
    'ohstem-motion-01'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM activity_logs al
    WHERE al.device_id = d.id
      AND al.action = 'INIT_DEVICE'
      AND al.detail ->> 'purpose' = 'sensor node'
  );

insert into device_capabilities (device_id, capability_code, value_type)
select d.id, 'TEMPERATURE', 'NUMBER'
from devices d
where d.device_key = 'ohstem-temp-01'
and not exists (
  select 1
  from device_capabilities dc
  where dc.device_id = d.id
    and dc.capability_code = 'TEMPERATURE'
);

insert into device_capabilities (device_id, capability_code, value_type)
select d.id, 'HUMIDITY', 'NUMBER'
from devices d
where d.device_key = 'ohstem-humidity-01'
and not exists (
  select 1
  from device_capabilities dc
  where dc.device_id = d.id
    and dc.capability_code = 'HUMIDITY'
);

insert into device_capabilities (device_id, capability_code, value_type)
select d.id, 'BRIGHTNESS', 'NUMBER'
from devices d
where d.device_key = 'ohstem-light-01'
and not exists (
  select 1
  from device_capabilities dc
  where dc.device_id = d.id
    and dc.capability_code = 'BRIGHTNESS'
);

insert into device_capabilities (device_id, capability_code, value_type)
select d.id, 'MOTION', 'BOOLEAN'
from devices d
where d.device_key = 'ohstem-motion-01'
and not exists (
  select 1
  from device_capabilities dc
  where dc.device_id = d.id
    and dc.capability_code = 'MOTION'
);

ALTER TABLE alerts DROP CONSTRAINT IF EXISTS ck_alerts_resolved_pair;

ALTER TABLE alerts
ADD CONSTRAINT ck_alerts_resolved_pair
CHECK (
  (resolved_at IS NULL AND resolved_by IS NULL AND status <> 'RESOLVED')
  OR (resolved_at IS NOT NULL AND status = 'RESOLVED')
);

COMMIT;
