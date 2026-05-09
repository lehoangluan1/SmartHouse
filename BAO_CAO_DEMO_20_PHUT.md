# Báo cáo và kịch bản demo trực tiếp

## 1. Mục tiêu của ứng dụng

Ứng dụng `Smart House` quản lý một ngôi nhà thông minh mẫu, trong đó người dùng theo dõi cảm biến, điều khiển thiết bị, cấu hình ngưỡng tự động, xem lịch sử dữ liệu và tra cứu nhật ký vận hành.

Đối tượng sử dụng chính là người quản lý nhà, thành viên được cấp quyền trong nhà và người quản trị hệ thống. Phần giao diện có các màn hình đăng nhập, bảng điều khiển, cấu hình, lịch sử, cài đặt và nhật ký hệ thống trong `frontend/src/router/AppRouter.jsx`.

Giá trị chính của ứng dụng là gom các luồng theo dõi môi trường, điều khiển quạt/đèn, cấu hình tự động hóa, cảnh báo và kiểm tra lịch sử vào một hệ thống thống nhất. Mã nguồn thể hiện rõ các luồng này qua `DashboardPage.jsx`, `ConfigPage.jsx`, `HistoryPage.jsx`, `SettingsPage.jsx`, `AuditLogsPage.jsx` và các điểm xử lý trong `backend/src/main/java/com/java/controller`.

## 2. Tổng quan mô hình hệ thống

Hệ thống gồm các thành phần chính sau:

| Thành phần | Vai trò | Bằng chứng trong kho mã |
| --- | --- | --- |
| Phần giao diện React | Hiển thị bảng điều khiển, cấu hình, lịch sử, cài đặt, nhật ký và đăng nhập | `frontend/src/router/AppRouter.jsx`, `frontend/package.json` |
| Phần máy chủ Spring Boot | Cung cấp `RESTful API`, xử lý đăng nhập, thiết bị, cấu hình, điều khiển, cảnh báo, lịch sử và nhật ký | `backend/src/main/java/com/java/controller` |
| Cơ sở dữ liệu PostgreSQL | Lưu người dùng, nhà, thiết bị, cảm biến, cấu hình, trạng thái, lịch sử, lệnh điều khiển, cảnh báo và nhật ký | `backend/db/init.sql`, `docs/smart_home_schema_documentation.md` |
| RabbitMQ | Nhận sự kiện người dùng từ bảng `outbox_event` thông qua `OutboxEventPublisher` | `docker-compose.yml`, `OutboxEventPublisher.java` |
| Gateway Python | Là lớp trung gian cho thiết bị gọi tới phần máy chủ và dịch vụ nhận diện hình ảnh | `gateway.py` |
| Mã thiết bị OhStem/YoloBit | Đọc cảm biến, cập nhật LCD, xử lý hồng ngoại, điều khiển cửa, quạt, đèn và gửi dữ liệu | `aiot_code_v3.py` |
| Dịch vụ camera YOLO | Nhận diện người và chuyển động qua camera, cung cấp `/check_human` và `/health` | `camera_interface.py` |

Luồng tương tác tổng quát:

1. Người dùng đăng nhập ở phần giao diện qua `LoginPage.jsx`.
2. Phần giao diện gọi `apiClient.js`, gắn `Bearer token` khi yêu cầu cần xác thực.
3. Phần máy chủ nhận yêu cầu qua các lớp điều khiển như `DashboardController`, `ControlController`, `ConfigController`, `AuditQueryController`.
4. Lớp xử lý trong `domain/service` thực hiện nghiệp vụ và dùng các `Repository` trong `persistence/repo` để đọc ghi PostgreSQL.
5. Thiết bị hoặc mô phỏng thiết bị gửi dữ liệu cảm biến tới `gateway.py`, sau đó gateway chuyển tiếp tới `/api/device-telemetry`.
6. Bảng điều khiển nhận dữ liệu tổng hợp qua `/api/dashboard/homes/{homeId}` và nhận cập nhật trực tiếp qua `/api/dashboard/homes/{homeId}/stream`.

## 3. Thiết kế ứng dụng

Phần giao diện được chia theo trách nhiệm:

- `frontend/src/pages`: chứa các màn hình chính như `DashboardPage`, `ConfigPage`, `HistoryPage`, `SettingsPage`, `AuditLogsPage`, `LoginPage`.
- `frontend/src/components`: chứa các khối giao diện tái sử dụng như `MonitoringCard`, `DeviceSwitchCard`, `ConfigDetailPanel`, `ScheduleTable`, `EventHistoryTable`.
- `frontend/src/api`: tập trung các lời gọi `API` như `dashboardApi.js`, `configApi.js`, `historyApi.js`, `settingsApi.js`, `authApi.js`.
- `frontend/src/providers/AuthProvider.jsx`: quản lý phiên đăng nhập, người dùng hiện tại, `accessToken` và `refreshToken`.
- `frontend/src/router/AppRouter.jsx`: định nghĩa tuyến màn hình và các lớp bảo vệ tuyến như `ProtectedRoute`, `RequireHomeRoute`, `RoleProtectedRoute`.

Các màn hình chính:

| Màn hình | Chức năng |
| --- | --- |
| `LoginPage` | Đăng nhập bằng tài khoản cục bộ hoặc Google. |
| `DashboardPage` | Xem dữ liệu giám sát, điều khiển thiết bị, đổi chế độ hệ thống, nhận trạng thái trực tiếp. |
| `ConfigPage` | Tạo, sửa, xóa, kích hoạt cấu hình; gán thiết bị giám sát cho nhiệt độ, độ ẩm, ánh sáng, chuyển động, quạt và đèn. |
| `HistoryPage` | Xem biểu đồ lịch sử theo khoảng thời gian và loại thiết bị. |
| `SettingsPage` | Quản lý lịch chế độ nhà và quyền thành viên trong nhà. |
| `AuditLogsPage` | Xem thay đổi cấu hình, sự kiện thiết bị, cảnh báo và sự kiện hệ thống. |

