# Smart Home Mode Flows and OpenAPI Draft

## Document Status
Draft

## Purpose
This document describes the current behavioral flows of the smart-home modes found in the backend codebase, focusing on the modes **AUTO**, **MANUAL**, **AWAY**, and **SLEEP**. It also includes example conditions that trigger actions in each flow and an **OpenAPI draft** that can be used as a prompt or implementation guide.

## Important Note
This draft is based on the backend classes and snippets visible in the current discussion, especially the services and patterns around:

- `ModeAutomationServiceImpl`
- `AutoControlService`
- `ManualControlService`
- `ControlFacadeService`
- `HomeModeResolver`
- `SensorSnapshotService`
- `FanAutomationPolicy`
- `LightAutomationPolicy`
- `DeviceRuntimeStateService`
- `AutomationCooldownService`
- `ManualHoldQueryService`
- `DeviceTargetPolicy`
- telemetry / alert / runtime-state related services mentioned in the conversation

Because the full `backend/src/main` folder was not directly attached here, some parts below are **grounded interpretation** rather than a literal line-by-line extraction of the repository. The structure is designed to be precise enough for engineering docs and for prompt reuse.

---

## 1. System Behavior Overview

The backend appears to separate control logic into two main branches:

1. **Automation-driven control**
   - Used when the home is in an automated mode such as `AUTO`, `AWAY`, or `SLEEP`.
   - Decisions are derived from current sensor values, home mode, runtime state, configured thresholds, cooldown rules, and manual-hold constraints.

2. **Manual control**
   - Used when the user explicitly changes device state.
   - Manual commands typically override automation for a period of time or until a hold condition expires.

This design suggests the following responsibility split:

- **Mode resolver** decides what mode is currently active.
- **Snapshot service** collects the latest sensor readings.
- **Policy services** decide the desired target state for each device type.
- **Auto control service** applies automation decisions.
- **Manual control service** applies explicit user commands.
- **Device runtime state service** persists effective state and mode-related runtime values.
- **Cooldown / hold services** prevent unstable toggling and unwanted overrides.

---

## 2. High-Level Mode Semantics

### 2.1 AUTO mode
AUTO is the normal autonomous operating mode. Device behavior is determined by live environmental conditions and configuration thresholds.

Typical characteristics:
- Fan responds to temperature-related rules.
- Light responds to brightness and motion / occupancy-related rules.
- Automation is active unless blocked by manual hold or cooldown.

### 2.2 MANUAL mode
MANUAL means the user explicitly controls device state. Automation should not immediately undo the user action.

Typical characteristics:
- User sends `ON`, `OFF`, or mode-specific commands.
- Manual commands are persisted as runtime state.
- Automation may be temporarily suspended for affected devices.
- A manual-hold window may exist before automation can regain control.

### 2.3 AWAY mode
AWAY is an energy-saving / absence mode. The house assumes low or no occupancy and uses stricter rules.

Typical characteristics:
- Comfort thresholds may be relaxed.
- Lights are generally kept off unless a security or motion rule requires otherwise.
- Fan activation may use a higher temperature threshold than AUTO.
- Motion may be treated as a notable event rather than comfort input.

### 2.4 SLEEP mode
SLEEP is a nighttime comfort mode with different environmental targets.

Typical characteristics:
- Fan may operate with sleep-specific thresholds.
- Lighting is usually minimized or made more conservative.
- Motion handling may be softer than AUTO, depending on implementation.
- Transitions try to reduce disturbance.

---

## 3. Core Runtime Flow

The current backend likely follows this generalized control flow:

1. **Telemetry arrives** from sensors.
2. Telemetry is validated and persisted.
3. Latest values are converted into a sensor snapshot.
4. The system resolves the current home mode.
5. The corresponding policy logic evaluates each controllable device.
6. The system checks whether automation is allowed:
   - manual hold active?
   - cooldown active?
   - device target already satisfied?
7. If a change is needed, the target state is produced.
8. Runtime state is written.
9. A downstream device command / event / dashboard update is emitted.

This means mode logic is not just about thresholds. It is also constrained by:
- current device state
- anti-flapping rules
- recent manual interaction
- target-state reconciliation

