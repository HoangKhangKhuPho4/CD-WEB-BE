# Trạng thái Backend CD Web

**Cập nhật:** Hoàn thiện module cấu hình hệ thống, API thống kê legacy, cấu hình AI.

## Base URL

`http://localhost:8080/api`

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Cấu hình công khai (storefront)

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/settings/general` | Public |

## Module đã có logic đầy đủ

| Module | Prefix |
|--------|--------|
| Auth (JWT, OAuth, reset password) | `/api/auth` |
| Sản phẩm (public + admin) | `/api/products`, `/api/admin/products` |
| Danh mục, NSX, coupon, thuộc tính | `/api/admin/...` |
| Giỏ, wishlist, địa chỉ | `/api/cart`, `/api/wishlist`, `/api/addresses` |
| Đơn hàng | `/api/orders`, `/api/admin/orders` |
| Thanh toán | `/api/payment` |
| GHN | `/api/ghn` |
| Kho / IMEI | `/api/admin/inventory` |
| Bảo hành | `/api/admin/warranty` |
| Đánh giá | `/api/reviews`, `/api/admin/reviews` |
| User admin | `/api/admin/users` |
| Thống kê dashboard | `/api/admin/statistics` |
| **Cấu hình hệ thống** | `/api/admin/system` |

## Cấu hình hệ thống (mới)

| Method | Path | Mô tả |
|--------|------|--------|
| GET | `/api/admin/system/general` | Phí ship, freeship, cổng TT, hotline… |
| PUT | `/api/admin/system/general` | Lưu cấu hình chung |
| GET | `/api/admin/system/ai-config` | Tham số SVD, URL AI service, trạng thái retrain |
| PUT | `/api/admin/system/ai-config` | Cập nhật tham số AI |
| POST | `/api/admin/system/ai-retrain` | Gọi `POST {ai-base-url}/retrain` (async) |

Cấu hình mặc định lưu bảng `system_configuration` (id=1).

## Biến môi trường

| Biến | Mặc định |
|------|----------|
| `AI_SERVICE_BASE_URL` | `http://localhost:5000` |
| `APP_BACKEND_URL` | `http://localhost:8080` |
| `APP_FRONTEND_URL` | `http://localhost:3000` |

## Phụ thuộc ngoài (runtime)

- MySQL `cd_web`
- Redis (cache/session nếu dùng)
- Mail SMTP (quên mật khẩu)
- Flask AI service (gợi ý + retrain) — tùy chọn, lỗi mạng không làm sập API shop

## Ghi chú

- API legacy `/api/admin/statistics/revenue` và `/top-products-legacy` đã trả dữ liệu thật (delegate sang API mới).
- Khuyến nghị FE dùng `/overview`, `/revenue/chart`, `/top-products`, v.v.