Luồng dữ liệu khi điều khiển thiết bị:

1. Người dùng thao tác trên `DeviceSwitchCard` hoặc `SegmentControl` trong `DashboardPage`.
2. `useDashboardController.js` gọi `controlDevice` trong `dashboardApi.js`.
3. `dashboardApi.js` gửi yêu cầu tới `/api/control/devices/{deviceId}`.
4. `ControlController` gọi `ControlFacadeService`.
5. `ControlFacadeService` chuyển luồng sang `ManualControlService`.
6. `ManualControlService` chuẩn hóa yêu cầu, cập nhật chế độ thủ công khi cần và gọi `DeviceCommandExecutionService`.
7. Lệnh được ghi vào bảng `control_commands`, trạng thái thiết bị được ghi vào `device_runtime_state` và lịch sử được lưu trong `device_state_history`.
8. Thiết bị nhận lệnh qua điểm xử lý `/api/v1/device/{deviceKey}/commands/next` hoặc qua gateway `/gw/commands/next`.

Luồng dữ liệu khi thiết bị gửi cảm biến:

1. `aiot_code_v3.py` đọc nhiệt độ, độ ẩm, ánh sáng và chuyển động.
2. Mã thiết bị gửi từng giá trị tới `/gw/device-telemetry`.
3. `gateway.py` kiểm tra loại cảm biến trong `ALLOWED_SENSOR_TYPES` rồi chuyển tiếp tới `/api/device-telemetry`.
4. `TelemetryController` gọi `TelemetryIngestService`.
5. `TelemetryPersistenceService` lưu bản ghi vào `sensor_data`, cập nhật `device_runtime_state`, cập nhật `last_seen` của thiết bị và cảm biến.
6. `TelemetryIngestService` phát sự kiện qua `DomainEventBus`, từ đó các listener cập nhật bảng điều khiển, cảnh báo và nhật ký.

## 4. Cơ sở dữ liệu

Cơ sở dữ liệu được mô tả trong `backend/db/init.sql` và `docs/smart_home_schema_documentation.md`. Các bảng chính:

| Bảng | Ý nghĩa | Trường quan trọng |
| --- | --- | --- |
| `users` | Tài khoản hệ thống | `username`, `password_hash`, `role`, `status`, `must_change_password`, `last_login` |
| `user_auth_providers` | Phương thức đăng nhập gắn với tài khoản | `user_id`, `provider`, `provider_user_id`, `provider_email` |
| `user_refresh_tokens` | Phiên làm mới đăng nhập | `user_id`, `token_hash`, `issued_at`, `expires_at`, `revoked_at` |
| `homes` | Nhà thông minh | `name`, `address`, `owner_user_id` |
| `home_users` | Thành viên trong nhà | `home_id`, `user_id`, `role_in_home`, `allow_profile_activation`, `is_primary` |
| `devices` | Thiết bị vật lý hoặc logic | `home_id`, `device_key`, `name`, `class`, `subtype`, `room_name`, `is_online`, `last_seen` |
| `device_capabilities` | Khả năng của thiết bị | `device_id`, `capability_code`, `value_type`, `is_writable`, `min_value`, `max_value`, `unit` |
| `device_runtime_state` | Trạng thái hiện tại của từng khả năng | `device_id`, `capability_code`, `value_boolean`, `value_number`, `value_text` |
| `device_state_history` | Lịch sử thay đổi trạng thái | `device_id`, `capability_code`, `source`, `source_ref_id`, `changed_by`, `created_at` |
| `sensors` | Cảm biến gắn với thiết bị cảm biến | `device_id`, `name`, `sensor_kind`, `unit`, `last_seen` |
| `sensor_data` | Dữ liệu cảm biến theo thời gian | `sensor_id`, `value_numeric`, `value_text`, `value_boolean`, `created_at` |
| `configs` | Cấu hình tự động hóa của nhà | `thigh`, `tlow`, `lhigh`, `llow`, `tcritical`, `auto_fan_speed`, `is_active` |
| `schedules` | Lịch áp dụng giá trị cho thiết bị | `device_id`, `capability_code`, `start_time`, `end_time`, `days_mask`, `enabled` |
| `control_commands` | Lệnh điều khiển gửi tới thiết bị | `device_id`, `target`, `value_boolean`, `value_number`, `value_text`, `status`, `sent_at`, `ack_at` |
| `alerts` | Cảnh báo | `home_id`, `device_id`, `sensor_id`, `type`, `message`, `status` |
| `activity_logs` | Nhật ký hành động và sự kiện | `home_id`, `device_id`, `user_id`, `action`, `method`, `old_value`, `new_value`, `detail` |
| `outbox_event` | Hàng đợi sự kiện để gửi ra RabbitMQ | `aggregate_type`, `aggregate_id`, `event_type`, `payload`, `status`, `retry_count` |

Quan hệ dữ liệu chính:

- `homes` có nhiều `devices`, `configs`, `alerts`, `activity_logs`.
- `home_users` nối `users` với `homes`.
- `devices` có nhiều `device_capabilities`, `device_runtime_state`, `device_state_history`, `sensors`, `schedules`, `control_commands`.
- `sensors` có nhiều `sensor_data`.
- `configs` liên kết tới các thiết bị giám sát qua các cột `monitoring_temperature_device_id`, `monitoring_humidity_device_id`, `monitoring_light_sensor_device_id`, `monitoring_motion_device_id`, `monitoring_fan_device_id`, `monitoring_light_device_id`.

Dữ liệu mẫu dùng cho demo trong `backend/db/init.sql`:

