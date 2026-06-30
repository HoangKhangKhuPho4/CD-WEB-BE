# Mô phỏng đơn DELIVERED + serial SOLD để test tiếp nhận hàng hoàn
# Chạy: .\scripts\mark-order-delivered-for-return-test.ps1
#      .\scripts\mark-order-delivered-for-return-test.ps1 -OrderCode "ORD-xxxxxxxx"

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
$sqlFile = Join-Path $repoRoot "src\main\resources\db\mark-order-delivered-for-return-test.sql"

if (-not (Test-Path $sqlFile)) {
    Write-Error "Không tìm thấy: $sqlFile"
}

$passArg = if ($MySqlPassword) { "-p$MySqlPassword" } else { "" }

Write-Host "=== Kiểm tra đơn: $OrderCode ===" -ForegroundColor Cyan

$checkSql = @"
USE $Database;
SET @order_code = '$OrderCode' COLLATE utf8mb4_vietnamese_ci;
SELECT o.order_code, o.status, pi.serial_number, pi.status AS serial_status
FROM orders o
LEFT JOIN order_details od ON od.order_id = o.id
LEFT JOIN order_item_serials ois ON ois.order_detail_id = od.id
LEFT JOIN product_items pi ON pi.id = ois.product_item_id
WHERE o.order_code COLLATE utf8mb4_vietnamese_ci = @order_code;
"@

mysql -u $MySqlUser $passArg $Database -e $checkSql

if ($DryRun) {
    Write-Host "`nDryRun — không cập nhật." -ForegroundColor Yellow
    exit 0
}

Write-Host "`n=== Cập nhật DELIVERED + SOLD ===" -ForegroundColor Green

$tempFile = [System.IO.Path]::GetTempFileName() + ".sql"
(Get-Content $sqlFile -Raw) -replace "ORD-20260630083203120", $OrderCode | Set-Content $tempFile -Encoding UTF8

mysql -u $MySqlUser $passArg $Database < $tempFile
Remove-Item $tempFile -Force

Write-Host "`nXong. Quét serial tại /admin/return để test hàng hoàn." -ForegroundColor Green
