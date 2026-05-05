@echo off
:: 设置控制台编码为 UTF-8（不重定向输出，避免错误）
chcp 65001

echo ============================================
echo    相机租赁平台 - 中间件启动脚本
echo ============================================
echo.

:: 设置颜色
color 0A

echo [INFO] 开始启动所有中间件服务...
echo.

:: 1. 启动 MySQL
echo [1/6] 正在启动 MySQL...
net start MySQL80
if %errorlevel% equ 0 (
    echo [√] MySQL 启动成功
) else (
    echo [!] MySQL 已在运行或启动失败
)
echo.

:: 2. 启动 Redis
echo [2/6] 正在启动 Redis...
sc query Redis | find "RUNNING" >nul 2>&1
if %errorlevel% neq 0 (
    net start Redis
    if %errorlevel% equ 0 (
        echo [√] Redis 启动成功
    ) else (
        echo [!] Redis 启动失败，请检查 Redis 服务是否安装
    )
) else (
    echo [√] Redis 已在运行
)
echo.

:: 3. 启动 Nacos
echo [3/6] 正在启动 Nacos...
cd /d D:\Program\nacos-server-2.5.2\nacos\bin
if exist startup.cmd (
    start "Nacos Server" cmd /c "startup.cmd -m standalone"
    timeout /t 3 >nul
    echo [√] Nacos 正在启动... (端口: 8848)
    echo     访问地址: http://localhost:8848/nacos
    echo     用户名/密码: nacos/nacos
) else (
    echo [!] 未找到 Nacos 启动脚本，请检查 Nacos 安装路径
)
echo.

:: 4. 启动 MinIO
echo [4/6] 正在启动 MinIO...
cd /d D:\Program\Minio\bin
if exist minio.exe (
    start "MinIO Server" cmd /c "minio.exe server D:\Documents\Minio\data --console-address :9001"
    timeout /t 2 >nul
    echo [√] MinIO 正在启动...
    echo     API 端口: 9000
    echo     控制台地址: http://localhost:9001
    echo     用户名/密码: root/xuan.328
) else (
    echo [!] 未找到 MinIO 可执行文件，请检查 MinIO 安装路径
)
echo.

:: 5. 启动 Sentinel
echo [5/6] 正在启动 Sentinel...
cd /d D:\Program\Sentinel
if exist sentinel-dashboard-1.8.9.jar (
    start "Sentinel Dashboard" cmd /c "java -Dserver.port=8858 -Dcsp.sentinel.dashboard.server=localhost:8858 -Dproject.name=sentinel-dashboard -jar sentinel-dashboard-1.8.9.jar"
    timeout /t 3 >nul
    echo [√] Sentinel 正在启动... (端口: 8858)
    echo     访问地址: http://localhost:8858
    echo     用户名/密码: sentinel/sentinel
) else (
    echo [!] 未找到 Sentinel JAR 文件，请检查 Sentinel 安装路径
)
echo.

:: 6. 启动 RabbitMQ
echo [6/6] 正在启动 RabbitMQ...
sc query RabbitMQ | find "RUNNING" >nul 2>&1
if %errorlevel% neq 0 (
    net start RabbitMQ
    if %errorlevel% equ 0 (
        echo [√] RabbitMQ 启动成功
        echo     管理界面: http://localhost:15672
        echo     用户名/密码: guest/guest
    ) else (
        echo [!] RabbitMQ 启动失败，请检查 RabbitMQ 服务是否安装
    )
) else (
    echo [√] RabbitMQ 已在运行
    echo     管理界面: http://localhost:15672
)
echo.


echo ============================================
echo    所有中间件启动完成！
echo ============================================
echo.
echo 等待服务完全启动 (约 30 秒)...
timeout /t 30 >nul
echo.

echo [服务检查]
echo 正在检查各服务状态...
echo.

:: 检查 MySQL
netstat -ano | findstr ":3306" >nul 2>&1
if %errorlevel% equ 0 (
    echo [√] MySQL (3306) - 运行中
) else (
    echo [×] MySQL (3306) - 未运行
)

:: 检查 Redis
netstat -ano | findstr ":6379" >nul 2>&1
if %errorlevel% equ 0 (
    echo [√] Redis (6379) - 运行中
) else (
    echo [×] Redis (6379) - 未运行
)

:: 检查 Nacos
netstat -ano | findstr ":8848" >nul 2>&1
if %errorlevel% equ 0 (
    echo [√] Nacos (8848) - 运行中
) else (
    echo [×] Nacos (8848) - 未运行
)

:: 检查 MinIO API
netstat -ano | findstr ":9000" >nul 2>&1
if %errorlevel% equ 0 (
    echo [√] MinIO API (9000) - 运行中
) else (
    echo [×] MinIO API (9000) - 未运行
)

:: 检查 MinIO Console
netstat -ano | findstr ":9001" >nul 2>&1
if %errorlevel% equ 0 (
    echo [√] MinIO Console (9001) - 运行中
) else (
    echo [×] MinIO Console (9001) - 未运行
)

:: 检查 Sentinel
netstat -ano | findstr ":8858" >nul 2>&1
if %errorlevel% equ 0 (
    echo [√] Sentinel (8858) - 运行中
) else (
    echo [×] Sentinel (8858) - 未运行
)

:: 检查 RabbitMQ
netstat -ano | findstr ":5672" >nul 2>&1
if %errorlevel% equ 0 (
    echo [√] RabbitMQ (5672) - 运行中
) else (
    echo [×] RabbitMQ (5672) - 未运行
)

echo.
echo ============================================
echo    服务访问地址汇总
echo ============================================
echo.
echo Nacos:        http://localhost:8848/nacos
echo MinIO:        http://localhost:9001
echo Sentinel:     http://localhost:8858
echo RabbitMQ:     http://localhost:15672
echo.
echo ============================================
echo.

echo 按任意键继续...
pause >nul