- Nhà: `Nhà thông minh mẫu`.
- Thiết bị điều khiển trung tâm: `Living Room Controller`, `device_key = yolobit-01`.
- Thiết bị quạt: `OhStem Fan Control`, `device_key = ohstem-fan-ctrl-01`.
- Thiết bị đèn: `OhStem Light Control`, `device_key = ohstem-light-ctrl-01`.
- Cảm biến nhiệt độ: `OhStem Temperature Sender`, `device_key = ohstem-temp-01`.
- Cảm biến độ ẩm: `OhStem Humidity Sender`, `device_key = ohstem-humidity-01`.
- Cảm biến ánh sáng: `OhStem Light Sender`, `device_key = ohstem-light-01`.
- Cảm biến chuyển động: `OhStem Motion Sender`, `device_key = ohstem-motion-01`.
- Cấu hình mẫu: `My Smart House Config`, có các ngưỡng nhiệt độ, ánh sáng, tốc độ quạt và thiết bị giám sát.

## 5. Design pattern, SOLID, OOP và cách tổ chức mã nguồn

### Kiến trúc phân lớp

Phần máy chủ được tổ chức theo lớp:

- `controller`: nhận yêu cầu HTTP và trả phản hồi, ví dụ `DashboardController`, `ControlController`, `ConfigController`.
- `domain/service`: xử lý nghiệp vụ, ví dụ `ControlFacadeService`, `ManualControlService`, `TelemetryIngestService`.
- `domain/provider`: chứa các chính sách và chiến lược xử lý, ví dụ `AuthenticationStrategyResolver`, `ScheduleExecutionStrategyResolver`.
- `mapper`: chuyển đổi dữ liệu giữa thực thể và phản hồi, ví dụ `ControlCommandMapper`, `DeviceMapper`.
- `persistence/entity` và `persistence/repo`: mô hình dữ liệu và truy vấn.

Lợi ích trong dự án là luồng xử lý được tách rõ: lớp điều khiển không trực tiếp xử lý toàn bộ nghiệp vụ và không trực tiếp viết câu lệnh SQL.

### `MVC` và `RESTful API`

Mã nguồn thể hiện kiểu tổ chức `MVC` trong Spring Boot:

- Lớp điều khiển nằm trong `backend/src/main/java/com/java/controller`.
- Dữ liệu trả ra qua các DTO trong `controller/dto`.
- Mô hình lưu trữ nằm trong `persistence/entity`.

