# Smart House API Docs And Design Patterns

## 1. Scope

This document summarizes:

- Backend APIs implemented under `backend/src/main/java/com/java/controller`
- Backend architectural/design patterns implemented under `backend/src/main`
- Frontend architectural/design patterns implemented under `frontend/src`

The content below is based on the current source code, not on assumed behavior.

## 2. Backend Overview

### Base URL

- Local backend URL: `http://localhost:8080`

### Success response format

Most controllers return the same wrapper from `com.java.config.ApiResponse`:

```json
{
  "success": true,
  "data": {},
  "message": "OK"
}
```

### Authentication

- JWT bearer authentication is configured in `SecurityConfig`
- Public endpoints include:
  - `/api/auth/**`
  - `/api/devices/home/**`
  - `/api/devices/*/state`
  - `/api/homes/*/configs`
  - `/api/homes/*/alerts`
  - `/api/device-telemetry`
  - `/api/v1/device/**`
  - `/api/control/**`
- Other endpoints require a bearer token in the `Authorization` header

## 3. Backend API Reference

### 3.1 Authentication

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Login with local account or Google | `LoginRequest` |
| `POST` | `/api/auth/refresh` | Refresh JWT access token | `RefreshTokenRequest` |

Notes:

- `LoginRequest` supports both `LOCAL` and `GOOGLE` providers
- Frontend currently sends `provider`, then either `username/password` or `authorizationCode/redirectUri`

### 3.2 Profile

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `GET` | `/api/profile/me` | Get current authenticated user profile | none |
| `PATCH` | `/api/profile/password` | Change current user password | `ChangePasswordRequest` |

### 3.3 Account Auth Providers

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `GET` | `/api/account/auth-providers` | Get linked auth providers for current user | none |
| `POST` | `/api/account/auth-providers/google/link` | Link Google auth for current user | `LinkCurrentGoogleAccountRequest` |

### 3.4 Admin Users

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `POST` | `/api/admin/users` | Create user by admin | `CreateUserRequest` |
| `GET` | `/api/admin/users` | Get all managed users | none |
| `GET` | `/api/admin/users/{userId}` | Get one user | none |
| `PATCH` | `/api/admin/users/{userId}` | Update one user | `AdminUpdateUserRequest` |
| `POST` | `/api/admin/users/{userId}/reset-password` | Reset password | none |
| `GET` | `/api/admin/users/{userId}/auth-providers` | Get linked providers of a user | none |
| `POST` | `/api/admin/users/{userId}/auth-providers` | Link provider for a user | `LinkUserAuthProviderRequest` |

### 3.5 Home Users

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `GET` | `/api/homes/{homeId}/users` | List users in a home | none |
| `POST` | `/api/homes/{homeId}/users` | Add user into a home | `AddHomeUserRequest` |
| `PATCH` | `/api/homes/{homeId}/users/{userId}` | Update home user role/info | `UpdateHomeUserRequest` |
| `DELETE` | `/api/homes/{homeId}/users/{userId}` | Remove user from home | none |
| `POST` | `/api/homes/{homeId}/users/{userId}/set-password` | Set password for home user | `SetHomeUserPasswordRequest` |

### 3.6 Home Profile

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `POST` | `/api/home-profiles/{homeId}/activate` | Activate home profile | none |

### 3.7 Devices

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `GET` | `/api/devices/home/{homeId}` | List devices in a home | none |
| `POST` | `/api/devices/home/{homeId}` | Create device | `DeviceCreateRequest` plus query param `userId` |
| `GET` | `/api/devices/{deviceId}` | Get device details | none |
| `GET` | `/api/devices/{deviceId}/state` | Get current runtime state | none |

### 3.8 Manual Control

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `POST` | `/api/control/devices/{deviceId}` | Manual device control | `ControlRequest` |

