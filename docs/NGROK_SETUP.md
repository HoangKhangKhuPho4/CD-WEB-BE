# Hướng dẫn cấu hình Ngrok — Bảo Khang Gadget (Backend)

> **Dự án:** CD-WEB-BE · **Nhóm:** Nguyễn Lê Hoàng Khang (22130116) & Phạm Thái Bảo (22130025)  
> **Mục đích:** Expose backend local (`localhost:8080`) ra internet để test **VNPay**, **MoMo**, webhook và URL ảnh public.

---

## 1. Ngrok dùng để làm gì?

Backend chạy trên máy dev chỉ nghe `http://localhost:8080` — **VNPay / MoMo / webhook** trên internet **không gọi được** vào đó.

**Ngrok** tạo tunnel:

```text
https://xxxx.ngrok-free.app  ──►  http://localhost:8080  (Spring Boot)
```

Trong project, URL ngrok được gán vào **`app.backend-url`** trong `application.yml`. Các module sau **tự lấy** URL này:

| Chức năng | URL sinh ra |
|-----------|-------------|
| VNPay Return (user quay lại sau thanh toán) | `{backend-url}/api/payment/vnpay/return` |
| VNPay IPN (server VNPay gọi — cấu hình thêm trên portal VNPay) | `{backend-url}/api/payment/vnpay/ipn` |
| MoMo Return / IPN | `{backend-url}/api/payment/momo/return` · `.../ipn` |
| URL ảnh sản phẩm trong API JSON | `{backend-url}/img/{id}` |

> **Lưu ý:** Backend **không** cài thư viện ngrok. Ngrok chạy **riêng** trong terminal; Spring Boot chỉ **lưu URL** trong config.

---

## 2. Khi nào cần / không cần Ngrok?

| Tình huống | Cần ngrok? | `app.backend-url` gợi ý |
|------------|------------|-------------------------|
| Xem sản phẩm, đăng nhập, API trên cùng máy | ❌ Không | `http://localhost:8080` |
| Test **VNPay / MoMo** sandbox | ✅ Có | URL ngrok hiện tại |
| Demo cho GV / điện thoại khác mạng | ✅ Có | URL ngrok |
| Production (VPS / Render) | ❌ Không | Domain thật (vd. `https://api.example.com`) |

---

## 3. Cài đặt Ngrok (Windows)

### 3.1. Tải & cài

