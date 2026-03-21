# Smart Home Schema Documentation

## 1. Overview

This document describes the core entities in the Smart Home database schema, the relationships between them, and the planned device configuration for the current implementation.

The schema is designed around five main concerns:

- identity and access management
- home ownership and membership
- device and sensor modeling
- automation, control, and monitoring
- operational audit, alerts, and event delivery

The design is flexible enough to support different device classes, reusable configuration profiles, runtime state tracking, schedules, command processing, and historical observability.

---

## 2. Domain Scope

The system manages one or more smart homes. Each home can have multiple users, devices, sensors, configurations, schedules, alerts, and audit records.

At a high level, the platform supports:

- user authentication and authorization
- home ownership and multi-user collaboration
- controller, actuator, and sensor-node registration
- capability-based device modeling
- live runtime state management
- telemetry ingestion from sensors
- automatic and manual control commands
- alerting and activity tracking
- event outbox for reliable integration

---

## 3. Core Entity Groups

## 3.1 Identity and Access

### `users`
Represents a system account.

**Purpose**
- store login identity
- define system-wide role and status
- support both local and external authentication
- track lifecycle metadata such as creation, updates, invitation, and login time

**Important fields**
- `id`
- `username`
- `password_hash`
- `role` (`system_user_role`)
- `status` (`user_status`)
- `must_change_password`
- `invited_at`
- `last_login`

### `user_auth_providers`
Represents authentication methods linked to a user.

**Purpose**
- allow one account to authenticate via multiple providers
- support local login and Google login
- track linked identity from an external provider

**Relationship**
- many auth providers belong to one user

### `user_refresh_tokens`
Stores refresh tokens issued to users.

**Purpose**
- support session renewal
- track revocation and expiration
- record client metadata for security auditing

**Relationship**
- many refresh tokens belong to one user

---

## 3.2 Home Management

### `homes`
Represents a smart home managed by the system.

**Purpose**
- define a logical home boundary
- attach address and ownership
- scope devices, configs, alerts, and logs

**Important fields**
- `id`
- `name`
- `address`
- `owner_user_id`

### `home_users`
Represents membership of users in homes.

**Purpose**
- assign users to homes
- define home-level roles such as `OWNER`, `CO_OWNER`, `RESIDENT`, `TECHNICIAN`, `VIEWER`
- identify whether a home is a primary home for a user
- control whether a member may activate configuration profiles

**Relationship**
- many users can belong to many homes
- implemented as a junction table between `users` and `homes`

---

## 3.3 Device Model

### `devices`
Represents a physical or logical device installed in a home.

**Purpose**
- register all controllers, actuators, sensor nodes, hubs, and other device types
- identify device class and subtype
- track installation, room placement, connectivity, and firmware

**Important fields**
- `id`
- `home_id`
- `device_key`
- `name`
- `class` (`device_class`)
- `subtype`
- `room_name`
- `status`
- `last_seen`
- `is_online`
- `installed_by`
- `firmware_version`

**Examples in the current seed**
- living room controller
- fan actuator
- light actuator
- temperature sensor node
- humidity sensor node
- light sensor node
- motion sensor node

### `device_capabilities`
Represents what a device can do or expose.

**Purpose**
- model capabilities such as `POWER`, `SPEED`, `BRIGHTNESS`, `MODE`, `TEMPERATURE`, `HUMIDITY`, `MOTION`
- define whether a capability is writable
- define capability type and numeric range

**Important fields**
- `device_id`
- `capability_code`
- `value_type`
- `is_writable`
- `min_value`
- `max_value`
- `unit`

**Examples**
- fan actuator: `POWER`, `SPEED`
- light actuator: `POWER`, `BRIGHTNESS`
- controller: `MODE`
- sensor nodes: telemetry-related capabilities such as `TEMPERATURE`, `HUMIDITY`, `BRIGHTNESS`, `MOTION`

### `device_runtime_state`
Represents the latest effective value of each device capability.

**Purpose**
- store the current actual state for a capability
- support state-driven dashboard rendering
- unify boolean, numeric, and text-based states

**Examples**
- fan `POWER = false`
- fan `SPEED = 0`
- light `BRIGHTNESS = 0`
- controller `MODE = auto`

### `device_state_history`
Stores historical state changes for device capabilities.

**Purpose**
- provide a full timeline of state transitions
- preserve change source such as `COMMAND`, `DEVICE_ACK`, `AUTOMATION`, `SCHEDULE`, `SYSTEM`
- support audit, debugging, and analytics