### 3.9 Configurations

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `GET` | `/api/homes/{homeId}/configs` | List configurations of a home | none |
| `GET` | `/api/homes/{homeId}/configs/active` | Get active configuration | none |
| `POST` | `/api/homes/{homeId}/configs` | Create configuration | `ConfigUpsertRequest` |
| `PUT` | `/api/homes/{homeId}/configs/{configId}` | Update configuration | `ConfigUpsertRequest` |
| `PUT` | `/api/homes/{homeId}/configs/{configId}/activate` | Activate configuration | none |
| `DELETE` | `/api/homes/{homeId}/configs/{configId}` | Delete configuration | none |

Important:

- Device/config mutation endpoints use `userId` query parameters in the backend service flow

### 3.10 Alerts

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `GET` | `/api/homes/{homeId}/alerts` | List alerts by home | none |
| `POST` | `/api/homes/{homeId}/alerts` | Open/create an alert | `AlertOpenRequest` |
| `POST` | `/api/homes/{homeId}/alerts/{alertId}/ack` | Acknowledge alert | `AlertActionRequest` |
| `POST` | `/api/homes/{homeId}/alerts/{alertId}/resolve` | Resolve alert | `AlertActionRequest` |

### 3.11 Dashboard

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `GET` | `/api/dashboard/homes/{homeId}` | Load dashboard summary | none |

### 3.12 Audit

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `GET` | `/api/audit/homes/{homeId}` | Query audit dashboard for a home | query params: `from`, `to`, paging/filter fields |

Supported query parameters from the controller/frontend:

- `from`
- `to`
- `configPage`
- `configSize`
- `configKeyword`
- `eventPage`
- `eventSize`
- `eventKeyword`
- `eventCategory`

### 3.13 Schedules

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `POST` | `/api/schedules` | Create or update a device schedule | `ScheduleUpsertRequest` |
| `GET` | `/api/schedules/devices/{deviceId}` | Get schedules by device | none |

### 3.14 Mode Schedules

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `GET` | `/api/homes/{homeId}/mode-schedules` | List home mode schedules | none |
| `POST` | `/api/homes/{homeId}/mode-schedules` | Create home mode schedule | `ModeScheduleUpsertRequest` |
| `PUT` | `/api/homes/{homeId}/mode-schedules/{id}` | Update home mode schedule | `ModeScheduleUpsertRequest` |
| `DELETE` | `/api/homes/{homeId}/mode-schedules/{id}` | Delete home mode schedule | none |

### 3.15 Device Command Polling API

These endpoints appear designed for device-side polling/execution:

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `GET` | `/api/v1/device/{deviceKey}/commands/next` | Get next pending command for device | none |
| `POST` | `/api/v1/device/{deviceKey}/commands/ack` | Acknowledge command execution result | `CommandAckRequest` |

### 3.16 Telemetry

| Method | Endpoint | Purpose | Request |
| --- | --- | --- | --- |
| `POST` | `/api/device-telemetry` | Ingest telemetry from device/simulator | `TelemetryIngestRequest` |
| `GET` | `/api/v1/device/{deviceKey}/telemetry` | Read telemetry history | query param `range` |

## 4. Backend Design Patterns and Architectural Practices

### 4.1 Layered Architecture

The backend is primarily organized as a layered Spring Boot application:

- `controller`: HTTP entry points, request parsing, validation, and response shaping
- `domain/service`: application use cases and orchestration
- `domain/provider`: pluggable business behaviors and rule-selection helpers
- `mapper`: conversion between entities, DTOs, and view models
- `persistence/entity` and `persistence/repo`: JPA persistence model and repositories
- `adapter`: integration boundary for external device protocols or vendor-specific behavior
- `scheduler` and `eventing`: background processing and event-driven runtime behavior

Why it helps:

- Separates transport, business, and persistence concerns
- Improves maintainability and testability
- Reduces coupling between controllers and data access code

---

### 4.2 Strategy Pattern

The clearest design pattern in the backend is Strategy.

Examples include:

- `AuthenticationStrategy` with `LocalAuthenticationStrategy` and `GoogleAuthenticationStrategy`
- `ScheduleExecutionStrategy`
- `DeviceRuntimeStateWriteStrategy`
- `ModeScheduleExecutionStrategy`
- Audit/event-related strategy-style components where multiple implementations share one contract

How it works:

- Each implementation follows a shared interface or contract
- Runtime selection determines the appropriate behavior for a given context
- New behavior can be added with minimal impact on existing orchestration code

This is one of the strongest and most clearly implemented patterns in the backend.

---

### 4.3 Strategy Resolution / Registry

Classes such as:

- `AuthenticationStrategyResolver`
- `ScheduleExecutionStrategyResolver`
- `DeviceRuntimeStateWriteStrategyResolver`

should be described as **strategy selectors/registries**, not as a separate design pattern.

In practice:

- Spring injects multiple implementations of one strategy contract
- The resolver selects the correct implementation, often through `supports(...)`
- Services depend on one resolver instead of many concrete strategies

So the main pattern remains **Strategy**, while resolver classes provide the Spring-friendly selection mechanism around it.

---

### 4.4 Adapter Pattern

External device communication is abstracted through `DeviceCommandAdapter`.

Purpose:

- Separate domain-level command execution from vendor/protocol-specific details
- Allow different device ecosystems to be integrated behind a stable contract

This is especially visible in:

- `adapter/DeviceCommandAdapter.java`
- `adapter/ohstem/...`

This is a valid and appropriate use of the Adapter pattern.

---

### 4.5 Factory Pattern

The backend uses factory classes to centralize object creation rules.

Examples:

- `DeviceEntityFactory`
- `ModeScheduleEntityFactory`
- `DeviceRuntimeStateFactory`

Benefits:

- Centralizes defaulting and creation logic
- Reduces duplicated construction code
- Makes object creation rules more consistent and easier to evolve

This classification is appropriate when those classes are responsible for constructing objects rather than only filling fields procedurally.

---

### 4.6 Mapper / Assembler Pattern

The `mapper` package and several `...Assembler` classes separate data transformation from business logic.

Examples:

- `DeviceMapper`
- `ScheduleMapper`
- `ModeScheduleMapper`
- `DeviceRuntimeViewAssembler`
- `HomeUserViewAssembler`

Benefits:

- Keeps DTO/view transformation out of controllers and core services
- Improves consistency of API response shaping
- Supports dedicated read models without exposing persistence entities directly

This is accurately described as mapper/assembler usage rather than a more formal GoF pattern.

---

### 4.7 Facade Pattern

`ControlFacadeService` is a valid example of the Facade pattern.

It provides a single simplified entry point for control operations through `control(...)`, while hiding the internal coordination among multiple subsystem services such as:

- `ManualControlService`
- `AutoControlService`
- `HomeAccessGuard`
- `DeviceRepository`

Why it fits the Facade pattern:

- clients interact with one service instead of coordinating multiple services directly
- the facade hides device loading, validation, home access checks, and flow selection
- manual and auto control paths are exposed through one unified API
- subsystem complexity remains behind a narrower and simpler interface

In this design, `ControlFacadeService` acts as the control use case boundary for callers, while the underlying services remain focused on their specialized responsibilities.
---

### 4.8 Observer Pattern

The project contains an in-process eventing mechanism:

- `DomainEvent`
- `DomainEventBus`
- `DomainEventListener`
- `SimpleDomainEventBus`

`EventBusWiringConfig` registers listeners at startup.

This is best described as **Observer-style eventing**:

- publishers emit events
- listeners react without tight coupling
- new listeners can be added with low impact on publishers

This captures the intent accurately without overstating the exact implementation.

---

### 4.9 Transactional Outbox Pattern

The project also appears to use a Transactional Outbox approach:

- `UserEventOutboxService` stores pending integration events in the database
- `OutboxEventPublisher` periodically publishes pending rows to RabbitMQ

Why this matters:

- Improves reliability when domain changes and message publication must remain consistent
- Reduces the chance of losing integration events if RabbitMQ is temporarily unavailable
- Supports retry and publish-status tracking