Các điểm xử lý dùng chú thích `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, ví dụ trong `ConfigController`, `AlertController`, `ScheduleController`.

### `Repository pattern`

Các lớp trong `backend/src/main/java/com/java/persistence/repo` kế thừa `JpaRepository`, ví dụ `DeviceRepository`, `ConfigRepository`, `ControlCommandRepository`, `SensorDataRepository`.

Lợi ích trong dự án là các lớp xử lý như `TelemetryPersistenceService` và `ControlCommandService` dùng repository để truy xuất dữ liệu thay vì tự viết truy vấn trong lớp điều khiển.

### `Service layer`

Các lớp `Service` chịu trách nhiệm điều phối nghiệp vụ:

- `ControlFacadeService` điều phối điều khiển thủ công và tự động.
- `ManualControlService` xử lý điều khiển từ người dùng.
- `TelemetryIngestService` xử lý dữ liệu cảm biến.
- `ConfigService` xử lý cấu hình.
- `AlertService` xử lý cảnh báo.

Lợi ích trong dự án là nghiệp vụ được đặt ở một lớp riêng, dễ kiểm thử và dễ theo dõi khi demo.

### `Strategy`

Mẫu `Strategy` xuất hiện rõ ở đăng nhập và lịch:

- `AuthenticationStrategy` có `LocalAuthenticationStrategy` và `GoogleAuthenticationStrategy`.
- `AuthenticationStrategyResolver` chọn chiến lược theo `AuthProvider`.
- `ScheduleExecutionStrategy` được chọn qua `ScheduleExecutionStrategyResolver` trong `DeviceScheduleScheduler`.

Lợi ích trong dự án là cùng một luồng đăng nhập hoặc thực thi lịch chọn cách xử lý khác nhau mà không nhồi nhiều nhánh xử lý vào một lớp lớn.

### `Adapter`

`DeviceCommandAdapter` định nghĩa hợp đồng gửi lệnh thiết bị. `MockOhstemDeviceAdapter` triển khai hợp đồng này cho nhóm thiết bị có khóa như `yolobit`, `ohstem`, `fan`, `light`.

Lợi ích trong dự án là phần nghiệp vụ điều khiển không phụ thuộc trực tiếp vào cách gửi lệnh của từng loại thiết bị.

### `Factory`

`ControlCommandFactory` tạo `ControlCommandEntity`, gán thiết bị, đích điều khiển, giá trị và trạng thái `PENDING`.

Lợi ích trong dự án là quy tắc tạo lệnh điều khiển được gom vào một nơi, tránh lặp lại cách tạo đối tượng ở nhiều lớp xử lý.

### `Facade`

`ControlFacadeService` là điểm vào thống nhất cho điều khiển thiết bị. Lớp này tải thiết bị, kiểm tra nhà đã kích hoạt hồ sơ, rồi chuyển sang `ManualControlService` hoặc `AutoControlService`.

Lợi ích trong dự án là `ControlController` chỉ cần gọi một lớp đại diện thay vì tự điều phối nhiều lớp nghiệp vụ.

### `Observer` và xử lý theo sự kiện

`DomainEventBus`, `DomainEventListener` và `SimpleDomainEventBus` tạo cơ chế phát và nhận sự kiện trong tiến trình. `SimpleDomainEventBus` phát sự kiện sau khi giao dịch hoàn tất thông qua `TransactionSynchronizationManager`.

Lợi ích trong dự án là khi telemetry được lưu, `TelemetryIngestService` phát sự kiện để các phần khác như bảng điều khiển trực tiếp, cảnh báo và nhật ký phản ứng mà không gắn chặt vào lớp lưu telemetry.

### `Transactional Outbox`

`UserEventOutboxService` ghi sự kiện vào bảng `outbox_event`. `OutboxEventPublisher` dùng `@Scheduled` để lấy sự kiện chờ và gửi qua `RabbitTemplate` tới RabbitMQ.

Lợi ích trong dự án là sự kiện gửi ra ngoài được lưu lại trong cơ sở dữ liệu, có `status`, `retry_count`, `last_error` và `published_at` để theo dõi.

### `Dependency Injection`

Spring tự tiêm phụ thuộc qua constructor trong nhiều lớp có `@RequiredArgsConstructor`, ví dụ `DashboardController`, `ControlController`, `ManualControlService`, `TelemetryPersistenceService`.

Lợi ích trong dự án là các lớp phụ thuộc vào hợp đồng hoặc service cần dùng, giảm việc tự tạo đối tượng và giúp kiểm thử thuận lợi hơn.

### `OOP`

Mã Java dùng lớp, interface, enum và record:

- Interface: `AuthenticationStrategy`, `DeviceCommandAdapter`, `DomainEventListener`.
- Enum: `SystemMode`, `DeviceClass`, `CommandStatus`, `AlertStatus`, `AuthProvider`.
- Entity: `DeviceEntity`, `ConfigEntity`, `SensorDataEntity`, `ControlCommandEntity`.

Lợi ích trong dự án là mô hình nhà thông minh được biểu diễn bằng các kiểu dữ liệu rõ ràng thay vì dữ liệu rời rạc.

## 6. Công nghệ sử dụng và ý nghĩa/lợi ích

| Công nghệ | Dùng ở đâu | Ý nghĩa trong ứng dụng |
| --- | --- | --- |
| Java 17 | Phần máy chủ | Ngôn ngữ chính cho các lớp điều khiển, service, entity và repository. |
| Spring Boot | `backend/pom.xml`, `SmartHouseApplication.java` | Dựng ứng dụng phần máy chủ, cấu hình web, bảo mật, lịch chạy và kết nối dữ liệu. |
| Spring Web MVC | `spring-boot-starter-webmvc` | Cung cấp các điểm xử lý `RESTful API`. |
| Spring Data JPA | `spring-boot-starter-data-jpa` | Ánh xạ các entity với PostgreSQL và dùng repository. |
| Spring Security | `SecurityConfig.java` | Xử lý `JWT`, phân quyền truy cập và mã hóa mật khẩu bằng `BCryptPasswordEncoder`. |
| JJWT | `backend/pom.xml`, `JwtService.java` | Tạo và đọc `JWT` cho phiên đăng nhập. |
| Google OAuth Client | `backend/pom.xml`, `GoogleAuthenticationStrategy.java` | Hỗ trợ đăng nhập Google khi tài khoản đã được liên kết. |
| RabbitMQ | `docker-compose.yml`, `OutboxEventPublisher.java` | Nhận thông điệp sự kiện người dùng từ `outbox_event`. |
| PostgreSQL | `docker-compose.yml`, `backend/db/init.sql` | Lưu toàn bộ dữ liệu nghiệp vụ của hệ thống. |
| React | `frontend/package.json` | Xây dựng phần giao diện theo `component`. |
| Vite | `frontend/package.json`, `vite.config.js` | Công cụ chạy và dựng phần giao diện. |
| React Router | `frontend/package.json`, `AppRouter.jsx` | Định tuyến các màn hình và bảo vệ tuyến. |
| Recharts | `frontend/package.json`, `HistoryChartCard.jsx` | Vẽ biểu đồ lịch sử dữ liệu. |
| Axios | `frontend/package.json` | Có trong phụ thuộc phần giao diện. |
| Server-Sent Events | `DashboardController.stream`, `dashboardRealtime.js` | Gửi cập nhật bảng điều khiển từ phần máy chủ về trình duyệt. |
| Python Flask | `gateway.py`, `camera_interface.py` | Tạo gateway thiết bị và dịch vụ kiểm tra camera. |
| OpenCV | `camera_interface.py` | Đọc khung hình từ camera. |
| Ultralytics YOLO | `camera_interface.py`, `yolov8m.pt` | Nhận diện người trong khung hình. |
| Docker Compose | `docker-compose.yml` | Mô tả các dịch vụ `db`, `rabbitmq`, `backend`, `frontend`. |
| Nginx | `frontend/Dockerfile`, `frontend/nginx.conf` | Phục vụ bản dựng phần giao diện trong container. |

## 7. Các chức năng chính có thể demo

### 1. Đăng nhập và duy trì phiên

- Người dùng nhập tài khoản, mật khẩu ở `LoginPage`.
- `AuthProvider` gọi `loginLocal` trong `authApi.js`.
- Phần máy chủ nhận `/api/auth/login` trong `AuthController`.
- `AuthenticationService` dùng `AuthenticationStrategyResolver` để chọn `LocalAuthenticationStrategy`.
- Kết quả là `accessToken`, `refreshToken` và thông tin người dùng được lưu ở `authStorage`.

### 2. Xem bảng điều khiển nhà thông minh

- Người dùng vào `/dashboard`.
- `DashboardPage` hiển thị danh sách giám sát và danh sách thiết bị điều khiển.
- `useDashboardController` gọi `fetchDashboardByHomeId` và `fetchActiveConfigByHomeId`.
- `DashboardController` trả dữ liệu tổng hợp qua `/api/dashboard/homes/{homeId}`.
- Kết quả hiển thị gồm dữ liệu giám sát, trạng thái thiết bị và chế độ hệ thống.

### 3. Điều khiển thiết bị theo tình huống sử dụng

- Người dùng chọn quạt hoặc đèn trong `DeviceSwitchCard`.
- Giao diện gửi `target`, `value`, `actorId`, `actorName`, `method` tới `/api/control/devices/{deviceId}`.
- `ControlFacadeService` chuyển sang `ManualControlService`.
- Lệnh được ghi vào `control_commands`; trạng thái được đồng bộ qua `DeviceRuntimeStateService`.
- Thiết bị nhận lệnh qua `CommandController` hoặc gateway.

### 4. Đổi chế độ hệ thống

- Người dùng chọn chế độ trong `SegmentControl`.
- Giao diện gửi lệnh `target = mode`.
- `ManualControlService` xử lý riêng nhánh `MODE` qua `ModeManualControlHandler`.
- Trạng thái chế độ được lưu trong `device_runtime_state` với capability `MODE`.

### 5. Cấu hình ngưỡng và thiết bị giám sát

- Người dùng vào `/configs`.
- `ConfigPage` tải danh sách cấu hình và thiết bị.
- Người dùng chỉnh ngưỡng nhiệt độ, ánh sáng, tốc độ quạt và gán thiết bị cảm biến.
- `ConfigController` xử lý tạo, cập nhật, kích hoạt và xóa cấu hình.
- Bảng `configs` lưu ngưỡng và các thiết bị giám sát.

### 6. Xem lịch sử dữ liệu

- Người dùng vào `/history`.
- Người dùng chọn khoảng thời gian như `2h`, `6h`, `12h`, `24h`, `7d`.
- `HistoryPage` gọi `fetchHistoryTelemetry`.
- `TelemetryHistoryController` đọc lịch sử theo `deviceKey`.
- `HistoryChartCard` hiển thị biểu đồ bằng `Recharts`.

### 7. Xem nhật ký và sự kiện

- Người dùng vào `/audit-logs`.
- `AuditLogsPage` tải thay đổi cấu hình và sự kiện theo thời gian.
- `AuditQueryController` nhận `from`, `to`, từ khóa, phân trang và loại sự kiện.
- Dữ liệu lấy từ các bảng như `activity_logs` và `alerts`.

### 8. Quản lý lịch chế độ và quyền thành viên

- Người dùng có quyền vào `/settings`.
- `SettingsPage` hiển thị `Automated Schedule` và `User Permissions`.
- Người dùng tạo, sửa, xóa lịch chế độ qua `ModeScheduleController`.
- Người dùng quản lý thành viên trong nhà qua `HomeUserController`.
- Dữ liệu liên quan nằm ở `home_users` và các service quản lý thành viên.

### 9. Thiết bị gửi telemetry và nhận lệnh

- `aiot_code_v3.py` đọc DHT20, ánh sáng, chuyển động, xử lý IR và cập nhật LCD.
- Thiết bị gửi dữ liệu tới gateway `/gw/device-telemetry`.
- Gateway chuyển tiếp tới `/api/device-telemetry`.
- Thiết bị lấy lệnh qua `/gw/commands/next` và xác nhận qua `/gw/commands/ack`.

## 8. Kịch bản demo thực tế

### Kịch bản 1: Gia đình theo dõi phòng khách trong ngày nóng

**Bối cảnh:** Phòng khách có cảm biến nhiệt độ, độ ẩm, ánh sáng và các thiết bị quạt, đèn.

**Vai trò người dùng:** Chủ nhà hoặc thành viên có quyền truy cập nhà.

**Mục tiêu:** Theo dõi môi trường, kiểm tra trạng thái quạt/đèn và điều chỉnh chế độ phù hợp.

**Các bước demo:**

1. Đăng nhập vào ứng dụng.
2. Mở màn hình `Dashboard`.
3. Quan sát các thẻ giám sát trong khu vực `Monitoring`.
4. Chọn chế độ hệ thống trên `SegmentControl`.
5. Chọn quạt trong khu vực `Control/Switch`, thay đổi trạng thái hoặc cường độ.
6. Mở `History` để xem dữ liệu nhiệt độ hoặc trạng thái thiết bị trong khoảng `12 Hours`.

**Kết quả mong đợi trên ứng dụng:**

- Bảng điều khiển hiển thị dữ liệu giám sát và trạng thái thiết bị.
- Lệnh điều khiển được gửi tới `/api/control/devices/{deviceId}`.
- Lịch sử thể hiện dữ liệu theo thời gian trong biểu đồ.

**Điểm nhấn khi thuyết trình:**

- Người dùng không chỉ bật/tắt thiết bị, mà theo dõi tình trạng phòng rồi đưa ra thao tác điều khiển.
- Luồng này đi qua `DashboardPage`, `ControlController`, `ManualControlService`, `control_commands`, `device_runtime_state`.

### Kịch bản 2: Cấu hình tự động hóa cho phòng khách

**Bối cảnh:** Chủ nhà muốn hệ thống tự xử lý theo ngưỡng nhiệt độ và ánh sáng.

**Vai trò người dùng:** Chủ nhà hoặc quản trị viên có quyền cấu hình.

**Mục tiêu:** Cập nhật ngưỡng tự động và gán đúng thiết bị cảm biến, quạt, đèn cho hồ sơ cấu hình.

**Các bước demo:**

1. Mở màn hình `Configs`.
2. Chọn cấu hình đang dùng hoặc tạo cấu hình mới.
3. Chỉnh các trường ngưỡng như `tHigh`, `tLow`, `lHigh`, `lLow`, `tCritical`.
4. Gán thiết bị giám sát cho nhiệt độ, độ ẩm, ánh sáng, chuyển động, quạt và đèn.
5. Lưu cấu hình và kích hoạt cấu hình.
6. Mở `Audit Logs` để xem thay đổi cấu hình.

**Kết quả mong đợi trên ứng dụng:**

- Cấu hình được lưu qua `ConfigController`.
- Bảng `configs` cập nhật ngưỡng và thiết bị giám sát.
- Nhật ký cấu hình xuất hiện trong màn hình `AuditLogsPage`.

**Điểm nhấn khi thuyết trình:**

- Cấu hình tự động hóa nằm trong dữ liệu, không phải sửa mã nguồn khi đổi ngưỡng.
- `FanAutomationPolicy` và `LightAutomationPolicy` đọc ngưỡng từ `ConfigEntity`.

### Kịch bản 3: Theo dõi cảnh báo và truy vết sự kiện

**Bối cảnh:** Hệ thống ghi nhận cảnh báo hoặc thay đổi trạng thái thiết bị trong quá trình vận hành.

**Vai trò người dùng:** Quản trị viên hoặc người cần kiểm tra lịch sử vận hành.

**Mục tiêu:** Xem sự kiện, lọc theo loại và kiểm tra thay đổi cấu hình.

**Các bước demo:**

1. Mở màn hình `Audit Logs`.
2. Dùng các thẻ lọc `All`, `Alerts`, `Device Events`, `System`.
3. Tìm kiếm sự kiện bằng ô tìm kiếm.
4. Chuyển trang trong bảng sự kiện.
5. Mở phần thay đổi cấu hình để đối chiếu cấu hình cũ và mới.

**Kết quả mong đợi trên ứng dụng:**

- Màn hình hiển thị số lượng cảnh báo, sự kiện thiết bị, sự kiện hệ thống và thay đổi cấu hình.
- Dữ liệu lấy từ `/api/audit/homes/{homeId}`.
- Các bảng `activity_logs` và `alerts` là nguồn dữ liệu chính.

**Điểm nhấn khi thuyết trình:**

- Demo thể hiện khả năng truy vết hoạt động, giúp giải thích vì sao trạng thái thiết bị hoặc cấu hình đã thay đổi.

### Kịch bản 4: Quản lý vận hành theo lịch và thành viên trong nhà

**Bối cảnh:** Nhà có nhiều thành viên, chủ nhà cần đặt lịch chế độ và quản lý quyền truy cập.

**Vai trò người dùng:** Chủ nhà, đồng chủ nhà hoặc quản trị viên.

**Mục tiêu:** Tạo lịch chế độ và kiểm soát quyền thành viên.

**Các bước demo:**

1. Mở màn hình `Settings`.
2. Trong `Automated Schedule`, tạo hoặc sửa lịch chế độ.
3. Trong `User Permissions`, bật hoặc tắt quyền `allowProfileActivation` cho thành viên.
4. Đặt mật khẩu cho thành viên đăng nhập cục bộ khi nút thao tác được phép.

**Kết quả mong đợi trên ứng dụng:**

- Lịch chế độ được xử lý qua `ModeScheduleController`.
- Quyền thành viên được xử lý qua `HomeUserController`.
- Bảng `home_users` lưu vai trò trong nhà, quyền kích hoạt hồ sơ và trạng thái nhà chính.

**Điểm nhấn khi thuyết trình:**

- Kịch bản này thể hiện quản lý vận hành của cả ngôi nhà, không chỉ điều khiển một thiết bị riêng lẻ.

## 9. Kịch bản thuyết trình khoảng 20 phút

### 0:00 - 2:00: Giới thiệu bài toán và mục tiêu ứng dụng

“Nhóm em xây dựng ứng dụng `Smart House` để quản lý một ngôi nhà thông minh mẫu. Trọng tâm của hệ thống là theo dõi môi trường, điều khiển thiết bị, cấu hình tự động hóa, lưu lịch sử dữ liệu, ghi nhận cảnh báo và truy vết sự kiện. Trong mã nguồn, các chức năng này được thể hiện qua các màn hình `Dashboard`, `Configs`, `History`, `Settings`, `Audit Logs` và phần máy chủ Spring Boot.”

### 2:00 - 5:00: Trình bày mô hình hệ thống

“Hệ thống gồm phần giao diện React, phần máy chủ Spring Boot, cơ sở dữ liệu PostgreSQL, RabbitMQ, gateway Python và mã thiết bị OhStem/YoloBit. Người dùng thao tác trên phần giao diện, phần giao diện gọi `RESTful API`, phần máy chủ xử lý nghiệp vụ và lưu dữ liệu. Thiết bị gửi telemetry qua gateway, còn bảng điều khiển nhận cập nhật qua `Server-Sent Events`.”

### 5:00 - 8:00: Trình bày thiết kế ứng dụng

“Phần giao diện được chia thành `pages`, `components`, `api`, `providers`, `router`, `utils`. Phần máy chủ được chia thành `controller`, `domain/service`, `domain/provider`, `mapper`, `persistence/entity`, `persistence/repo`. Khi điều khiển thiết bị, yêu cầu đi từ `DashboardPage` tới `ControlController`, rồi qua `ControlFacadeService`, `ManualControlService` và cuối cùng ghi vào các bảng trạng thái và lệnh.”

### 8:00 - 11:00: Trình bày cơ sở dữ liệu

“Cơ sở dữ liệu xoay quanh nhà, người dùng, thiết bị, khả năng thiết bị, trạng thái hiện tại, lịch sử trạng thái, cảm biến, dữ liệu cảm biến, cấu hình, lịch, lệnh điều khiển, cảnh báo và nhật ký. Điểm quan trọng là thiết bị không bị cố định theo vài cột riêng lẻ, mà được mô hình hóa bằng `device_capabilities`, `device_runtime_state`, `device_state_history` và `control_commands`.”

### 11:00 - 14:00: Trình bày `Design pattern`, `SOLID`, `OOP` và công nghệ sử dụng

“Dự án có kiến trúc phân lớp, `Repository pattern`, `Service layer`, `Strategy`, `Adapter`, `Factory`, `Facade`, cơ chế sự kiện kiểu `Observer` và `Transactional Outbox`. Các mẫu này xuất hiện trực tiếp trong mã nguồn, ví dụ `AuthenticationStrategy`, `DeviceCommandAdapter`, `ControlCommandFactory`, `ControlFacadeService`, `SimpleDomainEventBus`, `OutboxEventPublisher`. Công nghệ chính gồm Java, Spring Boot, PostgreSQL, RabbitMQ, React, Vite, Python Flask và YOLO.”

### 14:00 - 18:00: Demo kịch bản thực tế

“Phần demo tập trung vào tình huống sử dụng thật: theo dõi phòng khách trong ngày nóng, cấu hình ngưỡng tự động, xem lịch sử dữ liệu và truy vết sự kiện. Khi thao tác trên bảng điều khiển, em sẽ chỉ ra yêu cầu được gửi tới điểm xử lý nào và dữ liệu được lưu ở bảng nào.”

### 18:00 - 20:00: Tổng kết và chuẩn bị trả lời câu hỏi

“Tổng kết lại, hệ thống đã có luồng từ người dùng đến phần máy chủ, từ thiết bị đến dữ liệu cảm biến, từ dữ liệu đến tự động hóa, cảnh báo và nhật ký. Điểm đáng chú ý là cách tổ chức mã nguồn tách lớp rõ, mô hình dữ liệu có khả năng mô tả nhiều loại thiết bị thông qua capability, và phần giao diện có đủ màn hình để demo một quy trình vận hành nhà thông minh.”

## 10. Lời thoại gợi ý khi trình bày

### Mở đầu

“Em xin trình bày đồ án `Smart House`. Đây là hệ thống quản lý nhà thông minh mẫu, tập trung vào theo dõi môi trường, điều khiển thiết bị, cấu hình tự động hóa và theo dõi lịch sử vận hành.”

### Mô hình hệ thống

“Ở mức tổng quan, hệ thống gồm phần giao diện React, phần máy chủ Spring Boot, cơ sở dữ liệu PostgreSQL, RabbitMQ, gateway Python và mã thiết bị. Người dùng thao tác trên trình duyệt, còn thiết bị gửi dữ liệu và nhận lệnh qua gateway.”

### Thiết kế ứng dụng

“Phần giao diện chia theo màn hình và `component`, còn phần máy chủ chia theo lớp điều khiển, service, provider, mapper, entity và repository. Cách chia này giúp mỗi phần chịu một trách nhiệm rõ ràng.”

### Cơ sở dữ liệu

“Cơ sở dữ liệu không chỉ lưu thiết bị, mà còn lưu capability, trạng thái hiện tại, lịch sử trạng thái, telemetry, cấu hình, lệnh điều khiển, cảnh báo và nhật ký. Nhờ đó hệ thống hiển thị trạng thái hiện tại và truy vết lại quá trình vận hành.”

### `Design pattern`, `SOLID`, `OOP`

“Trong mã nguồn có một số mẫu thiết kế rõ ràng. Ví dụ `Strategy` dùng cho đăng nhập cục bộ và Google, `Adapter` dùng cho gửi lệnh thiết bị, `Factory` dùng cho tạo lệnh điều khiển, `Facade` dùng cho luồng điều khiển thiết bị, và `Observer` dùng cho cơ chế sự kiện nội bộ.”

### Công nghệ sử dụng

“Phần máy chủ dùng Java, Spring Boot, Spring Security, Spring Data JPA và PostgreSQL. Phần giao diện dùng React, Vite, React Router và Recharts. Phần thiết bị dùng Python gateway và mã OhStem/YoloBit. RabbitMQ được dùng cho luồng sự kiện người dùng qua bảng `outbox_event`.”

### Demo

“Em sẽ demo theo bối cảnh phòng khách. Trước tiên em đăng nhập, xem bảng điều khiển, theo dõi cảm biến, điều khiển thiết bị theo tình huống, sau đó xem lịch sử và nhật ký để chứng minh hệ thống có lưu lại quá trình vận hành.”

### Tổng kết

“Qua phần demo, hệ thống thể hiện được một luồng nhà thông minh hoàn chỉnh: cảm biến gửi dữ liệu, người dùng theo dõi và điều khiển, cấu hình quyết định tự động hóa, còn nhật ký và lịch sử giúp kiểm tra lại các sự kiện đã xảy ra.”

## 11. Câu hỏi giảng viên có thể hỏi và câu trả lời gợi ý

### Vì sao chọn React cho phần giao diện?

React được dùng trong `frontend/package.json` và cấu trúc `frontend/src/components`, `frontend/src/pages`. Dự án có nhiều màn hình và nhiều khối hiển thị lặp lại như thẻ giám sát, thẻ thiết bị, bảng lịch sử, bảng nhật ký, hộp thoại xác nhận. React phù hợp với cách chia giao diện thành `component` tái sử dụng.

### Vì sao chọn Spring Boot cho phần máy chủ?

Spring Boot đang được dùng trong `backend/pom.xml`. Dự án cần nhiều điểm xử lý `RESTful API`, bảo mật, truy cập PostgreSQL, lịch chạy nền và tích hợp RabbitMQ. Spring Boot cung cấp các phần này qua `spring-boot-starter-webmvc`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-amqp`.