### `device_configs`
Maps a device to the currently selected configuration profile.

**Purpose**
- indicate which config a device is using
- record who changed the config and why

**Constraint**
- device and config must belong to the same home

---

## 3.4 Sensor and Telemetry Model

### `sensors`
Represents a sensor attached to a sensor-node device.

**Purpose**
- define a measurable channel such as temperature, humidity, light, or motion
- provide metadata such as unit and operational status
- track the sensor's last seen time

**Constraint**
- sensors may only belong to devices whose class is `SENSOR_NODE`

### `sensor_data`
Stores the telemetry history produced by sensors.

**Purpose**
- persist numeric, text, or boolean readings
- support charts, monitoring, automation input, and alert analysis

**Examples**
- temperature reading in °C
- humidity reading in %
- light reading in %
- motion reading as boolean

---

## 3.5 Automation and Control

### `configs`
Represents a reusable automation and monitoring profile for a home.

**Purpose**
- centralize temperature, light, and mode thresholds
- define automation timing windows
- configure fan-speed preferences for different modes
- define which devices are monitored for temperature, humidity, light, and motion
- allow one active config per home

**Important fields**
- thresholds: `thigh`, `tlow`, `lhigh`, `llow`
- mode thresholds: `tsleep_high`, `tsleep_low`, `taway_high`, `tcritical`
- timing: `n_minutes`, `m_minutes`, `thold_minutes`, `dpresent`, `k_minutes`
- fan preferences: `auto_fan_speed`, `sleep_fan_speed`, `away_fan_speed`
- activation: `is_active`
- monitoring targets:
  - `monitoring_temperature_device_id`
  - `monitoring_humidity_device_id`
  - `monitoring_light_device_id`
  - `monitoring_motion_device_id`

### `schedules`
Represents planned value application for a capability.

**Purpose**
- apply recurring values to device capabilities
- support daily or weekly automation
- keep schedules separate from runtime state

**Examples in the current seed**
- turn fan power on at 18:00
- set fan speed to 60 at 18:05
- turn light on at 18:00
- set light brightness to 70 at 18:01
- switch controller mode to `sleep` at 22:00

### `control_commands`
Represents manual or system-generated commands sent to devices.

**Purpose**
- record intent to change a device capability
- track command delivery lifecycle from `PENDING` to `ACKED` or `FAILED`
- preserve actor information

**Examples**
- set fan `POWER = true`
- set fan `SPEED = 60`
- set controller `MODE = manual`

---

## 3.6 Monitoring, Alerts, and Auditing

### `alerts`
Represents an active or historical alert.

**Purpose**
- track issues and threshold violations
- support acknowledgement and resolution
- link an alert to a home, and optionally to a device and sensor

**Supported alert categories**
- `CRITICAL_TEMP`
- `DEVICE_OFFLINE`
- `SENSOR_ERROR`
- `HIGH_HUMIDITY`
- `LOW_HUMIDITY`
- `LOW_LIGHT`
- `HIGH_LIGHT`
- `MOTION_DETECTED`
- `WRONG_PASSWORD`
- `HIGH_TEMPERATURE`
- `LOW_TEMPERATURE`
- `CUSTOM`

### `activity_logs`
Stores user and system actions.

**Purpose**
- record operational changes and important events
- support traceability and auditing
- preserve structured change details using JSON

**Examples**
- initial device registration
- manual control actions
- config changes
- automation actions

### `outbox_event`
Stores integration events for reliable asynchronous publishing.

**Purpose**
- implement the outbox pattern
- decouple database transactions from external event delivery
- track retry count, error state, and publication timestamp

---

## 4. Entity Relationships

## 4.1 Main Relationships

### User and Home
- one `users` record can own many `homes`
- one `homes` record optionally references one owner user
- many users can belong to many homes through `home_users`

### Home and Device
- one home has many devices
- each device belongs to exactly one home

### Device and Capability
- one device has many capabilities
- each capability belongs to exactly one device

### Device and Runtime State
- one capability can have one current runtime state entry
- runtime state is keyed by `(device_id, capability_code)`

### Device and Sensor
- one sensor-node device can have many sensors
- each sensor belongs to exactly one device

### Sensor and Sensor Data
- one sensor has many telemetry records
- each telemetry record belongs to exactly one sensor

### Home and Config
- one home can have many configuration profiles
- at most one config is active per home

### Device and Config
- each device may reference one active config through `device_configs`
- a config can be used by many devices in the same home