---

## 4. Detailed Mode Flows

## 4.1 AUTO Mode Flow

### Intent
Maintain comfort automatically based on live sensor conditions.

### Likely inputs
- temperature
- humidity
- light level
- motion / presence
- home config thresholds
- current runtime state
- manual hold state
- cooldown state

### Main flow
1. New sensor data is received.
2. `HomeModeResolver` resolves mode as `AUTO`.
3. `SensorSnapshotService` loads the most recent readings relevant for automation.
4. `ModeAutomationServiceImpl` evaluates device decisions.
5. `FanAutomationPolicy` determines whether the fan target should be `ON` or `OFF`.
6. `LightAutomationPolicy` determines whether the light target should be `ON` or `OFF`.
7. `ManualHoldQueryService` checks whether the device is still under manual override.
8. `AutomationCooldownService` checks whether a recent automation action prevents another state change.
9. `DeviceTargetPolicy` / runtime-state reconciliation checks whether the desired state differs from the current effective state.
10. `AutoControlService` applies the action.
11. `DeviceRuntimeStateService` persists the resulting runtime state.

### Example conditions and actions

#### Fan in AUTO
Example rule pattern:
- If `temperature >= highThreshold`, turn fan `ON`.
- If `temperature <= lowThreshold`, turn fan `OFF`.

Example:
- `temperature = 31.2°C`
- config `highThreshold = 30°C`
- current fan state = `OFF`
- no manual hold
- no cooldown
- action: **turn fan ON**

Another example:
- `temperature = 26.5°C`
- config `lowThreshold = 27°C`
- current fan state = `ON`
- action: **turn fan OFF**

This suggests hysteresis-like behavior through separate high/low thresholds instead of a single cutoff.

#### Light in AUTO
Example rule pattern:
- If `lightLevel <= lowLightThreshold` and motion is detected, turn light `ON`.
- If `lightLevel >= highLightThreshold`, or no occupancy condition is satisfied, turn light `OFF`.

Example:
- `light = 20`
- `lowLightThreshold = 35`
- motion = `true`
- current light state = `OFF`
- action: **turn light ON**

Another example:
- `light = 60`
- `highLightThreshold = 55`
- current light state = `ON`
- action: **turn light OFF**

### Expected safeguards
- Cooldown prevents repeated `ON/OFF/ON/OFF` oscillation.
- Manual hold prevents AUTO from overriding a recent user action.
- If target state already equals current state, no command is sent.

---

## 4.2 MANUAL Mode Flow

### Intent
Honor user-issued commands directly.

### Trigger
A user sends a control request from web/mobile or another control endpoint.

### Main flow
1. User issues a command for a device.
2. `ControlFacadeService` receives the request.
3. It delegates to `ManualControlService` or a specific handler such as `ModeManualControlHandler` depending on command type.
4. The service validates:
   - device exists
   - capability exists
   - action/value is allowed
   - user / home scope is valid
5. The requested target state is computed.
6. `DeviceRuntimeStateService` writes the state.
7. A manual-hold marker may be set so automation will not immediately reverse the change.
8. Response returns updated state.

### Example conditions and actions

#### Manual fan ON
Example:
- User presses **Turn Fan ON**.
- Device exists and supports power control.
- current mode may still be AUTO at home level, but this device is now manually overridden.
- action: **fan ON** is persisted.
- expected side effect: automation for that fan is temporarily blocked.

#### Manual light OFF while room is dark
Example:
- Room is dark.
- AUTO would normally turn light ON.
- User explicitly turns light OFF.
- action: **light OFF** is applied.
- expected result: light remains OFF until manual hold expires or another explicit rule overrides it.

### Design meaning
MANUAL is not just a device state; it is also an **authority switch**. The user becomes the immediate source of truth for the controlled device.

---

## 4.3 AWAY Mode Flow

### Intent
Optimize for absence, energy saving, and possibly security sensitivity.

### Likely inputs
- away-specific temperature thresholds
- motion events
- brightness
- current runtime state
- cooldown / hold state