### Cơ sở dữ liệu được thiết kế như vậy để làm gì?

Cơ sở dữ liệu tách `devices`, `device_capabilities`, `device_runtime_state`, `device_state_history`, `control_commands` và `sensor_data`. Cách tách này giúp hệ thống lưu được thiết bị, khả năng thiết bị, trạng thái hiện tại, lịch sử thay đổi và dữ liệu cảm biến theo thời gian.

### Ứng dụng xử lý luồng dữ liệu cảm biến như thế nào?

Thiết bị gửi dữ liệu tới `/gw/device-telemetry`, gateway chuyển tiếp tới `/api/device-telemetry`. `TelemetryController` gọi `TelemetryIngestService`; `TelemetryPersistenceService` lưu vào `sensor_data`, cập nhật `device_runtime_state`, cập nhật `last_seen` của thiết bị và cảm biến.

### Dự án có dùng `Design pattern` nào không?

Dự án có nhiều mẫu thể hiện rõ trong mã. `Strategy` ở `AuthenticationStrategy`, `Adapter` ở `DeviceCommandAdapter`, `Factory` ở `ControlCommandFactory`, `Facade` ở `ControlFacadeService`, `Observer` ở `DomainEventBus` và `DomainEventListener`, `Transactional Outbox` ở `UserEventOutboxService` và `OutboxEventPublisher`.

