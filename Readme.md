# Smart House - Hướng dẫn Setup và Chạy Dự án Local

## Mục lục
1. [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
2. [Chuẩn bị môi trường](#chuẩn-bị-môi-trường)
3. [Cấu hình Backend](#cấu-hình-backend)
4. [Cấu hình Frontend](#cấu-hình-frontend)
5. [Khởi động dự án](#khởi-động-dự-án)
6. [Kiểm tra kết nối](#kiểm-tra-kết-nối)
7. [Troubleshooting](#troubleshooting)

---

## Yêu cầu hệ thống

### Bắt buộc:
- **Java JDK 17** trở lên
- **Node.js 18** trở lên (kèm npm)
- **PostgreSQL 12** trở lên
- **RabbitMQ 3.8** trở lên
- **Python 3.8** trở lên (tùy chọn, cho IoT simulators)

### Windows:
```powershell
# Kiểm tra phiên bản
java -version
node --version
npm --version
python --version
```

---

## Chuẩn bị môi trường

### 1. Cài đặt PostgreSQL
Đảm bảo PostgreSQL đang chạy trên máy:

```powershell
# Kiểm tra PostgreSQL service
Get-Service postgresql-x64-* | Select-Object Status, DisplayName

# Nếu chưa chạy, khởi động service
Start-Service postgresql-x64-14  # Thay số phiên bản tương ứng
```

**Tạo cơ sở dữ liệu:**

```sql
-- Kết nối với PostgreSQL
psql -U postgres

-- Tạo database
CREATE DATABASE smarthouse;

-- Tạo user (nếu cần)
CREATE USER postgres WITH PASSWORD '123456';

-- Gán quyền
ALTER ROLE postgres SUPERUSER CREATEDB CREATEROLE;
```

### 2. Cài đặt và khởi động RabbitMQ
```powershell
# Windows - Khởi động RabbitMQ Service
Start-Service RabbitMQ

# Hoặc chạy từ command line
cd "C:\Program Files\RabbitMQ Server\rabbitmq_server-4.2.5\sbin"
.\rabbitmq-server.bat

# Kích hoạt Management Plugin
.\rabbitmq-plugins.bat enable rabbitmq_management

# Truy cập: http://localhost:15672/
# Default: guest / guest
```

### 3. Clone và chuyển đến thư mục dự án
```powershell
cd c:\Users\LUAN\Downloads\smart-house
```

### 4. File `.env` hoặc tạo Environment Variables
Tạo file `.env` ở thư mục `backend` hoặc đặt trong `application.properties`:

```properties
# Database
DB_URL=jdbc:postgresql://localhost:5432/smarthouse
DB_USERNAME=postgres
DB_PASSWORD=123456

# JWT Secret (tạo chuỗi ngẫu nhiên dài)
APP_JWT_SECRET=your-super-secret-key-here-minimum-32-characters-long

# OAuth (tùy chọn)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Email (tùy chọn)
MAIL_HOST=live.smtp.mailtrap.io
MAIL_PORT=587
MAIL_USERNAME=your_email@mailtrap.io
MAIL_PASSWORD=your_password

# RabbitMQ
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest

# Offline threshold (seconds)
APP_OFFLINE_THRESHOLD_SECONDS=120
```

---

## Cấu hình Backend

### 1. Chuyển vào thư mục backend
```powershell
cd backend
```

### 2. Khởi tạo cơ sở dữ liệu
```powershell
# Chạy SQL init script
psql -U postgres -d smarthouse -f db/init_v2.sql
```

### 3. Build Backend
```powershell
# Sử dụng Maven Wrapper (không cần cài Maven)
.\mvnw clean install

# Hoặc nếu dùng Maven được cài sẵn
mvn clean install
```

**Lưu ý:** Lần đầu build sẽ tải nhiều dependencies, có thể mất 5-10 phút.

### 4. Chạy Backend
```powershell
# Cách 1: Chạy bằng Spring Boot Maven plugin
.\mvnw spring-boot:run

# Cách 2: Chạy từ JAR đã build
java -jar target/aiot-project-0.0.1-SNAPSHOT.jar

# Cách 3: Chạy trong IDE (IntelliJ/Eclipse)
# Click vào main method trong Application class hoặc nhấn Shift + F10 (IntelliJ)
```

**Backend sẽ chạy tại:** `http://localhost:8080`

---

## Cấu hình Frontend

### 1. Chuyển vào thư mục frontend
```powershell
# Từ thư mục smart-house
cd frontend
```

### 2. Cài đặt Dependencies
```powershell
npm install
```

### 3. Cấu hình API Client
Mở file `src/api/apiClient.js` và đảm bảo base URL chỉ đến backend:

```javascript
const instance = axios.create({
  baseURL: 'http://localhost:8080/api',  // Hoặc process.env.VITE_API_URL
  timeout: 10000
});
```

### 4. Chạy Frontend Development Server
```powershell
npm run dev
```

**Frontend sẽ khả dụng tại:** `http://localhost:5173` (hoặc port khác nếu 5173 đã dùng)

---

## Khởi động dự án

### Bước 1: Khởi động các dịch vụ cơ sở hạ tầng

#### Terminal 1 - PostgreSQL (nếu chạy standalone)
```powershell
# Hoặc service đã running
psql -U postgres
```

#### Terminal 2 - RabbitMQ (nếu chạy standalone)
```powershell
cd "C:\Program Files\RabbitMQ Server\rabbitmq_server-4.2.5\sbin"
.\rabbitmq-server.bat
# Hoặc đã running như service
```

### Bước 2: Khởi động Backend

#### Terminal 3 - Backend
```powershell
cd c:\Users\LUAN\Downloads\smart-house\backend
.\mvnw spring-boot:run
```

Khi thấy `Tomcat started on port(s): 8080` = Backend sẵn sàng

### Bước 3: Khởi động Frontend

#### Terminal 4 - Frontend
```powershell
cd c:\Users\LUAN\Downloads\smart-house\frontend
npm run dev
```

Khi thấy `Local: http://localhost:5173/` = Frontend sẵn sàng

### Bước 4 (Tùy chọn): Chạy Python IoT Simulator

#### Terminal 5 - Python Scripts
```powershell
# Cấu hình Python environment (tùy chọn)
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt  # Nếu có file này

# Chạy simulator device
cd c:\Users\LUAN\Downloads\smart-house
python virtual_device_simulator.py
```

---

## Kiểm tra kết nối

### 1. Backend Health Check
```bash
curl http://localhost:8080/health
# hoặc mở browser: http://localhost:8080/health
```

### 2. Frontend
```
Mở browser: http://localhost:5173
```

### 3. RabbitMQ Management Console
```
http://localhost:15672/
# Username: guest
# Password: guest
```

### 4. Database Connection
```powershell
psql -U postgres -d smarthouse -c "SELECT * FROM information_schema.tables;"
```

---

## Build và Deployment

### Build Frontend
```powershell
cd frontend
npm run build

# Output: dist/ folder
```

### Build Backend
```powershell
cd backend
.\mvnw clean package -DskipTests

# Output: target/aiot-project-0.0.1-SNAPSHOT.jar
```

---

## Troubleshooting

### Lỗi: "Connection refused" khi kết nối Database

**Giải pháp:**
```powershell
# Kiểm tra PostgreSQL service
Get-Service postgresql-x64-* | Select-Object Status

# Khởi động nếu không chạy
Start-Service postgresql-x64-14

# Kiểm tra port 5432
netstat -ano | findstr :5432
```

### Lỗi: "RabbitMQ Connection refused"

**Giải pháp:**
```powershell
# Kiểm tra RabbitMQ service
Get-Service RabbitMQ | Select-Object Status

# Khởi động nếu cần
Start-Service RabbitMQ

# Kiểm tra port 5672
netstat -ano | findstr :5672
```

### Lỗi: "Port 8080 already in use"

**Giải pháp:**
```powershell
# Tìm process dùng port 8080
netstat -ano | findstr :8080

# Xem chi tiết
Get-Process -Id <PID>

# Kill process (thay <PID>)
Stop-Process -Id <PID> -Force

# Hoặc đổi port trong application.properties
# server.port=8081
```

### Lỗi: "Maven compilation failed"

**Giải pháp:**
```powershell
# Clear Maven cache
cd backend
.\mvnw clean

# Rebuild
.\mvnw clean install -DskipTests

# Nếu vẫn lỗi, xóa thư mục .m2
# Defaul location: %USERPROFILE%\.m2\repository
```

### Lỗi: Frontend không thể gọi API (CORS)

**Kiểm tra:**
1. Backend running trên `http://localhost:8080`
2. Frontend API URL trỏ đến đúng: `http://localhost:8080/api`
3. Backend có cấu hình CORS (nếu cần)

---

## Ghi chú bổ sung

### Cấu trúc dự án
```
smart-house/
├── backend/           # Spring Boot API (port 8080)
├── frontend/          # React + Vite (port 5173)
├── aiot_code_v1.py   # IoT data processor
├── aiot_code_v2.py   # IoT data processor v2 - use this
└── virtual_device_simulator.py  # Mock IoT data process aiot_v2
```

### Default Credentials
- **Database:** postgres / 123456
- **RabbitMQ:** guest / guest
- **JWT Expiration:** 720 minutes (12 hours)

### Useful Commands

#### Backend
```powershell
# Chỉ compile không build
.\mvnw compile

# Chạy tests
.\mvnw test

# Skip tests khi build
.\mvnw clean install -DskipTests

# View dependencies
.\mvnw dependency:tree
```

#### Frontend
```powershell
# Lint code
npm run lint

# Preview build output
npm run preview
```

---

## Hỗ trợ

Nếu gặp vấn đề:
1. Kiểm tra lại file `.env` hoặc environment variables
2. Đảm bảo tất cả services đang chạy (PostgreSQL, RabbitMQ)
3. Check lại console logs để tìm error message
4. Xóa cache (`.m2` cho Maven, `node_modules` cho npm)
---

**Thanks !**