This is a strong architectural pattern for reliable event-driven integration.

---

### 4.10 Scheduled Jobs / Time-Based Automation

The backend uses scheduled jobs for runtime automation.

Examples:

- `DeviceScheduleScheduler`
- `ManualHoldScheduler`
- `OfflineScheduler`
- `OutboxEventPublisher` using `@Scheduled`

Purpose:

- Apply schedules periodically
- Detect offline devices
- Publish queued integration events

This should be described as **scheduled background processing** or **time-based automation**, rather than as a formal design pattern.

---

### 4.11 Business Rule Encapsulation

Several provider classes encapsulate domain-specific rules.

Examples:

- `ScheduleSelectionPolicy`
- `HomeUserRoleChangePolicy`
- `HomeUserPrimaryEligibilityPolicy`
- `AuditEventVisibilityPolicy`
- `ControllerTargetSupportPolicy`

These classes help by:

- isolating business rules from controllers and repositories
- making rules easier to test independently
- reducing duplication of decision logic across services

It is more precise to describe this as **business rule encapsulation**. In some cases, if multiple interchangeable implementations exist behind a shared contract, they may also be considered a specialized use of Strategy.

## 5. Frontend Design Patterns and Structural Practices

### 5.1 Modular Frontend Structure by Responsibility

The frontend `src` folder is organized mostly by technical responsibility:

- `api`: backend communication layer
- `components`: reusable UI pieces
- `pages`: route-level screens
- `providers`: application-wide state providers
- `router`: route definitions
- `hooks`: reusable stateful logic
- `utils`: helpers and storage utilities

This is better described as a **modular React structure organized by responsibility** than as a strictly feature-oriented folder structure.

---

### 5.2 API Service Layer

The frontend uses a thin service layer in `frontend/src/api`.

Examples:

- `authApi.js`
- `dashboardApi.js`
- `configApi.js`
- `deviceApi.js`
- `homeUserApi.js`

Why it helps:

- Components do not call `fetch` directly
- Endpoint details stay centralized
- Request/response shaping is reusable

`apiClient.js` acts as the shared low-level client abstraction for:

- base URL handling
- query-string building
- bearer token injection
- automatic token refresh on `401`
- normalized API errors via `ApiError`

This is accurately described as an API service layer or client abstraction.

---

### 5.3 Provider Pattern With React Context

`AuthProvider.jsx` uses React Context to provide shared authentication state and behavior.

It centralizes:

- current user
- access token
- bootstrap/refresh logic
- login/logout methods

Benefits:

- avoids prop drilling
- gives protected pages a shared auth source
- keeps auth lifecycle logic in one place

This is a standard and valid use of the Provider pattern in React.

---

### 5.4 Route Guard Pattern

Routing uses protective wrapper components such as:

- `ProtectedRoute`
- `RequireHomeRoute`
- `RoleProtectedRoute`
- `RouteErrorBoundary`

This is appropriately described as a **route guard pattern**:

- unauthenticated users are redirected to login
- unauthorized roles are redirected to `403`
- access control logic is reused across routes

---

### 5.5 Custom Hook Pattern

Reusable UI state logic is extracted into hooks such as:

- `useConfirmDialog`

This keeps components smaller by moving reusable stateful behavior into dedicated hooks.

This is a standard React custom hook pattern.

---

### 5.6 Loose Container / Presentational Separation

The frontend loosely separates page-level orchestration from reusable display components:

- `pages/*` handle data flow and screen composition
- `components/*` handle reusable UI pieces such as cards, tables, filters, dialogs, and layout blocks

This is not a strict container/presentational architecture, but the separation intent is clearly present.

---

### 5.7 Layout Composition

`AppRouter.jsx` composes the application through nested wrappers such as:

- `ProtectedRoute`
- `AppLayout`
- `RequireHomeRoute`
- `RoleProtectedRoute`

This creates a clean route tree where cross-cutting concerns are layered around page content instead of repeated inside each page.