### Device and Schedule
- one device can have many schedules
- each schedule targets exactly one capability of one device

### Device and Control Command
- one device can receive many control commands
- each command targets one device capability

### Home / Device / Sensor and Alert
- one home can have many alerts
- a device can have many alerts
- a sensor can have many alerts
- every alert belongs to exactly one home

### Home / Device / User and Activity Log
- one activity log may reference a home, device, and user
- logs provide historical traceability across many entity types

---

## 4.2 Relationship Summary Diagram

```text
users
 ├──< homes.owner_user_id
 ├──< home_users >── homes
 ├──< user_auth_providers
 ├──< user_refresh_tokens
 ├──< control_commands.actor_id
 ├──< activity_logs.user_id
 └──< alerts.acknowledged_by / resolved_by

homes
 ├──< devices
 ├──< configs
 ├──< alerts
 └──< activity_logs

devices
 ├──< device_capabilities
 ├──< device_runtime_state
 ├──< device_state_history
 ├──< sensors
 ├──< schedules
 ├──< control_commands
 ├──< alerts
 ├──< activity_logs
 └──< device_configs >── configs

sensors
 ├──< sensor_data
 └──< alerts
```

---

## 5. Data Integrity and Validation Rules

The schema includes strong validation through constraints, indexes, and triggers.

### Key validation rules
- usernames and key text fields cannot be blank
- device keys are globally unique
- device names are unique within a home
- sensor kind is unique within a device
- runtime state, schedule, sensor data, and control command must contain exactly one value type
- sensor records can only be attached to `SENSOR_NODE` devices
- config and device in `device_configs` must belong to the same home
- alert home reference must match the referenced device or sensor home
- numeric capability values must respect configured min/max ranges
- only one active config is allowed per home
- only one owner role is allowed per home
- update timestamps are automatically refreshed by triggers

These rules make the schema safer for automation logic and reduce application-level inconsistency.

---

## 6. Planned Device Configuration

This section describes the intended device layout reflected by the seed data and implied by the schema.

## 6.1 Deployment Goal

The current target deployment is a **single-room smart home demonstration**, centered on a **living room** setup that can later be extended to additional rooms and homes.

## 6.2 Planned Physical / Logical Devices

### 1. Central Controller
**Device**
- `Living Room Controller`
- `device_key = yolobit-01`
- class: `CONTROLLER`
- subtype: `SMART_CONTROLLER`

**Responsibilities**
- maintain current system mode
- act as the main local coordination unit
- receive automation decisions or user-triggered changes
- switch between `auto`, `manual`, `sleep`, and `away`

**Capability**
- `MODE`

### 2. Fan Actuator
**Device**
- `OhStem Fan Control`
- `device_key = ohstem-fan-ctrl-01`
- class: `ACTUATOR`
- subtype: `FAN`

**Responsibilities**
- turn fan on or off
- adjust speed according to automation profile or manual command

**Capabilities**
- `POWER` (BOOLEAN)
- `SPEED` (NUMBER, 0–100%)

### 3. Light Actuator
**Device**
- `OhStem Light Control`
- `device_key = ohstem-light-ctrl-01`
- class: `ACTUATOR`
- subtype: `LIGHT`

**Responsibilities**
- turn light on or off
- set light brightness according to ambient-light conditions or manual scheduling

**Capabilities**
- `POWER` (BOOLEAN)
- `BRIGHTNESS` (NUMBER, 0–100%)

### 4. Temperature Sensor Node
**Device**
- `OhStem Temperature Sender`
- `device_key = ohstem-temp-01`
- class: `SENSOR_NODE`
- subtype: `TEMPERATURE_NODE`

**Responsibilities**
- publish ambient temperature readings
- support temperature-based fan automation and over-temperature alerting

**Sensor**
- `TEMPERATURE`

### 5. Humidity Sensor Node
**Device**
- `OhStem Humidity Sender`
- `device_key = ohstem-humidity-01`
- class: `SENSOR_NODE`
- subtype: `HUMIDITY_NODE`

**Responsibilities**
- publish humidity readings
- support humidity alerts and future automation extension

**Sensor**
- `HUMIDITY`

### 6. Light Sensor Node
**Device**
- `OhStem Light Sender`
- `device_key = ohstem-light-01`
- class: `SENSOR_NODE`
- subtype: `LIGHT_NODE`

**Responsibilities**
- publish ambient light readings
- support automatic light control and low/high light alerts

**Sensor**
- `LIGHT`

