# Hướng dẫn cài đặt và chạy dự án CD-WEB (Bảo Khang Gadget)

Tài liệu này hướng dẫn chạy **toàn bộ hệ thống** gồm Backend (Spring Boot) và Frontend (Next.js) trên máy local.

| Thành phần | Repository | Công nghệ |
|------------|------------|-----------|
| **Backend** | [CD-WEB-BE](https://github.com/HoangKhangKhuPho4/CD-WEB-BE) | Java 17, Spring Boot 3.2, MySQL, Redis |
| **Frontend** | [CD-WEB-FE](https://github.com/HoangKhangKhuPho4/CD-WEB-FE) | Next.js 16, React 19, TypeScript, Tailwind |

---

## 1. Yêu cầu hệ thống

Cài đặt các phần mềm sau **trước khi** chạy dự án:

| Phần mềm | Phiên bản khuyến nghị | Kiểm tra |
|----------|------------------------|----------|
| **JDK** | 17+ | `java -version` |
| **Maven** | 3.8+ | `mvn -version` |
| **Node.js** | 20.x LTS | `node -version` |
| **npm** | 10+ | `npm -version` |
| **MySQL** | 8.x | MySQL Workbench hoặc CLI |
| **Redis** | 6+ | `redis-cli ping` → trả về `PONG` |
| **Git** | Mới nhất | `git --version` |

**Tùy chọn** (không bắt buộc cho dev cơ bản):

- **Postman** — test API
- **ngrok** — test VNPay/MoMo callback trên local ([docs/NGROK_SETUP.md](./NGROK_SETUP.md))

---

## 2. Clone mã nguồn

Mở terminal và clone **2 repository** (đặt cạnh nhau trên ổ đĩa):

```bash
git clone https://github.com/HoangKhangKhuPho4/CD-WEB-BE.git
git clone https://github.com/HoangKhangKhuPho4/CD-WEB-FE.git
```

Cấu trúc gợi ý:

```text
📁 D:\Projects\
├── 📂 CD-WEB-BE/     ← Backend
└── 📂 CD-WEB-FE/     ← Frontend
```

---

## 3. Cấu hình MySQL

### 3.1. Tạo database

Đăng nhập MySQL và chạy:

```sql
CREATE DATABASE cd_web CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3.2. Cấu hình kết nối Backend

Mở file `CD-WEB-BE/src/main/resources/application.yml`, chỉnh phần datasource cho khớp máy bạn:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cd_web
    username: root
    password: ""          # ← điền mật khẩu MySQL của bạn
```

> **Lưu ý:** Hibernate dùng `ddl-auto: update` — lần chạy đầu sẽ **tự tạo/cập nhật bảng**. RBAC (role, permission) được seed tự động qua `RbacDataInitializer`.

### 3.3. Import dữ liệu mẫu (nếu nhóm có file SQL)

Nếu giảng viên / nhóm cung cấp file dump `.sql`, import vào `cd_web`:

```bash
mysql -u root -p cd_web < du_lieu_mau.sql
```

Không có file dump → chạy Backend lần đầu, sau đó tạo tài khoản qua đăng ký hoặc seed thủ công.

---

## 4. Cấu hình Redis

Backend dùng Redis cho analytics / hàng đợi tương tác người dùng.

**Windows (Docker):**

```bash
docker run -d --name redis-cdweb -p 6379:6379 redis:7
```

**Hoặc cài Redis native** và đảm bảo chạy tại `localhost:6379` (mặc định trong `application.yml`).

Kiểm tra:

```bash
redis-cli ping
```

Kết quả mong đợi: `PONG`

---

## 5. Chạy Backend (Spring Boot)

### 5.1. Biến môi trường (khuyến nghị)

```bash
cd CD-WEB-BE
copy .env.example .env        # Windows
# cp .env.example .env        # macOS / Linux
```

Chỉnh `.env` nếu cần (JWT, email, OAuth). Dev local có thể dùng giá trị mặc định trong `application.yml`.

**Không commit** file `.env` lên GitHub.

### 5.2. Build và chạy

```bash
cd CD-WEB-BE
mvn clean install -DskipTests
mvn spring-boot:run
```

Hoặc chạy class main `com.cdweb.be.BeApplication` từ IDE (IntelliJ / VS Code).

### 5.3. Kiểm tra Backend

| Kiểm tra | URL |
|----------|-----|
| API base | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

Console không lỗi, log có dòng `Started BeApplication` → Backend **OK**.

---

## 6. Chạy Frontend (Next.js)

### 6.1. Cài dependencies

```bash
cd CD-WEB-FE
npm ci
```

> Dùng `npm ci` (khuyến nghị) hoặc `npm install` nếu chưa có `package-lock.json`.

### 6.2. Biến môi trường

```bash
copy .env.local.example .env.local    # Windows
# cp .env.local.example .env.local    # macOS / Linux
```

Nội dung tối thiểu trong `.env.local`:

```env
# URL Backend — KHÔNG thêm /api ở cuối
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

# Tùy chọn — bật đăng nhập Google/Facebook
NEXT_PUBLIC_GOOGLE_CLIENT_ID=
NEXT_PUBLIC_FACEBOOK_APP_ID=
```

**Không commit** `.env.local` lên GitHub.

### 6.3. Chạy dev server

```bash
npm run dev
```

Script dev dùng `--experimental-https` → trình duyệt có thể mở:

- **https://localhost:3000** (HTTPS)
- hoặc **http://localhost:3000**

### 6.4. Kiểm tra Frontend

| Trang | URL |
|-------|-----|
| Storefront (trang chủ) | http://localhost:3000 |
| Đăng nhập | http://localhost:3000/signin |
| Admin panel | http://localhost:3000/admin |
| Thống kê | http://localhost:3000/admin/analytics |

Frontend **bắt buộc** Backend đang chạy tại `NEXT_PUBLIC_API_BASE_URL`.

---

## 7. Luồng chạy đúng thứ tự

```text
1. MySQL đang chạy          → database cd_web sẵn sàng
2. Redis đang chạy          → localhost:6379
3. Backend (port 8080)      → mvn spring-boot:run
4. Frontend (port 3000)     → npm run dev
5. Mở trình duyệt           → localhost:3000
```

---

## 8. Tài khoản đăng nhập

Tài khoản phụ thuộc **dữ liệu trong MySQL**. Nếu nhóm dùng DB mẫu / Postman environment:

| Username | Mật khẩu | Vai trò | Ghi chú |
|----------|----------|---------|---------|
| `admin` | `admin123` | ADMIN | Toàn quyền, vào `/admin` |
| `sales` | _(theo DB nhóm)_ | SALES | Bán hàng, đơn hàng |
| `warehouse` | _(theo DB nhóm)_ | WAREHOUSE | Kho, IMEI |
| `luutien` | `123456` | CUSTOMER | Khách hàng test |

Đăng nhập admin: **http://localhost:3000/signin** → tự redirect `/admin` nếu có quyền staff.

---

## 9. Test API bằng Postman

Collection nằm trong `CD-WEB-BE/postman/`:

| File | Mô tả |
|------|--------|
| `CD-WEB-Local.postman_environment.json` | Biến môi trường local |
| `CD-WEB-Revenue-Statistics.postman_collection.json` | API Tổng quan / thống kê |
| `CD-WEB-Stock-Import-Return.postman_collection.json` | Nhập kho / trả hàng |
| _(và các collection khác)_ | Sản phẩm, review, warranty, … |

**Thứ tự test:**

1. Import collection + environment vào Postman
2. Chọn environment **CD-WEB Local**
3. Chạy **Login Admin** (folder `00 - Setup & Auth`)
4. Chạy các request còn lại

> Nếu gặp **401 Unauthorized**: chạy Login Admin trước; đảm bảo `adminToken` trong environment có giá trị sau login.

---

## 10. Build production (tùy chọn)

**Backend:**

```bash
cd CD-WEB-BE
mvn clean package -DskipTests
java -jar target/be-0.0.1-SNAPSHOT.jar
```

**Frontend:**

```bash
cd CD-WEB-FE
npm run build
npm run start
```

---

## 11. Xử lý lỗi thường gặp

### Backend không start — lỗi MySQL

```
Communications link failure / Access denied for user
```

→ Kiểm tra MySQL đang chạy, username/password trong `application.yml`, database `cd_web` đã tạo.

### Backend không start — lỗi Redis

```
Unable to connect to Redis
```

→ Khởi động Redis tại `localhost:6379` hoặc chỉnh `spring.data.redis` trong `application.yml`.

### Frontend — API lỗi / không load dữ liệu

→ Kiểm tra Backend chạy tại `http://localhost:8080`  
→ Kiểm tra `.env.local`: `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`  
→ Restart frontend sau khi sửa `.env.local`

### CORS / cookie

→ Dev local: FE `localhost:3000`, BE `localhost:8080` — cấu hình CORS đã có trong Backend.

### Postman 401

→ Chưa login hoặc token hết hạn → chạy lại **Login Admin**.

### Thanh toán VNPay/MoMo trên local

→ Cần **ngrok** expose port 8080 — xem [NGROK_SETUP.md](./NGROK_SETUP.md).

---

## 12. Tài liệu liên quan

| Tài liệu | Đường dẫn |
|----------|-----------|
| README tổng quan dự án | [../README.md](../README.md) |
| API Documentation | [API_Documentation.md](./API_Documentation.md) |
| Trạng thái Backend | [BACKEND_STATUS.md](./BACKEND_STATUS.md) |
| Cấu hình ngrok (VNPay) | [NGROK_SETUP.md](./NGROK_SETUP.md) |
| README Frontend | [CD-WEB-FE README](https://github.com/HoangKhangKhuPho4/CD-WEB-FE/blob/main/README.md) |

---

<p align="center">
  <strong>Bảo Khang Gadget — CD-WEB</strong><br/>
  <em>Hướng dẫn cài đặt · Phiên bản cập nhật 2026</em>
</p>
