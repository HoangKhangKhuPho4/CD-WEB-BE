# Bảo Khang Gadget — Nền tảng Thương mại Điện tử Công nghệ

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16-black?logo=next.js)](https://nextjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue?logo=typescript)](https://www.typescriptlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![CI Frontend](https://img.shields.io/badge/CI-Frontend%20lint%20%26%20build-blue?logo=github)](https://github.com/HoangKhangKhuPho4/CD-WEB-FE/actions)

> **Đồ án / Dự án:** Website thương mại điện tử chuyên bán thiết bị công nghệ, phụ kiện chính hãng — giao diện storefront hiện đại, khu vực quản trị phân quyền (Admin / Sales / Warehouse), thanh toán trực tuyến và quản lý kho theo IMEI.

---

## 1. Tên dự án & Thông tin cốt lõi

| Hạng mục | Nội dung |
|----------|----------|
| **Tên thương hiệu** | **Bảo Khang Gadget** |
| **Mô tả ngắn** | Nền tảng TMĐT cung cấp sản phẩm công nghệ chính hãng, hỗ trợ tìm kiếm thông minh, đặt hàng trực tuyến, theo dõi vận chuyển và quản trị kho — bảo hành tập trung. |
| **Mã đồ án** | Website Thương Mại Điện Tử |

### Thành viên nhóm (2 người)

| MSSV | Họ tên | Vai trò | Liên kết |
|------|--------|---------|----------|
| 22130116 | **Nguyễn Lê Hoàng Khang** | Trưởng nhóm · Backend Developer | [GitHub @HoangKhangKhuPho4](https://github.com/HoangKhangKhuPho4) |
| 22130025 | **Phạm Thái Bảo** | Frontend Developer | _Cập nhật link GitHub cá nhân_ |

### Repository

| Thành phần | Repository |
|------------|------------|
| **Backend (API)** | [CD-WEB-BE](https://github.com/HoangKhangKhuPho4/CD-WEB-BE) |
| **Frontend (Web)** | [CD-WEB-FE](https://github.com/HoangKhangKhuPho4/CD-WEB-FE) |

---

## 2. Tổng quan dự án (Live Demo & Ảnh minh họa)

### Demo

| Môi trường | URL |
|------------|-----|
| **Storefront (local)** | [http://localhost:3000](http://localhost:3000) |
| **Admin panel (local)** | [http://localhost:3000/admin](http://localhost:3000/admin) |
| **API / Swagger (local)** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| **Production** | _Đang cập nhật — Vercel (FE) / Render hoặc VPS (BE)_ |

> Sau khi deploy, thay các link trên bằng URL thật (ví dụ: `https://baokhanggadget.vercel.app`).

### Ảnh minh họa

Chèn screenshot hoặc GIF vào thư mục `docs/images/` và cập nhật đường dẫn bên dưới:

| Trang | Gợi ý file |
|-------|------------|
| Trang chủ | `docs/images/home.png` |
| Chi tiết sản phẩm | `docs/images/product-detail.png` |
| Giỏ hàng & Thanh toán | `docs/images/cart-checkout.png` |
| Khu vực Admin | `docs/images/admin-dashboard.png` |

```markdown
![Trang chủ](docs/images/home.png)
![Chi tiết sản phẩm](docs/images/product-detail.png)
![Admin Dashboard](docs/images/admin-dashboard.png)
```

### Mục đích dự án

- Cung cấp **trải nghiệm mua sắm trực quan** cho khách hàng mua thiết bị công nghệ.
- **Tìm kiếm & gợi ý sản phẩm** theo từ khóa, danh mục (autocomplete).
- **Thanh toán đa kênh** (VNPay, COD, …) và **tích hợp vận chuyển GHN**.
- **Quản lý nội bộ** theo vai trò: quản trị viên, nhân viên bán hàng, nhân viên kho (RBAC).
- **Truy xuất IMEI / serial**, phiếu bảo hành phù hợp mô hình cửa hàng điện tử.

---

## 3. Công nghệ sử dụng (Tech Stack)

### Frontend — [CD-WEB-FE](https://github.com/HoangKhangKhuPho4/CD-WEB-FE)

- **Framework:** [Next.js 16](https://nextjs.org/) (App Router), [React 19](https://react.dev/)
- **Ngôn ngữ:** TypeScript
- **State:** Redux Toolkit, React Redux
- **Styling:** Tailwind CSS
- **HTTP:** Axios
- **UI / UX:** React Hot Toast, Swiper, component module hóa (Storefront + Admin)

### Backend — [CD-WEB-BE](https://github.com/HoangKhangKhuPho4/CD-WEB-BE) _(repo hiện tại)_

- **Framework:** Spring Boot 3.2
- **Ngôn ngữ:** Java 17
- **Bảo mật:** Spring Security, JWT (+ refresh token), OAuth2 (Google / Facebook)
- **ORM:** Spring Data JPA (Hibernate)
- **Cơ sở dữ liệu:** MySQL
- **Cache:** Redis
- **API docs:** SpringDoc OpenAPI (Swagger UI)
- **Khác:** Spring Mail, MapStruct / ModelMapper, Actuator

### DevOps & Công cụ hỗ trợ

- **Version control:** Git, GitHub
- **CI (Frontend):** GitHub Actions — `lint` + `build` trên nhánh `main`
- **Thanh toán:** VNPay (sandbox), MoMo _(service tích hợp trong codebase)_
- **Vận chuyển:** GHN API (sandbox)
- **Gợi ý AI (tùy chọn):** Microservice Python/Flask SVD — `AI_SERVICE_BASE_URL`
- **Container _(khuyến nghị triển khai):_** Docker _(có thể bổ sung `docker-compose` sau)_

---

## 4. Các tính năng nổi bật (Features)

### Dành cho Khách hàng (Customer / Storefront)

- Đăng ký, đăng nhập (email, Google, Facebook), quên / đặt lại mật khẩu.
- Duyệt danh mục, **tìm kiếm & gợi ý sản phẩm** (autocomplete), lọc theo danh mục.
- Xem chi tiết sản phẩm, đánh giá, wishlist.
- **Giỏ hàng**, chọn địa chỉ, tính phí ship (GHN), áp mã giảm giá.
- **Đặt hàng & thanh toán** (VNPay, COD, …).
- Theo dõi **lịch sử đơn hàng**, hủy đơn (theo quy tắc nghiệp vụ).
- **Tra cứu bảo hành** theo mã / IMEI.

### Dành cho Quản trị viên & Nhân viên (Admin Panel)

| Vai trò | Quyền hạn chính |
|---------|------------------|
| **ADMIN** | Toàn quyền: sản phẩm, đơn hàng, kho, CMS, cấu hình, nhân viên, thống kê doanh thu |
| **SALES** | Xem SP, xử lý đơn (xác nhận / hủy / giao), phiếu bảo hành, báo cáo bán hàng |
| **WAREHOUSE** | Sản phẩm & kho, nhập tồn, IMEI, đơn hàng (xuất), xử lý hàng hoàn |

**Module admin:**

- Dashboard thống kê (doanh thu / đơn hàng / sản phẩm bán chạy).
- Quản lý **sản phẩm, danh mục, thương hiệu, thuộc tính, mã giảm giá**.
- **Nhập kho, IMEI**, trả hàng nhập kho, gán IMEI cho đơn.
- **Quản lý đơn hàng** (trạng thái, vận đơn, timeline).
- **Phiếu bảo hành**, duyệt & phản hồi đánh giá.
- **Khách hàng** (danh sách), **nhân viên & phân quyền (RBAC)**.
- **Banner & bài viết (CMS)**, cấu hình hệ thống & AI.

> **Lưu ý phát triển:** Đặt hộ khách (POS) và CRM chăm sóc khách hàng đang trong lộ trình bổ sung (xem mục 8).

---

## 5. Hướng dẫn cài đặt và chạy dự án (Getting Started)

> **Tài liệu chi tiết (khuyến nghị):** [docs/HUONG_DAN_CAI_DAT.md](docs/HUONG_DAN_CAI_DAT.md) — hướng dẫn đầy đủ BE + FE, MySQL, Redis, Postman, xử lý lỗi.

### Yêu cầu hệ thống

| Phần mềm | Phiên bản khuyến nghị |
|----------|------------------------|
| **JDK** | 17+ |
| **Maven** | 3.8+ |
| **Node.js** | 20.x |
| **npm** | 10+ |
| **MySQL** | 8.x |
| **Redis** | 6+ _(tùy cấu hình cache)_ |

### Bước 1 — Clone repository

```bash
git clone https://github.com/HoangKhangKhuPho4/CD-WEB-BE.git
git clone https://github.com/HoangKhangKhuPho4/CD-WEB-FE.git
```

### Bước 2 — Cấu hình Database (MySQL)

```sql
CREATE DATABASE cd_web CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Hibernate `ddl-auto: update` sẽ tự tạo/cập nhật bảng khi chạy lần đầu. Dữ liệu RBAC mẫu được seed qua `RbacDataInitializer`.

### Bước 3 — Backend

```bash
cd CD-WEB-BE
cp .env.example .env   # Windows: copy .env.example .env
# Chỉnh .env hoặc application.yml — KHÔNG commit file chứa secret thật
mvn spring-boot:run
```

API chạy tại: **http://localhost:8080**  
Swagger: **http://localhost:8080/swagger-ui.html**

### Bước 4 — Frontend

```bash
cd CD-WEB-FE
cp .env.local.example .env.local
# Điền NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
npm ci
npm run dev
```

Storefront: **http://localhost:3000**  
Admin: **http://localhost:3000/admin**

### Bước 5 — Ngrok (test VNPay / MoMo trên local)

Dev hàng ngày dùng `http://localhost:8080` — **không bắt buộc** ngrok.

Khi cần test **thanh toán VNPay / MoMo** (Return URL & callback từ internet), xem hướng dẫn chi tiết:

**[docs/NGROK_SETUP.md](docs/NGROK_SETUP.md)**

Tóm tắt:

1. `ngrok config add-authtoken "..."` (token từ [dashboard ngrok](https://dashboard.ngrok.com/get-started/your-authtoken))
2. Chạy Backend → `ngrok http 8080`
3. Sửa `app.backend-url` trong `application.yml` → restart Backend
4. Giữ terminal ngrok mở suốt phiên test

### Biến môi trường

| Repo | File mẫu | File thật (gitignore) |
|------|----------|------------------------|
| Backend | [`.env.example`](.env.example) | `.env` |
| Frontend | [`.env.local.example`](https://github.com/HoangKhangKhuPho4/CD-WEB-FE/blob/main/.env.local.example) | `.env.local` |

**Tuyệt đối không** đẩy `.env`, `.env.local`, mật khẩu DB, `JWT_SECRET`, key VNPay/GHN lên GitHub.

### Tài khoản demo (sau seed RBAC / import DB nhóm)

| Username | Vai trò | Ghi chú |
|----------|---------|---------|
| `admin` | ADMIN | Mật khẩu theo DB mẫu nhóm (Postman: `admin123`) |
| `sales` | SALES | Nhân viên bán hàng |
| `warehouse` | WAREHOUSE | Nhân viên kho |
| `luutien` | CUSTOMER | Khách test (Postman: `123456`) |

Chi tiết đăng nhập và test API: [docs/HUONG_DAN_CAI_DAT.md](docs/HUONG_DAN_CAI_DAT.md).

---

## 6. Cấu trúc thư mục (Project Structure)

Dự án tách **2 repository**; sơ đồ tổng thể:

```text
📦 Bao-Khang-Gadget (hệ thống)
├── 📂 CD-WEB-FE/                    # Frontend — Next.js
│   ├── 📂 src/
│   │   ├── 📂 app/                  # App Router (storefront + /admin)
│   │   ├── 📂 components/           # UI (Header, Shop, Admin, …)
│   │   ├── 📂 redux/                # Store & slices (auth, cart, …)
│   │   ├── 📂 utils/                # API client, RBAC, format
│   │   └── 📂 config/               # brand.ts, …
│   ├── 📂 public/images/
│   ├── 📜 package.json
│   └── 📜 .env.local.example
│
└── 📂 CD-WEB-BE/                    # Backend — Spring Boot
    ├── 📂 src/main/java/com/cdweb/be/
    │   ├── 📂 config/               # Security, CORS, RBAC seed
    │   ├── 📂 controller/          # REST (public + admin)
    │   ├── 📂 service/             # Business logic
    │   ├── 📂 repository/          # JPA
    │   ├── 📂 entity/              # Domain models
    │   ├── 📂 dto/                 # Request / Response
    │   └── 📂 exception/           # Global handler
    ├── 📂 src/main/resources/
    │   └── application.yml
    ├── 📂 docs/                    # API_Documentation.md, …
    ├── 📜 pom.xml
    └── 📜 .env.example
```

Chi tiết API: xem [`docs/API_Documentation.md`](docs/API_Documentation.md) và [`docs/BACKEND_STATUS.md`](docs/BACKEND_STATUS.md).

---

## 7. Phân công công việc (Contribution)

### Nguyễn Lê Hoàng Khang — Backend · Trưởng nhóm

- Thiết kế cơ sở dữ liệu MySQL, entity JPA, repository.
- Xây dựng **RESTful API** (sản phẩm, đơn hàng, kho, thanh toán, bảo hành).
- **Xác thực & phân quyền:** JWT, OAuth2, RBAC (`ADMIN` / `SALES` / `WAREHOUSE` / `CUSTOMER`).
- Tích hợp **VNPay**, **GHN**, email, Redis; API thống kê admin.
- Swagger, xử lý ngoại lệ, seed dữ liệu & tài liệu backend.

### Phạm Thái Bảo — Frontend

- Thiết kế **UI/UX** storefront và admin (Tailwind, responsive).
- Kết nối API (Axios), quản lý state **Redux** (auth, giỏ hàng, …).
- Khu vực **Admin:** dashboard, đơn hàng, sản phẩm, kho, RBAC UI.
- Tối ưu trải nghiệm: tìm kiếm, autocomplete, luồng checkout.
- CI GitHub Actions (lint + build), cấu trúc component theo module.

### Quy ước làm việc nhóm

- Nhánh feature → Pull Request vào `main`, review chéo.
- Commit message rõ ràng; không commit secret / `.env` thật.
- Đồng bộ contract API qua Swagger + `adminApi.ts` / `api.ts` phía FE.

<!-- Optional: thêm contribution graph khi public repo -->
<!-- ![GitHub contributors](https://contrib.rocks/image?repo=HoangKhangKhuPho4/CD-WEB-BE) -->

---

## 8. Lộ trình phát triển tương lai (Future Enhancements)

- [ ] **Đặt hộ khách (POS)** — nhân viên Sales tạo đơn thay khách tại quầy.
- [ ] **CRM nhẹ** — hồ sơ khách, lịch sử mua, ghi chú chăm sóc.
- [ ] Quản lý **biến thể sản phẩm & gallery ảnh** đầy đủ trên admin.
- [ ] **So sánh thông số kỹ thuật** giữa các sản phẩm.
- [ ] Mở rộng **gợi ý AI** (SVD) theo hành vi duyệt / mua.
- [ ] Deploy production (Vercel + VPS/Render), Docker Compose một lệnh.
- [ ] Thông báo đơn hàng real-time (WebSocket / email template).

---

## 9. Giấy phép và Liên hệ (License & Contact)

### License

Dự án phát hành theo giấy phép **[MIT License](LICENSE)** — được phép sử dụng, sao chép và chỉnh sửa với điều kiện giữ copyright notice.

### Liên hệ

| Thành viên | Email / LinkedIn |
|------------|------------------|
| Nguyễn Lê Hoàng Khang | _Cập nhật email / LinkedIn_ |
| Phạm Thái Bảo | _Cập nhật email / LinkedIn_ |

### Tài liệu tham khảo thêm

- [API Documentation](docs/API_Documentation.md)
- [Backend Status](docs/BACKEND_STATUS.md)
- [Frontend README](https://github.com/HoangKhangKhuPho4/CD-WEB-FE/blob/main/README.md)

---

<p align="center">
  <strong>Bảo Khang Gadget</strong> — Công nghệ & phụ kiện chính hãng<br/>
  <em>Đồ án Website Thương Mại Điện Tử · Nhóm 2 thành viên</em>
</p>
