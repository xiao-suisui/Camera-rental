@echo off
chcp 65001 >nul
echo ============================================================
echo   相机租赁平台 - 中间件停止脚本
echo   Camera Rental Platform - Middleware Shutdown Script
echo ============================================================
echo.

echo [1/5] 停止 Nacos...
taskkill /F /FI "WINDOWTITLE eq Nacos*" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Nacos 已停止
) else (
    echo ⚠️  Nacos 可能未运行
)

echo.
echo [2/5] 停止 Sentinel Dashboard...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8858"') do (
    taskkill /F /PID %%a >nul 2>&1
)
echo ✅ Sentinel Dashboard 已停止

echo.
echo [3/5] 停止 MinIO...
taskkill /F /FI "WINDOWTITLE eq MinIO*" >nul 2>&1
taskkill /F /IM minio.exe >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ MinIO 已停止
) else (
    echo ⚠️  MinIO 可能未运行
)

echo.
echo [4/5] 停止 MySQL (可选)...
echo ⚠️  MySQL 作为系统服务运行，建议保持运行状态
echo    如需停止，请执行: net stop MySQL80
REM net stop MySQL80 >nul 2>&1

echo.
echo [5/5] 检查端口状态...
timeout /t 2 /nobreak >nul

netstat -ano | findstr ":8848" >nul 2>&1
if %errorlevel% neq 0 (
    echo ✅ Nacos      [8848]  已停止
) else (
    echo ⚠️  Nacos      [8848]  仍在运行
)

netstat -ano | findstr ":8858" >nul 2>&1
if %errorlevel% neq 0 (
    echo ✅ Sentinel   [8858]  已停止
) else (
    echo ⚠️  Sentinel   [8858]  仍在运行
)

netstat -ano | findstr ":9000" >nul 2>&1
if %errorlevel% neq 0 (
    echo ✅ MinIO      [9000]  已停止
) else (
    echo ⚠️  MinIO      [9000]  仍在运行
)

echo.
echo ============================================================
echo   中间件停止完成！
echo ============================================================
echo.
pause
