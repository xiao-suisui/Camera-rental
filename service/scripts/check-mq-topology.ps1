param(
    [string]$HostName = "localhost",
    [int]$ManagementPort = 15672,
    [string]$Username = "guest",
    [string]$Password = "guest",
    [string]$VirtualHost = "/camera-rental",
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "[CHECK] $Message"
}

function Write-Pass {
    param([string]$Message)
    Write-Host "[PASS]  $Message" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Message)
    Write-Host "[FAIL]  $Message" -ForegroundColor Red
}

function Get-BasicAuthHeader {
    param(
        [string]$User,
        [string]$Pass
    )
    $pair = "{0}:{1}" -f $User, $Pass
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($pair)
    $token = [Convert]::ToBase64String($bytes)
    return @{ Authorization = "Basic $token" }
}

function Encode-Segment {
    param([string]$Value)
    return [System.Uri]::EscapeDataString($Value)
}

function Test-ExchangeExists {
    param(
        [string]$BaseUrl,
        [hashtable]$Headers,
        [string]$VHost,
        [string]$Exchange
    )

    $vhostEncoded = Encode-Segment -Value $VHost
    $exchangeEncoded = Encode-Segment -Value $Exchange
    $url = "$BaseUrl/exchanges/$vhostEncoded/$exchangeEncoded"

    try {
        $null = Invoke-RestMethod -Uri $url -Headers $Headers -Method Get
        return $true
    } catch {
        return $false
    }
}

$expected = @{
    Exchanges = @(
        "camera.rental.order.topic.v1",
        "camera.rental.order.dlx.v1"
    )
    Queues = @(
        "camera.rental.user.notify.q",
        "camera.rental.user.notify.q.dlq",
        "camera.rental.device.sync.q",
        "camera.rental.device.sync.q.dlq",
        "camera.rental.coupon.sync.q",
        "camera.rental.coupon.sync.q.dlq"
    )
    Bindings = @(
        @{ Exchange = "camera.rental.order.topic.v1"; Queue = "camera.rental.user.notify.q"; RoutingKey = "order.*.notify.user" },
        @{ Exchange = "camera.rental.order.topic.v1"; Queue = "camera.rental.device.sync.q"; RoutingKey = "order.cancelled.release.stock" },
        @{ Exchange = "camera.rental.order.topic.v1"; Queue = "camera.rental.coupon.sync.q"; RoutingKey = "order.cancelled.rollback.coupon" },
        @{ Exchange = "camera.rental.order.dlx.v1"; Queue = "camera.rental.user.notify.q.dlq"; RoutingKey = "order.notify.user.dead" },
        @{ Exchange = "camera.rental.order.dlx.v1"; Queue = "camera.rental.device.sync.q.dlq"; RoutingKey = "order.cancelled.release.stock.dead" },
        @{ Exchange = "camera.rental.order.dlx.v1"; Queue = "camera.rental.coupon.sync.q.dlq"; RoutingKey = "order.cancelled.rollback.coupon.dead" }
    )
}

Write-Step "Target RabbitMQ management endpoint: http://$HostName`:$ManagementPort"
Write-Step "Virtual host: $VirtualHost"

if ($DryRun) {
    Write-Step "DryRun enabled: no API request will be sent."
    Write-Host "Expected exchanges:"; $expected.Exchanges | ForEach-Object { Write-Host " - $_" }
    Write-Host "Expected queues:"; $expected.Queues | ForEach-Object { Write-Host " - $_" }
    Write-Host "Expected bindings:";
    $expected.Bindings | ForEach-Object {
        Write-Host (" - {0} -> {1} ({2})" -f $_.Exchange, $_.Queue, $_.RoutingKey)
    }
    exit 0
}

$headers = Get-BasicAuthHeader -User $Username -Pass $Password
$vhostEncoded = Encode-Segment -Value $VirtualHost
$base = "http://$HostName`:$ManagementPort/api"

$failed = $false

foreach ($exchange in $expected.Exchanges) {
    $exchangeEncoded = Encode-Segment -Value $exchange
    $url = "$base/exchanges/$vhostEncoded/$exchangeEncoded"
    try {
        $null = Invoke-RestMethod -Uri $url -Headers $headers -Method Get
        Write-Pass "Exchange exists: $exchange"
    } catch {
        Write-Fail "Exchange missing or inaccessible: $exchange"
        $failed = $true
    }
}

foreach ($queue in $expected.Queues) {
    $queueEncoded = Encode-Segment -Value $queue
    $url = "$base/queues/$vhostEncoded/$queueEncoded"
    try {
        $null = Invoke-RestMethod -Uri $url -Headers $headers -Method Get
        Write-Pass "Queue exists: $queue"
    } catch {
        Write-Fail "Queue missing or inaccessible: $queue"
        $failed = $true
    }
}

foreach ($binding in $expected.Bindings) {
    $exchangeEncoded = Encode-Segment -Value $binding.Exchange
    $queueEncoded = Encode-Segment -Value $binding.Queue
    $url = "$base/bindings/$vhostEncoded/e/$exchangeEncoded/q/$queueEncoded"

    try {
        $bindingList = Invoke-RestMethod -Uri $url -Headers $headers -Method Get
        $matched = $false

        foreach ($b in $bindingList) {
            if ($b.routing_key -eq $binding.RoutingKey) {
                $matched = $true
                break
            }
        }

        if ($matched) {
            Write-Pass ("Binding exists: {0} -> {1} ({2})" -f $binding.Exchange, $binding.Queue, $binding.RoutingKey)
        } else {
            Write-Fail ("Binding missing routing key: {0} -> {1} ({2})" -f $binding.Exchange, $binding.Queue, $binding.RoutingKey)
            $failed = $true
        }
    } catch {
        Write-Fail ("Binding query failed: {0} -> {1}" -f $binding.Exchange, $binding.Queue)
        $failed = $true
    }
}

if ($failed) {
    Write-Host ""
    if ($VirtualHost -ne "/") {
        $probe = Test-ExchangeExists -BaseUrl $base -Headers $headers -VHost "/" -Exchange $expected.Exchanges[0]
        if ($probe) {
            Write-Host "[HINT] Topology appears to exist in vhost '/' instead of '$VirtualHost'." -ForegroundColor Yellow
            Write-Host "[HINT] Retry with: .\\service\\scripts\\check-mq-topology.ps1 -VirtualHost /" -ForegroundColor Yellow
        }
    }
    Write-Fail "RabbitMQ topology check failed."
    exit 1
}

Write-Host ""
Write-Pass "RabbitMQ topology check passed."
exit 0
