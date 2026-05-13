# Kịch Bản Các Luồng Demo Báo Cáo - Smart House

Tài liệu này cung cấp 4 luồng kịch bản demo (Demo Flows) chi tiết, logic và bao quát các tính năng cốt lõi của hệ thống Smart House, được thiết kế tối ưu để trình bày trước hội đồng trong khoảng thời gian từ 15 - 20 phút.

---

## 🚀 Luồng 1: Giám sát Môi trường và Điều khiển Thiết bị (Tính năng cốt lõi)

**Bối cảnh:** Chủ nhà kiểm tra tình trạng phòng khách, phát hiện trời nóng và tự tay bật quạt, điều chỉnh đèn.

*   **Bước 1:** **Đăng nhập** vào hệ thống bằng tài khoản chủ nhà.
*   **Bước 2:** Mở màn hình **Dashboard** (Bảng điều khiển).
*   **Bước 3:** Trình bày khu vực *Monitoring (Giám sát)*. Chỉ ra các thông số thời gian thực từ cảm biến (Nhiệt độ, Độ ẩm, Ánh sáng) đang được stream trực tiếp về giao diện.
*   **Bước 4:** Thao tác trên *SegmentControl* để chuyển chế độ hệ thống (ví dụ: chuyển từ chế độ `Auto` sang `Manual`).
*   **Bước 5:** Trong khu vực *Control/Switch*, thao tác bật/tắt thiết bị Quạt và Đèn (thay đổi trạng thái on/off hoặc kéo thanh trượt cường độ).
*   **Bước 6:** Mở tab **History** (Lịch sử). Chọn khoảng thời gian "12 Hours" để cho thấy dữ liệu biểu đồ nhiệt độ và trạng thái thiết bị thay đổi theo thời gian thực (dữ liệu được truy vấn thực tế từ CSDL).

> 💡 **Điểm nhấn khi thuyết trình:** 
> Nhấn mạnh hệ thống có khả năng stream dữ liệu trực tiếp (sử dụng công nghệ Server-Sent Events - SSE) từ thiết bị thực gửi về qua Gateway. Trạng thái điều khiển được đồng bộ ngay lập tức xuống cơ sở dữ liệu (`device_runtime_state`).

---

## ⚙️ Luồng 2: Cấu hình Tự động hóa (Automation)

**Bối cảnh:** Người dùng muốn hệ thống tự động bật quạt khi nhiệt độ cao và tự động bật đèn khi trời tối mà không cần thao tác thủ công.

*   **Bước 1:** Mở màn hình **Configs** (Cấu hình).
*   **Bước 2:** Chọn tạo một cấu hình mới hoặc click vào nút sửa cấu hình mẫu có sẵn "My Smart House Config".
*   **Bước 3:** Thiết lập các ngưỡng môi trường: `tHigh` (Ngưỡng nhiệt độ cao), `tLow` (Ngưỡng nhiệt độ thấp), `lLow` (Ngưỡng ánh sáng yếu).
*   **Bước 4:** Tiến hành gán các thiết bị cảm biến và thiết bị điều khiển tương ứng (ví dụ: gán thiết bị cảm biến nhiệt độ DHT20 vào để đo nhiệt, và gán thiết bị Quạt vào để thực thi).
*   **Bước 5:** Nhấn Lưu thay đổi và chọn **Kích hoạt (Activate)** cấu hình này.
*   **Bước 6 (Tùy chọn nếu có phần cứng thật):** Dùng tay che cảm biến ánh sáng hoặc tác động nhiệt độ lên cảm biến nhiệt để cho hội đồng thấy đèn/quạt lập tức tự động phản hồi theo ngưỡng vừa cài đặt.

> 💡 **Điểm nhấn khi thuyết trình:** 
> Giải thích với hội đồng rằng tính năng tự động hóa của hệ thống được thiết kế hoàn toàn tách biệt và linh hoạt (Data-driven). Người dùng có thể thay đổi luật tự động hóa ngay trên giao diện UI mà không cần phải nạp lại code (hardcode) vào phần cứng hay khởi động lại Backend.

---

## 🛡️ Luồng 3: Quản lý Vận hành theo Lịch và Quyền Thành viên

**Bối cảnh:** Gia đình có nhiều thành viên (vợ chồng, con cái), chủ nhà muốn thiết lập lịch hoạt động tự động cho các thiết bị và phân quyền sử dụng cho người khác.