### Dự án áp dụng `OOP` như thế nào?

Mã Java dùng entity để biểu diễn dữ liệu, service để biểu diễn luồng xử lý, interface để định nghĩa hợp đồng, enum để giới hạn trạng thái và kiểu dữ liệu. Ví dụ `DeviceEntity`, `ConfigEntity`, `AuthenticationStrategy`, `DeviceCommandAdapter`, `SystemMode`, `CommandStatus`.

### Dữ liệu được lưu và truy xuất như thế nào?

Dữ liệu được ánh xạ qua JPA entity trong `persistence/entity` và truy xuất qua repository trong `persistence/repo`. Ví dụ `TelemetryPersistenceService` dùng `DeviceRepository`, `SensorRepository`, `SensorDataRepository`; `ControlCommandService` dùng `ControlCommandRepository`.

### Bảng điều khiển cập nhật trạng thái trực tiếp bằng gì?

`DashboardController` có điểm xử lý `/api/dashboard/homes/{homeId}/stream` trả `SseEmitter`. Phần giao diện dùng `EventSource` trong `dashboardRealtime.js` để nhận các sự kiện như `DEVICE_STATE_CHANGED`, `HOME_MODE_CHANGED`, `TELEMETRY_RECEIVED`.

### Khi người dùng điều khiển thiết bị, hệ thống lưu gì?