### Main flow
1. Mode resolver returns `AWAY`.
2. Automation service uses AWAY-specific thresholds from configuration.
3. Fan policy is evaluated with less aggressive comfort behavior.
4. Light policy is evaluated with stronger preference for `OFF`.
5. Security-related motion or anomaly logic may still produce events or alerts.
6. Auto control applies state changes only when allowed.

### Example conditions and actions

#### Fan in AWAY
Example rule pattern:
- Fan turns ON only at a higher threshold than AUTO.

Example:
- AUTO high temperature threshold = `30°C`
- AWAY high threshold = `33°C`
- `temperature = 31°C`
- current mode = `AWAY`
- action: **fan stays OFF**

Another example:
- `temperature = 34°C`
- AWAY high threshold = `33°C`
- action: **fan turns ON**

#### Light in AWAY
Example rule pattern:
- Keep light OFF by default.
- Turn ON only if a specific security or motion rule exists.

Example:
- `light = 10` but no occupancy expectation because house is away.
- action: **light remains OFF**

Possible security-flavored example:
- motion detected while in AWAY
- action may be one or more of:
  - keep light OFF
  - turn light ON briefly as deterrence
  - create alert / notification

The exact behavior depends on whether AWAY is modeled as pure energy saving or mixed with security automation.

---

## 4.4 SLEEP Mode Flow

### Intent
Provide nighttime comfort with reduced disturbance.

### Likely inputs
- sleep-specific temperature thresholds
- brightness
- motion
- current runtime state
- cooldown / hold state

### Main flow
1. Mode resolver returns `SLEEP`.
2. Automation service loads sleep-specific thresholds.
3. Fan policy uses a sleep comfort band.
4. Light policy becomes more conservative to avoid unnecessary lighting.
5. Auto control applies only required changes.

### Example conditions and actions

#### Fan in SLEEP
Example rule pattern:
- Use `Tsleep_high` and `Tsleep_low`.

Example:
- `Tsleep_high = 32°C`
- `Tsleep_low = 26°C`
- `temperature = 32.4°C`
- current fan state = `OFF`
- action: **fan turns ON**

Another example:
- `temperature = 25.8°C`
- current fan state = `ON`
- action: **fan turns OFF**

#### Light in SLEEP
Example rule pattern:
- Prefer OFF unless there is a strong reason to illuminate.

Example:
- room is dark
- motion = `false`
- action: **light remains OFF**

Possible motion example:
- motion = `true` during sleep mode
- if sleep policy supports safety navigation lighting, action may be:
  - light ON at reduced behavior level, or
  - keep OFF if current implementation only supports binary ON/OFF and chooses not to disturb

Because the shared code references a `LightAutomationPolicy` but not dimming, the most likely current implementation is still binary ON/OFF.

---

## 5. Decision Constraints Shared by All Automated Modes

## 5.1 Manual hold
If a user manually changed a device recently, automation should not immediately overwrite it.

Example:
- User turns light OFF manually.
- 10 seconds later, sensor input would normally turn it ON.
- `ManualHoldQueryService` returns active hold.
- action: **no automation action is applied**.

## 5.2 Cooldown
Cooldown prevents flapping.

Example:
- Fan turned ON 5 seconds ago.
- temperature fluctuates around the threshold.
- cooldown still active.
- action: **do not issue another command yet**.

## 5.3 Target reconciliation
Commands should only be sent if the desired target differs from the current state.

Example:
- Policy says fan should be ON.
- runtime state already says ON.
- action: **no write / no command**.

---

## 6. Suggested Event/Action Sequences

## 6.1 AUTO fan sequence
1. telemetry received
2. snapshot updated
3. mode = AUTO
4. temperature above AUTO high threshold
5. no manual hold
6. cooldown expired
7. target state = FAN ON
8. runtime state updated
9. dashboard/device command event emitted

## 6.2 MANUAL light sequence
1. user presses OFF
2. manual control endpoint called
3. command validated
4. runtime state updated to LIGHT OFF
5. manual hold started
6. response returned
7. later automation checks are blocked while hold is active

## 6.3 AWAY temperature sequence
1. telemetry received
2. mode = AWAY
3. temperature checked against away thresholds
4. if below away activation threshold, no fan action
5. if above away activation threshold and constraints pass, fan ON