### 7. Motion Sensor Node
**Device**
- `OhStem Motion Sender`
- `device_key = ohstem-motion-01`
- class: `SENSOR_NODE`
- subtype: `MOTION_NODE`

**Responsibilities**
- publish motion state
- support occupancy detection, away/sleep logic, and motion alerts

**Sensor**
- `MOTION`

---

## 6.3 Planned Control Logic

The intended control model is:

- **temperature** influences fan power and speed
- **ambient light** influences light power and brightness
- **motion** influences occupancy-related behavior and mode transitions
- **controller mode** determines which threshold set is active
- **schedules** can override or predefine expected behavior at fixed times
- **manual commands** can directly modify device capability values
- **alerts** are raised when thresholds, connectivity, or security conditions are violated

### Mode intent
- `auto`: normal automatic control
- `manual`: direct user control takes priority
- `sleep`: quieter and less intrusive behavior with different thresholds
- `away`: energy-saving and occupancy-aware behavior

---

## 6.4 Planned Monitoring Profile

The active home configuration is intended to define:

- temperature upper and lower thresholds
- light upper and lower thresholds
- sleep-mode temperature thresholds
- away-mode temperature thresholds
- critical temperature threshold
- delay windows before automation or alerts are triggered
- preferred fan speeds for each operating mode
- which devices serve as the authoritative source for:
  - temperature
  - humidity
  - light
  - motion

This design allows the platform to activate different automation profiles without changing device structure.

---

## 6.5 Example Seed Configuration Intent

The provided seed values suggest the following operational plan:

- normal temperature control around:
  - high threshold: `30.0°C`
  - low threshold: `27.0°C`
- light automation around:
  - high light threshold: `55`
  - low light threshold: `35`
- sleep-mode temperature control:
  - high: `32.0°C`
  - low: `26.0°C`
- away-mode high threshold:
  - `33.0°C`
- critical temperature threshold:
  - `35.0°C`
- delay / holding windows:
  - `n_minutes = 2`
  - `m_minutes = 2`
  - `thold_minutes = 5`
  - `dpresent = 20`
  - `k_minutes = 5`

Scheduled examples:
- 18:00 turn fan on
- 18:05 set fan speed to 60
- 18:00 turn light on
- 18:01 set brightness to 70
- 22:00 switch system mode to `sleep`

---

## 7. Architectural Notes

## 7.1 Why capability-based modeling is important
Instead of hard-coding device state into fixed columns, the schema models device behavior through `device_capabilities`, `device_runtime_state`, `schedules`, `control_commands`, and `device_state_history`.

This approach makes the design:
- extensible for new device types
- easier to automate consistently
- better suited for multi-vendor integration
- more maintainable than a fixed per-device schema

## 7.2 Why sensor and actuator concerns are separated
The schema clearly distinguishes:
- sensor data ingestion
- actuator state control
- controller mode management

That separation is useful because telemetry, command delivery, state acknowledgment, and automation policies often evolve independently.

## 7.3 Why configs are reusable
A home can store multiple named configuration profiles and activate one at a time. This enables:
- testing different automation strategies
- custom profiles for day/night/seasonal behavior
- future support for user-selectable presets

---

## 8. Recommended Documentation Interpretation

For implementation and presentation purposes, the schema can be understood through this layered model:

1. **Access Layer**  
   Users, login providers, refresh tokens, home membership.

2. **Topology Layer**  
   Homes, devices, sensors, room placement, installer tracking.

3. **Capability Layer**  
   Device capabilities, runtime state, state history.

4. **Telemetry Layer**  
   Sensors and sensor data.

5. **Automation Layer**  
   Configs, schedules, monitored devices, system modes.

6. **Execution Layer**  
   Control commands and acknowledgments.

7. **Observability Layer**  
   Alerts, activity logs, outbox events.

This layered explanation is suitable for technical documentation, architecture reviews, and project reports.

---

## 9. Conclusion

The schema represents a solid smart-home domain model with a flexible capability-driven device architecture.

It supports:
- multi-user home management
- controller, actuator, and sensor-node coordination
- reusable automation profiles
- schedule-driven and manual control
- runtime state and full state history
- telemetry persistence and monitoring
- alert lifecycle management
- auditable operational events
- reliable outbound event publishing

The seeded configuration already outlines a practical living-room deployment using a controller, fan actuator, light actuator, and four sensor nodes for temperature, humidity, light, and motion. This is a strong base for both a working prototype and future expansion into a more complete smart-home platform.
