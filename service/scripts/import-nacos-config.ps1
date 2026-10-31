# ============================================
# Nacos 配置导入脚本
# 将 nacos_config/ 下的配置导入到 Nacos 配置中心
#
# 用法（在项目根或任意目录执行）：
#   .\service\scripts\import-nacos-config.ps1                    # 导入到 public namespace（默认）
#   .\service\scripts\import-nacos-config.ps1 -Namespace b87db654-8323-4c9f-ba55-66ce6d0622be
#   .\service\scripts\import-nacos-config.ps1 -NacosAddr 192.168.1.100:8848
# ============================================

param(
    [string]$NacosAddr = "localhost:8848",
    [string]$Namespace = ""    # 空字符串 = public（默认 namespace）
)

$ErrorActionPreference = "Stop"

# 配置目录：service/scripts/ 的上两级 = 项目根/nacos_config
$configRoot = Join-Path $PSScriptRoot "..\..\nacos_config"

if (-not (Test-Path $configRoot)) {
    Write-Error "未找到配置目录：$configRoot"
    exit 1
}

$groups = @("SHARED_GROUP", "DEFAULT_GROUP")
$success = 0
$failed = 0

foreach ($g in $groups) {
    $dir = Join-Path $configRoot $g
    if (-not (Test-Path $dir)) {
        Write-Output "[WARN] 目录不存在，跳过：$dir"
        continue
    }

    Get-ChildItem $dir -Filter "*.yaml" | Sort-Object Name | ForEach-Object {
        $dataId = $_.Name
        $content = Get-Content $_.FullName -Raw -Encoding UTF8

        $body = @{
            dataId  = $dataId
            group   = $g
            content = $content
            type    = "yaml"
        }
        if ($Namespace -ne "") { $body["tenant"] = $Namespace }

        $url = "http://${NacosAddr}/nacos/v1/cs/configs"
        try {
            $resp = Invoke-RestMethod -Uri $url -Method Post -Body $body -ContentType "application/x-www-form-urlencoded" -TimeoutSec 30
            if ($resp -eq "true") {
                Write-Output ("[OK]   {0}/{1}" -f $g, $dataId)
                $success++
            } else {
                Write-Output ("[FAIL] {0}/{1} -> {2}" -f $g, $dataId, $resp)
                $failed++
            }
        } catch {
            Write-Output ("[ERR]  {0}/{1} -> {2}" -f $g, $dataId, $_.Exception.Message)
            $failed++
        }
    }
}

Write-Output ""
Write-Output ("导入完成：成功 {0} 个，失败 {1} 个" -f $success, $failed)
