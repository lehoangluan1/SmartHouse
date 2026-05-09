# AIOT Optimization Worklog

## 2026-05-07 latency-first correction pass

### Firmware audit
- Reviewed `aiot_code_v3.py` hot loop after the previous safe optimization pass.
- Found a correctness regression: `LAST[...]` was used by the scheduler but `LAST` was not initialized.
- Confirmed the old rotated command polling shape could make each command target wait one full rotation. With `command=3500 ms` and three targets, worst-case fetch delay was about `10.5 s + HTTP + loop sleep`.
- Confirmed the current edited file had moved away from rotation, but it could run command, state, telemetry, config, registry, and YOLO work in the same loop, creating blocking bursts.
- Confirmed current command polling at `120 ms` with three separate command GETs could exceed the gateway command rate limit and could still block one loop on three device HTTP calls.
- Confirmed LCD rendering used cached values and wrote only changed lines, so display can be refreshed independently from sensor sampling and telemetry.
- Confirmed telemetry upload was blocking and sent all telemetry values in one call group; changed it back to one item per scheduler pass.
- Confirmed `print_status()` printed every second in normal operation; production now gates it behind `STATUS_LOG=False`.

### Firmware changes
- Added production flags: `DEBUG=False`, `PERF_LOG=False`, `STATUS_LOG=False`.
- Restored scheduler clock map with `LAST={k:0 for k in ITV}`.
- Split DHT20 sampling (`sensor=1000 ms`) from fast local light/PIR sampling (`fast_sensor=100 ms`).
- Added LCD refresh interval (`display=150 ms`) using cached values; telemetry does not control LCD freshness.
- Added LED refresh interval (`led=100 ms`) to reduce repeated RGB writes.
- Changed command polling to `180 ms` normal, `400 ms` when the backend is unhealthy.
- Changed state sync to `3000 ms` normal, `5000 ms` when the backend is unhealthy.
- Changed telemetry to one value every `1200 ms` normal, `2500 ms` when unhealthy.
- Kept config refresh at `60000 ms` and registry refresh at `120000 ms`.
- Reworked backend scheduling so each loop does local hardware/display first, then at most one priority backend task:
  1. queued ACKs
  2. queued alerts
  3. command polling
  4. state sync
  5. YOLO check
  6. telemetry
  7. config
  8. registry
- Added batch command fetch path: `/gw/commands/next?keys=runtime,fan,light`, with fallback to old per-device command polling.
- Added small bounded ACK queue (`ACKQ`, max 6) and batch ACK path `/gw/commands/ack`.
- Changed command ACK handling so hardware is applied before ACK network work.
- Throttled ACK retries to `250 ms` so failed ACKs do not permanently starve command polling.
- Queued alert POSTs so security/high-temperature alert network calls do not run in the local critical path.
- Added compact performance summary support gated by `PERF_LOG=False`.
- Shortened socket timeout from `0.8 s` to `0.55 s` so unavailable backend/WiFi cannot freeze local behavior as long.

### Gateway/backend audit and changes
- Avoided Java backend changes; latency fix was possible with backward-compatible gateway aggregation over existing backend endpoints.
- Added gateway combined command endpoint `/gw/commands/next`.
- Added gateway combined ACK endpoint `/gw/commands/ack`.
- Added gateway combined state endpoint `/gw/devices/states?ids=...`.
- Raised gateway command rate-limit default from `60/min` to `600/min` to support responsive polling plus occasional ACKs.
- Changed gateway telemetry and ACK request body logs from `info` to `debug` to reduce normal runtime output.

### Latency notes
- Previous rotated command latency: `3 targets * 3500 ms = 10500 ms` worst-case before HTTP/loop overhead.
- Current pre-correction file was not rotated, but used 3 GETs per command tick and could burst multiple backend tasks in one loop.
- Corrected expected normal command fetch latency: up to `180 ms + one device HTTP GET + gateway/backend local fan-out + up to 8 ms loop sleep`.
- If backend is unhealthy: up to `400 ms + timeout/backoff behavior`, while local display/IR/door/hardware still run between failed backend attempts.
- Local IR, door close/open, actuator application, and local logic remain every loop or near every loop.
- LCD refresh is independent from telemetry and sensor upload; it uses cached sensor/state values every `150 ms`.

### Logging classification
- LCD output: preserved.
- Serial status logging: gated by `STATUS_LOG=False`.
- Performance logging: gated by `PERF_LOG=False`, compact 3-second summaries only.
- Firmware debug/error prints: no normal `print()` output remains unless a log flag is enabled.
- Gateway telemetry/ACK body logs: changed to debug level.
- Critical backend/gateway exceptions: kept via logger exception because they are server-side operational errors, not device serial spam.

### Memory notes
- Avoided many threads and did not add `uasyncio`; `urequests` remains blocking, so cooperative priority scheduling plus short socket timeout is safer on constrained OhStem/MicroPython.
- Kept queues bounded: ACK queue max 6, pending alert coalesced to one.
- Avoided per-loop status string construction unless logging is enabled.
- Reduced repeated LCD/RGB writes with display/LED intervals and changed-line LCD writes.
- Reduced telemetry allocation and network pressure by sending one telemetry item per low-priority pass.
- `gc.collect()` remains strategic, every 12 seconds.