*   **Bước 1:** Chuyển sang màn hình **Settings** (Cài đặt).
*   **Bước 2:** Trình bày tab *Automated Schedule* (Lịch tự động). Thực hiện tạo một lịch trình ví dụ: "Tự động tắt toàn bộ đèn ở phòng khách vào lúc 23:00 mỗi ngày".
*   **Bước 3:** Chuyển sang tab *User Permissions* (Phân quyền người dùng).
*   **Bước 4:** Thao tác bật/tắt quyền `allowProfileActivation` (quyền cho phép đổi cấu hình tự động) đối với một thành viên bất kỳ trong gia đình.

> 💡 **Điểm nhấn khi thuyết trình:** 
> Thể hiện rằng đây không chỉ là hệ thống điều khiển tắt/mở đơn thuần của một dự án IoT cơ bản, mà đã được nâng cấp thành một nền tảng quản lý nhà thông minh trọn vẹn dành cho nhiều đối tượng người dùng (Hỗ trợ Role-based access control - RBAC).

---

## 🔍 Luồng 4: Truy vết Sự kiện và Nhật ký Hệ thống (Audit Logs)

**Bối cảnh:** Quản trị viên hệ thống hoặc chủ nhà muốn kiểm tra xem ai đã tắt/bật quạt, thiết bị nào bị lỗi, hoặc ai đã thay đổi thông số cấu hình tự động trong ngày.

*   **Bước 1:** Mở màn hình **Audit Logs** (Nhật ký sự kiện).
*   **Bước 2:** Trình diễn các công cụ lọc dữ liệu (Filters): Lọc theo "Device Events" (Sự kiện từ thiết bị) hoặc "System" (Sự kiện hệ thống).
*   **Bước 3:** Sử dụng ô tìm kiếm để tìm lại sự kiện thay đổi cấu hình mà ta đã thực hiện ở phần trước (Luồng 2).
*   **Bước 4:** Click mở rộng chi tiết một bản ghi sự kiện đổi cấu hình để so sánh trực quan giá trị cũ (Old Value) và giá trị mới (New Value).

> 💡 **Điểm nhấn khi thuyết trình:** 
> Chứng minh hệ thống có tính bảo mật, minh bạch và khả năng truy xuất lịch sử cao. Mọi hành động từ việc bật tắt đèn, API trả về lỗi đến thay đổi cấu hình đều được hệ thống ghi nhận chính xác (sử dụng cơ chế `activity_logs` kết hợp RabbitMQ/EventBus ở phía Backend).

---

## 📷 Luồng 5: Nhận diện Người bằng AI Camera (YOLO) và Cảnh báo

**Bối cảnh:** Có chuyển động xảy ra tại khu vực giám sát. Hệ thống sử dụng cảm biến kết hợp AI Camera để xác thực chính xác có người hay không trước khi đưa ra cảnh báo an ninh.

*   **Bước 1:** Kích hoạt cảm biến chuyển động (Motion Sensor) trên mô hình phần cứng để tạo tín hiệu báo động.
*   **Bước 2:** Trình bày với hội đồng quá trình hệ thống xác thực: Gateway nhận tín hiệu chuyển động, sau đó tự động gọi API `/check_human` sang dịch vụ AI Camera.
*   **Bước 3:** Dịch vụ AI (chạy file `camera_interface.py` với model **YOLOv8**) chụp và phân tích khung hình từ camera, nhận diện đối tượng và trả về kết quả xác nhận `Có người`.
*   **Bước 4:** Mở lại màn hình **Audit Logs** (Nhật ký) hoặc phần Cảnh báo (Alerts) để hiển thị thông báo "Phát hiện chuyển động và xác nhận có người" vừa được lưu vào hệ thống thời gian thực.

> 💡 **Điểm nhấn khi thuyết trình:** 
> Nêu bật được sự kết hợp giữa **phần cứng IoT** (Cảm biến PIR) và **Trí tuệ nhân tạo** (Computer Vision - YOLO). Sự kết hợp này giúp chống báo động giả (False alarm - ví dụ vật nuôi chạy ngang qua) rất hiệu quả. Đồng thời khoe được kiến trúc Microservices của Gateway khi có thể giao tiếp dễ dàng với dịch vụ AI độc lập.