Hệ thống lưu lệnh trong `control_commands`, trạng thái hiện tại trong `device_runtime_state`, lịch sử trong `device_state_history` và nhật ký trong `activity_logs` theo các service điều khiển và ghi nhật ký.

### Phần demo thể hiện kịch bản thực tế nào?

Phần demo thể hiện việc chủ nhà theo dõi môi trường phòng khách, điều khiển thiết bị theo tình huống, chỉnh cấu hình tự động hóa, xem lịch sử và truy vết sự kiện qua nhật ký.

### Mở rộng hệ thống thì mở rộng ở đâu?

Mở rộng thiết bị bắt đầu từ `devices`, `device_capabilities`, các lớp chính sách như `DeviceTargetPolicy`, `DeviceRuntimeStateWriteStrategy` và phần hiển thị trong giao diện. Mở rộng luồng đăng nhập bắt đầu từ `AuthenticationStrategy`. Mở rộng gửi lệnh thiết bị bắt đầu từ `DeviceCommandAdapter`.

### Điểm mạnh kỹ thuật của dự án là gì?

Dự án có luồng dữ liệu đầy đủ từ thiết bị đến cơ sở dữ liệu và giao diện; có cấu trúc phân lớp; có bảng lịch sử và nhật ký; có cập nhật trực tiếp bằng `Server-Sent Events`; có mô hình capability cho thiết bị.

