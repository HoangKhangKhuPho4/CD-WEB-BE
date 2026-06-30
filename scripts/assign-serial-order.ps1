# Gán Serial tự động cho đơn hàng ORD-20260630083203120
# Chạy từ thư mục CD-WEB-BE:
#   .\scripts\assign-serial-order.ps1
#   .\scripts\assign-serial-order.ps1 -OrderCode "ORD-xxxxxxxx"

param(
    [string]$OrderCode = "ORD-20260630083203120",
    [string]$MySqlUser = "root",
    [string]$MySqlPassword = "",
    [string]$Database = "cd_web",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$sqlFile = Join-Path $repoRoot "src\main\resources\db\assign-serial-for-order.sql"

if (-not (Test-Path $sqlFile)) {
    Write-Error "Không tìm thấy file SQL: $sqlFile"
}

Write-Host "=== Kiểm tra đơn: $OrderCode ===" -ForegroundColor Cyan

$passArg = if ($MySqlPassword) { "-p$MySqlPassword" } else { "" }

$checkSql = @"
USE $Database;
SET @order_code = '$OrderCode';
SELECT o.id, o.order_code, o.status, od.id AS detail_id, od.sku_code, od.quantity,
  (SELECT COUNT(*) FROM order_item_serials ois WHERE ois.order_detail_id = od.id) AS assigned
FROM orders o
JOIN order_details od ON od.order_id = o.id
WHERE o.order_code = @order_code;
"@

mysql -u $MySqlUser $passArg $Database -e $checkSql

if ($DryRun) {
    Write-Host "`nDryRun — chỉ kiểm tra, không gán serial." -ForegroundColor Yellow
    exit 0
}

Write-Host "`n=== Đang gán serial (CALL sp_auto_assign_order_serials) ===" -ForegroundColor Green

$runSql = @"
USE $Database;
SET @order_code = '$OrderCode';
DROP PROCEDURE IF EXISTS sp_auto_assign_order_serials;
"@

# Đọc procedure từ file SQL gốc (từ DROP PROCEDURE đến DELIMITER ;)
$content = Get-Content $sqlFile -Raw
$procStart = $content.IndexOf("DROP PROCEDURE IF EXISTS sp_auto_assign_order_serials")
$procEnd = $content.IndexOf("COMMIT;")
if ($procStart -lt 0 -or $procEnd -lt 0) {
    Write-Error "Không parse được procedure từ $sqlFile"
}
$procBlock = $content.Substring($procStart, $procEnd - $procStart)

$tempFile = [System.IO.Path]::GetTempFileName() + ".sql"
@"
USE $Database;
SET @order_code = '$OrderCode';
$procBlock
COMMIT;
SELECT '=== KẾT QUẢ ===' AS section;
SELECT od.id, od.sku_code, od.quantity, COUNT(ois.id) AS da_gan,
  GROUP_CONCAT(COALESCE(pi.serial_number, pi.imei)) AS serials
FROM orders o
JOIN order_details od ON od.order_id = o.id
LEFT JOIN order_item_serials ois ON ois.order_detail_id = od.id
LEFT JOIN product_items pi ON pi.id = ois.product_item_id
WHERE o.order_code = @order_code
GROUP BY od.id, od.sku_code, od.quantity;
"@ | Set-Content -Path $tempFile -Encoding UTF8

mysql -u $MySqlUser $passArg $Database < $tempFile
Remove-Item $tempFile -Force

Write-Host "`nHoàn tất." -ForegroundColor Green
