# File Excel kiểm kê — Laptop & Desktop

**File:** `kiem-ke-laptop-desktop.xlsx`  
**Danh mục DB:** `Laptop & Desktop` (product_type_id = 12)  
**Tạo lại:** từ thư mục `CD-WEB-FE` chạy `node scripts/generate-inventory-audit-excel.mjs`

## Các sheet

| Sheet | Mục đích |
|-------|----------|
| `Laptop_Desktop_Full` | Toàn bộ 9 thiết bị có Serial/IMEI (mọi trạng thái) |
| `Quet_KiemKe_AVAILABLE` | 6 máy **AVAILABLE** — dùng khi kiểm kê thực tế |
| `Chi_Serial_IMPORT` | Chỉ cột **Serial** — kéo thả vào `/admin/inventory-audit` |

## Cách dùng nhanh

1. Admin → **Kiểm kê kho** → chọn danh mục **Laptop & Desktop** → Bắt đầu
2. Kéo thả sheet `Chi_Serial_IMPORT` hoặc `Quet_KiemKe_AVAILABLE` vào vùng upload Excel
3. **Hoàn tất kiểm đếm** → đối chiếu (kỳ vọng khớp 6/6 nếu quét đủ AVAILABLE)

## Lưu ý

- 3 serial `RESERVED` / `DEFECTIVE` có trong sheet Full nhưng **không** nằm trong sheet quét AVAILABLE.
- Khi tạo phiếu kiểm kê, hệ thống chỉ đếm tồn **AVAILABLE** làm số kỳ vọng.
