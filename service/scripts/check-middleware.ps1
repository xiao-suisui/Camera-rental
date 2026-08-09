param(
    [string]$MySqlHost = "localhost",
    [int]$MySqlPort = 3306,
    [string]$RedisHost = "localhost",
    [int]$RedisPort = 6379,
    [string]$NacosUrl = "http://localhost:8848/nacos",
    [string]$RabbitHost = "localhost",
    [int]$RabbitPort = 5672,
    [string]$RabbitManagementUrl = "http://localhost:15672",
    [switch]$Strict
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Test-TcpPort {
    param(
        [string]$Host,
        [int]$Port,
        [int]$TimeoutMs = 2000
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect($Host, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
            return $false
        }
        $client.EndConnect($async)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Test-HttpEndpoint {
    param(
        [string]$Url,
        [int]$TimeoutSec = 5
    )

    try {
        $resp = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec $TimeoutSec -UseBasicParsing
        return ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 400)
    } catch {
        return $false
    }
}

$results = @()

$results += [PSCustomObject]@{
    Name = "MySQL"
    Target = "${MySqlHost}:$MySqlPort"
    Passed = (Test-TcpPort -Host $MySqlHost -Port $MySqlPort)
}

$results += [PSCustomObject]@{
    Name = "Redis"
    Target = "${RedisHost}:$RedisPort"
    Passed = (Test-TcpPort -Host $RedisHost -Port $RedisPort)
}

$results += [PSCustomObject]@{
    Name = "Nacos HTTP"
    Target = $NacosUrl
    Passed = (Test-HttpEndpoint -Url $NacosUrl)
}

$results += [PSCustomObject]@{
    Name = "RabbitMQ AMQP"
    Target = "${RabbitHost}:$RabbitPort"
    Passed = (Test-TcpPort -Host $RabbitHost -Port $RabbitPort)
}

$results += [PSCustomObject]@{
    Name = "RabbitMQ Management"
    Target = $RabbitManagementUrl
    Passed = (Test-HttpEndpoint -Url $RabbitManagementUrl)
}

$anyFail = $false
Write-Host "Middleware health check summary:" 
foreach ($item in $results) {
    if ($item.Passed) {
        Write-Host ("[PASS] {0,-20} {1}" -f $item.Name, $item.Target) -ForegroundColor Green
    } else {
        Write-Host ("[FAIL] {0,-20} {1}" -f $item.Name, $item.Target) -ForegroundColor Red
        $anyFail = $true
    }
}

if ($anyFail) {
    if ($Strict) {
        exit 1
    }
    exit 0
}

exit 0
