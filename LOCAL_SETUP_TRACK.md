# Local/Flyway Track

Date: 2026-05-08

## Added

- Added Flyway dependencies in `backend/pom.xml`:
  - `org.flywaydb:flyway-core`
  - `org.flywaydb:flyway-database-postgresql`
- Added Flyway migration `backend/src/main/resources/db/migration/V1__init.sql` from `backend/db/init.sql`.
  - Removed `psql` meta commands that JDBC/Flyway cannot execute.
  - Removed `SET transaction_timeout = 0;` so local PostgreSQL versions older than 17 do not fail.
- Added `scripts/load-local-env.ps1`.
  - Detects LAN IPv4.
  - Loads root `.env`, applies local overrides, and writes `.env.local`.
  - Generates `aiot_local_config.py` so AIoT code and the virtual simulator use the same gateway host/port/token.
- Added generated local config files:
  - `.env.local`
  - `aiot_local_config.py`

## Updated For Local

- `backend/src/main/resources/application.properties`
  - Flyway enabled by default.
  - `baseline-on-migrate` enabled so an existing local schema can be adopted instead of failing.
  - Database fallback remains `jdbc:postgresql://localhost:5432/smarthouse`.
  - RabbitMQ fallback changed to local non-SSL `localhost:5672`.
  - Mail and Telegram notifications default to disabled for local runs.
  - JWT, Google OAuth, Telegram values now have local-safe fallbacks.
- `docker-compose.yml`
  - Removed direct Postgres init mount.
  - Backend container uses Flyway migration.
  - Docker backend still points to service names `db` and `rabbitmq` even though `.env` is local.
- `frontend/src/api/apiClient.js`
  - API fallback changed from Render to `http://localhost:8080`.
- `backend/src/main/java/com/java/config/SecurityConfig.java`
  - CORS now allows Vite from localhost and common LAN ranges on port `5173`.
- `.env`
  - Local defaults changed for `DB_URL`, RabbitMQ, gateway backend, YOLO, mail, and Telegram.
- `gateway.py`
  - Loads `.env.local`/`.env` automatically.
  - `GATEWAY_HOST` and `GATEWAY_PORT` are configurable.
  - Default upstreams are local.
- `aiot_code_v2.py` and `aiot_code_v3.py`
  - Read LAN gateway settings from `aiot_local_config.py`.
  - Fall back to local defaults if config is absent.
- `virtual_device_simulator.py`
  - Loads `.env.local`/`.env`.
  - Reads the same `aiot_local_config.py` values as AIoT code.

## Local Run Notes

Run this first from repo root when the LAN IP changes:

```powershell
& .\scripts\load-local-env.ps1
```

Or force a LAN IP:

```powershell
& .\scripts\load-local-env.ps1 -LanHost 10.229.90.208
```

Then use:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

```powershell
python gateway.py
```

```powershell
cd frontend
npm run dev -- --host 0.0.0.0
```