## 6.4 SLEEP comfort sequence
1. telemetry received
2. mode = SLEEP
3. sleep thresholds loaded
4. fan evaluated using sleep band
5. light evaluated conservatively
6. allowed changes applied

---

## 7. Example Rule Matrix

| Mode   | Sensor Condition | Additional Condition | Expected Action |
|--------|------------------|----------------------|-----------------|
| AUTO   | Temperature >= auto high | No hold, no cooldown | Fan ON |
| AUTO   | Temperature <= auto low | Fan currently ON | Fan OFF |
| AUTO   | Light <= low threshold | Motion detected | Light ON |
| AUTO   | Light >= high threshold | Light currently ON | Light OFF |
| MANUAL | User requests ON | Capability valid | Target device ON |
| MANUAL | User requests OFF | Capability valid | Target device OFF |
| AWAY   | Temperature < away high | None | Fan remains OFF |
| AWAY   | Temperature >= away high | No hold, no cooldown | Fan ON |
| AWAY   | Motion detected | Depending on policy | Alert and/or light/security action |
| SLEEP  | Temperature >= sleep high | No hold, no cooldown | Fan ON |
| SLEEP  | Temperature <= sleep low | Fan currently ON | Fan OFF |
| SLEEP  | Dark environment | No special night-navigation rule | Light remains OFF |

---

## 8. Engineering Interpretation of Current Design

The current design appears to use a **policy-based automation architecture** rather than embedding all conditions directly in controllers. That is a good sign because it gives:

- separation of concerns
- easier testing of decision logic
- easier extension for new modes or device types
- better runtime-state traceability

A likely interaction pattern is:

- controller/facade receives request
- service resolves context
- policy computes decision
- state service persists outcome
- eventing/notification/dashboard layers react afterward

This is cleaner than mixing control decisions and persistence in one large method.

---

## 9. Gaps / Ambiguities Worth Verifying in the Actual Repository

When you scan the real `backend/src/main`, verify these points explicitly:

1. Whether `MANUAL` is a home-wide mode, a per-device override, or both.
2. Whether `AWAY` motion triggers alerts only or also turns on lights.
3. Whether `SLEEP` allows motion-based light activation.
4. Whether cooldown is global, per device, or per capability.
5. Whether manual hold duration is config-driven.
6. Whether `DeviceRuntimeStateService` stores both effective state and requested target.
7. Whether there is hysteresis through thresholds, time windows, or both.
8. Whether offline/sensor-error conditions force safe device states.

---

# 10. OpenAPI Draft

Below is a draft API contract that matches the inferred control model. It is written to be strong enough for prompting, backend alignment, or future documentation.