### Hạn chế kỹ thuật trong kho mã là gì?

Mã thiết bị dùng `urequests`, vì vậy yêu cầu mạng vẫn là thao tác chặn trong MicroPython. `aiot_code_v3.py` đã giảm ảnh hưởng bằng cách ưu tiên xử lý cục bộ, tách telemetry khỏi hiển thị LCD và giới hạn log mặc định bằng `DEBUG=False`, `PERF_LOG=False`, `STATUS_LOG=False`.

## 12. Danh sách kiểm tra trước khi demo

### Môi trường

- Java JDK đã sẵn sàng để chạy phần máy chủ.
- Node.js và npm đã sẵn sàng để chạy phần giao diện.
- PostgreSQL đã có cơ sở dữ liệu `smarthouse`.
- RabbitMQ đã sẵn sàng với cổng `5672` và giao diện quản trị `15672`, theo `docker-compose.yml`.
- Tệp `.env` có các biến được dùng trong `docker-compose.yml` và `application.properties`.

### Cài thư viện và dựng ứng dụng

- Cài phụ thuộc phần giao diện:

```powershell
cd frontend
npm install
```

- Chạy phần giao diện:

```powershell
npm run dev
```

- Dựng phần giao diện:

```powershell
npm run build
```

- Chạy phần máy chủ:

```powershell
cd backend
.\mvnw spring-boot:run
```

- Dựng phần máy chủ:

```powershell
.\mvnw clean package -DskipTests
```

- Chạy kiểm thử phần máy chủ:

```powershell
.\mvnw test
```

### Dữ liệu và kết nối

- Kiểm tra `backend/db/init.sql` đã được dùng để tạo các bảng và dữ liệu mẫu.
- Kiểm tra nhà mẫu `Nhà thông minh mẫu` và các thiết bị `Living Room Controller`, `OhStem Fan Control`, `OhStem Light Control`, `OhStem Temperature Sender`, `OhStem Humidity Sender`, `OhStem Light Sender`, `OhStem Motion Sender`.
- Kiểm tra phần giao diện đang trỏ tới đúng địa chỉ `API` qua `VITE_API_BASE_URL` hoặc giá trị trong `apiClient.js`.
- Kiểm tra đăng nhập và vào được `/dashboard`.
- Kiểm tra `/dashboard`, `/configs`, `/history`, `/settings`, `/audit-logs` trước khi trình bày.

### Thiết bị, gateway và dịch vụ ngoài

- Gateway Python có các đường dẫn `/gw/device-telemetry`, `/gw/commands/next`, `/gw/commands/ack`, `/gw/yolo/check_human`.
- Mã thiết bị `aiot_code_v3.py` có `HOST`, `PORT`, `WIFI_SSID`, `WIFI_PASS`, `X-Device-Token`.
- Dịch vụ camera `camera_interface.py` dùng mô hình `yolov8m.pt` và cung cấp `/check_human`, `/health`.
- Chuẩn bị dữ liệu lịch sử và nhật ký trong cơ sở dữ liệu để phần `History` và `Audit Logs` có nội dung khi demo.

### Trình bày

- Chuẩn bị thứ tự demo: đăng nhập, bảng điều khiển, cấu hình, lịch sử, nhật ký, cài đặt.
- Mở sẵn mã nguồn liên quan: `DashboardPage.jsx`, `ControlController.java`, `TelemetryPersistenceService.java`, `ConfigController.java`, `backend/db/init.sql`.
- Chuẩn bị câu trả lời về mô hình dữ liệu capability, luồng telemetry, luồng điều khiển và các mẫu thiết kế đã dùng.