1. Vào [https://ngrok.com/download](https://ngrok.com/download)
2. Tải bản **Windows (MSIX hoặc ZIP)**
3. Đăng ký tài khoản miễn phí (email sinh viên được)

### 3.2. Lấy Authtoken

1. Đăng nhập [https://dashboard.ngrok.com/get-started/your-authtoken](https://dashboard.ngrok.com/get-started/your-authtoken)
2. Bấm **Copy** authtoken (chuỗi thường **bắt đầu bằng `2`**, không có ký tự `$`)

### 3.3. Gắn Authtoken (PowerShell)

**Bắt buộc** bọc token trong dấu ngoặc kép `"..."` — tránh PowerShell hiểu nhầm `$`:

```powershell
ngrok config add-authtoken "DÁN_AUTHTOKEN_Ở_ĐÂY"
```

Kết quả mong đợi:

```text
Authtoken saved to configuration file: C:\Users\<TênBạn>\AppData\Local\ngrok\ngrok.yml
```

#### Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách sửa |
|-----|-------------|----------|
| `ERR_NGROK_105` — token không hợp lệ | Copy nhầm / thừa `$` ở đầu token | Lấy token mới trên dashboard; dùng `"..."` |
| `ERR_NGROK_3200` — endpoint offline | Tunnel ngrok đã tắt | Chạy lại `ngrok http 8080` |

---

## 4. Quy trình chạy (mỗi lần test thanh toán)

### Bước 1 — Chạy Backend

IntelliJ IDEA: Run **`CdWebBeApplication`**  
Hoặc terminal:

```bash
cd CD-WEB-BE
mvn spring-boot:run
```

Kiểm tra: [http://localhost:8080/api/products](http://localhost:8080/api/products) trả JSON.

### Bước 2 — Mở tunnel Ngrok

**Terminal mới** (giữ cửa sổ này mở):

```powershell
ngrok http 8080
```

Output mẫu:

```text
Session Status                online
Forwarding                    https://bbe1-115-78-231-45.ngrok-free.app -> http://localhost:8080
Web Interface                 http://127.0.0.1:4040
```

Copy URL **`https://....ngrok-free.app`** (không có `/` ở cuối).

### Bước 3 — Cập nhật `application.yml`

File: **`src/main/resources/application.yml`**

```yaml
app:
  frontend-url: "http://localhost:3000"
  backend-url: "https://bbe1-115-78-231-45.ngrok-free.app"   # ← URL ngrok MỚI
  server:
    url: "${app.backend-url}"
```

Chỉ cần sửa **một dòng** `backend-url`. Các dòng sau **tự động** theo:

- `vnpay.return-url`
- `momo.returnUrl` / `momo.ipnUrl`
- URL ảnh trong API (`ProductService`, …)

### Bước 4 — Restart Backend

Stop → Run lại **`CdWebBeApplication`** để config có hiệu lực.

### Bước 5 — Kiểm tra

| Kiểm tra | URL |
|----------|-----|
| API qua ngrok | `https://[ngrok-url]/api/products` |
| Giao diện debug ngrok | [http://127.0.0.1:4040](http://127.0.0.1:4040) |

### Bước 6 — Test VNPay

1. FE: `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` (giữ nguyên — FE vẫn gọi API local)
2. Tạo **đơn hàng mới** → chọn VNPay
3. Thanh toán sandbox → VNPay redirect về `{backend-url}/api/payment/vnpay/return`
4. Backend xử lý → redirect về FE `/checkout/result`

> **Quan trọng:** Link VNPay của **đơn cũ** vẫn trỏ Return URL cũ. Sau khi đổi ngrok, phải **thanh toán đơn mới**.

---

## 5. Restart Backend vs Restart Ngrok

| Hành động | URL ngrok đổi? | Sửa `application.yml`? |
|-----------|----------------|------------------------|
| Restart Backend (Stop/Run IntelliJ) | ❌ Không | ❌ Không |
| Giữ ngrok chạy, restart Backend nhiều lần | ❌ Không | ❌ Không |
| **Tắt ngrok** (Ctrl+C) rồi mở lại `ngrok http 8080` | ✅ **Có** (gói Free) | ✅ **Có** — cập nhật tay `backend-url` |

**Gói Free:** mỗi lần mở lại ngrok thường có **domain mới** → phải sửa config + restart BE.

**Gói trả phí:** có thể giữ domain cố định:

```powershell
ngrok http --url ten-cua-ban.ngrok.app 8080
```

---

## 6. Cấu hình VNPay IPN (tuỳ chọn, khuyến nghị)

**Return URL** đã cấu hình trong `application.yml`.

**IPN URL** (VNPay server gọi ngược) thường khai báo trên **cổng merchant VNPay Sandbox**:

```text
https://[ngrok-url-cua-ban]/api/payment/vnpay/ipn
```

Cập nhật IPN mỗi khi URL ngrok đổi.

---

## 7. Frontend (CD-WEB-FE)

Dev thường **không** cần đổi FE khi bật ngrok:

```env
# .env.local
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

FE gọi API trực tiếp `localhost:8080`. Chỉ **VNPay redirect** (sau thanh toán) đi qua URL ngrok trên backend.

Nếu FE chạy HTTPS (`npm run dev --experimental-https`), đảm bảo backend CORS cho phép `https://localhost:3000`.

---

## 8. Xử lý sự cố (Troubleshooting)

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|------------|
| Trang `ERR_NGROK_3200` sau VNPay | Tunnel offline / URL cũ trong yml | Chạy `ngrok http 8080`; cập nhật `backend-url` |
| Ảnh sản phẩm không hiện | DB/API trả URL ngrok cũ | Cập nhật `backend-url` + restart BE |
| `ERR_NGROK_105` khi chạy ngrok | Authtoken sai | `ngrok config add-authtoken "..."` với token mới |
| `/api/products` local OK, ngrok lỗi | BE chưa chạy hoặc sai port | Kiểm tra port 8080; chạy `ngrok http 8080` |
| VNPay OK nhưng đơn không cập nhật | IPN chưa cấu hình / ngrok tắt | Cấu hình IPN trên VNPay; giữ ngrok online |

---

## 9. Bảo mật

- **Không** commit authtoken ngrok lên GitHub.
- **Không** chia sẻ authtoken trong chat / screenshot công khai.
- Nếu token lộ → [dashboard.ngrok.com](https://dashboard.ngrok.com/) → **Revoke** → tạo token mới.
- File config ngrok: `%LOCALAPPDATA%\ngrok\ngrok.yml`

---

## 10. Checklist nhanh

```text
□ MySQL + Redis đang chạy
□ CdWebBeApplication chạy port 8080
□ ngrok http 8080 — Session Status: online
□ application.yml → app.backend-url = URL Forwarding mới
□ Restart Backend
□ Mở https://[ngrok]/api/products → có JSON
□ Tạo đơn MỚI → test VNPay
□ Giữ terminal ngrok MỞ suốt phiên test
```

---

## 11. Quay về dev local (không ngrok)

Khi không test thanh toán, đặt lại:

```yaml
app:
  backend-url: "http://localhost:8080"
```

Restart Backend. Có thể tắt ngrok (Ctrl+C).

---

## Liên kết liên quan

- [README.md](../README.md) — Hướng dẫn cài đặt tổng thể
- [application.yml](../src/main/resources/application.yml) — Config `app.backend-url`
- [Ngrok Docs — ERR_NGROK_105](https://ngrok.com/docs/errors/err_ngrok_105)
- [Ngrok Docs — ERR_NGROK_3200](https://ngrok.com/docs/errors/err_ngrok_3200)