```yaml
openapi: 3.0.3
info:
  title: Smart Home Control and Mode API
  version: 0.1.0-draft
  description: |
    Draft API for smart-home mode management, manual control, runtime state inspection,
    and automation evaluation. This contract is based on the current backend architecture
    and should be aligned with actual controllers before implementation.

servers:
  - url: http://localhost:8080
    description: Local development server

paths:
  /api/homes/{homeId}/mode:
    get:
      summary: Get current home mode
      tags: [Mode]
      parameters:
        - name: homeId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: Current home mode returned
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HomeModeResponse'

    put:
      summary: Change current home mode
      tags: [Mode]
      parameters:
        - name: homeId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateHomeModeRequest'
      responses:
        '200':
          description: Home mode updated
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HomeModeResponse'

  /api/homes/{homeId}/automation/evaluate:
    post:
      summary: Force automation re-evaluation for a home
      tags: [Automation]
      parameters:
        - name: homeId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: Automation decision result
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AutomationEvaluationResponse'

  /api/devices/{deviceId}/control:
    post:
      summary: Apply manual device control
      tags: [Manual Control]
      parameters:
        - name: deviceId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ManualControlRequest'
      responses:
        '200':
          description: Manual control applied
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DeviceRuntimeStateResponse'

  /api/devices/{deviceId}/runtime-state:
    get:
      summary: Get current runtime state of a device
      tags: [Runtime State]
      parameters:
        - name: deviceId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: Runtime state returned
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DeviceRuntimeStateResponse'

  /api/devices/{deviceId}/manual-hold:
    get:
      summary: Check whether manual hold is active for a device
      tags: [Manual Hold]
      parameters:
        - name: deviceId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: Manual hold status returned
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ManualHoldStatusResponse'

  /api/homes/{homeId}/sensor-snapshot:
    get:
      summary: Get latest automation-relevant sensor values for a home
      tags: [Telemetry]
      parameters:
        - name: homeId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '200':
          description: Latest sensor snapshot returned
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SensorSnapshotResponse'

components:
  schemas:
    UpdateHomeModeRequest:
      type: object
      required: [mode]
      properties:
        mode:
          type: string
          enum: [AUTO, MANUAL, AWAY, SLEEP]
        reason:
          type: string
          example: User selected Away mode from mobile app

    HomeModeResponse:
      type: object
      properties:
        homeId:
          type: integer
          format: int64
        mode:
          type: string
          enum: [AUTO, MANUAL, AWAY, SLEEP]
        updatedAt:
          type: string
          format: date-time

    ManualControlRequest:
      type: object
      required: [capability, action]
      properties:
        capability:
          type: string
          example: POWER
        action:
          type: string
          example: ON
        value:
          nullable: true
          oneOf:
            - type: string
            - type: number
            - type: boolean
        source:
          type: string
          example: WEB_APP
        actorId:
          type: integer
          format: int64
          nullable: true
        holdMinutes:
          type: integer
          nullable: true
          description: Optional manual hold duration to prevent immediate automation override

    DeviceRuntimeStateResponse:
      type: object
      properties:
        deviceId:
          type: integer
          format: int64
        mode:
          type: string
          enum: [AUTO, MANUAL, AWAY, SLEEP]
        currentState:
          type: object
          additionalProperties: true
        targetState:
          type: object
          additionalProperties: true
        lastChangedAt:
          type: string
          format: date-time
        lastChangedBy:
          type: string
          example: AUTOMATION

    ManualHoldStatusResponse:
      type: object
      properties:
        deviceId:
          type: integer
          format: int64
        active:
          type: boolean
        expiresAt:
          type: string
          format: date-time
          nullable: true

    SensorSnapshotResponse:
      type: object
      properties:
        homeId:
          type: integer
          format: int64
        temperature:
          type: number
          format: double
          nullable: true
        humidity:
          type: number
          format: double
          nullable: true
        light:
          type: number
          format: double
          nullable: true
        motionDetected:
          type: boolean
          nullable: true
        capturedAt:
          type: string
          format: date-time

    AutomationEvaluationResponse:
      type: object
      properties:
        homeId:
          type: integer
          format: int64
        mode:
          type: string
          enum: [AUTO, MANUAL, AWAY, SLEEP]
        decisions:
          type: array
          items:
            $ref: '#/components/schemas/AutomationDecision'

    AutomationDecision:
      type: object
      properties:
        deviceId:
          type: integer
          format: int64
        capability:
          type: string
          example: POWER
        desiredAction:
          type: string
          example: ON
        reason:
          type: string
          example: temperature exceeded AUTO high threshold
        blockedByManualHold:
          type: boolean
        blockedByCooldown:
          type: boolean
        applied:
          type: boolean
```

---

## 11. Prompt-Ready Summary

Use this prompt when you want another model or teammate to continue from this document:

> Analyze the backend smart-home control flow under `backend/src/main` and validate the behavior of modes AUTO, MANUAL, AWAY, and SLEEP. Extract the real conditions, thresholds, service interactions, and action paths from the actual code. Confirm how `ModeAutomationServiceImpl`, `ManualControlService`, `ControlFacadeService`, `DeviceRuntimeStateService`, `ManualHoldQueryService`, `AutomationCooldownService`, `FanAutomationPolicy`, and `LightAutomationPolicy` interact. Then update the OpenAPI draft so it matches the real controllers and DTOs exactly.

---

## 12. Recommended Next Step

Once the actual `backend/src/main` folder is available, this draft should be upgraded into a repository-accurate version with:
- exact class and method references
- exact DTO names
- exact endpoint paths
- exact threshold property names
- exact manual-hold and cooldown behavior
- exact alert/security behavior in AWAY and SLEEP

