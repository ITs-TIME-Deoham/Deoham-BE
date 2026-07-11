# Monitoring Stack Validation Script
# Phase 7: Validation and Optimization
# Usage: .\validate-stack.ps1

param(
    [switch]$Verbose = $false,
    [int]$Timeout = 60  # Timeout in seconds for each check
)

$ErrorActionPreference = "Continue"
$SuccessCount = 0
$FailureCount = 0
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

# Color output functions
function Write-Success {
    param([string]$message)
    Write-Host "[$timestamp] [✓] $message" -ForegroundColor Green
    $script:SuccessCount++
}

function Write-Error {
    param([string]$message)
    Write-Host "[$timestamp] [✗] $message" -ForegroundColor Red
    $script:FailureCount++
}

function Write-Info {
    param([string]$message)
    Write-Host "[$timestamp] [i] $message" -ForegroundColor Cyan
}

function Write-Warning {
    param([string]$message)
    Write-Host "[$timestamp] [!] $message" -ForegroundColor Yellow
}

function Test-ServiceHealth {
    param(
        [string]$ServiceName,
        [string]$Url,
        [int]$ExpectedStatus = 200
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq $ExpectedStatus) {
            Write-Success "$ServiceName is healthy (HTTP $($response.StatusCode))"
            return $true
        } else {
            Write-Error "$ServiceName returned unexpected status code: $($response.StatusCode)"
            return $false
        }
    } catch {
        Write-Error "$ServiceName health check failed: $($_.Exception.Message)"
        return $false
    }
}

function Test-DockerContainer {
    param(
        [string]$ContainerName,
        [string]$HealthUrl,
        [int]$ExpectedStatus = 200
    )

    try {
        # Check if container is running
        $running = & docker ps --format "{{.Names}}" | Select-String $ContainerName
        if (-not $running) {
            Write-Error "$ContainerName container is not running"
            return $false
        }

        Write-Info "$ContainerName container is running"

        # Check container health
        if ($HealthUrl) {
            return Test-ServiceHealth -ServiceName $ContainerName -Url $HealthUrl -ExpectedStatus $ExpectedStatus
        }
        return $true
    } catch {
        Write-Error "Failed to check $ContainerName: $($_.Exception.Message)"
        return $false
    }
}

function Get-ContainerMetrics {
    param([string]$ContainerName)

    try {
        $stats = & docker stats --no-stream --format "table {{.CPUPerc}}\t{{.MemUsage}}" $ContainerName
        return $stats
    } catch {
        Write-Error "Failed to get metrics for $ContainerName"
        return $null
    }
}

# ============================================================================
# PHASE 7 VALIDATION CHECKS
# ============================================================================

Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         DEOHAM MONITORING STACK VALIDATION (PHASE 7)          ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# 1. Docker Compose Services Check
Write-Host "`n[1] DOCKER COMPOSE SERVICES CHECK" -ForegroundColor Yellow
Write-Host "─" * 64

try {
    $containers = @("postgres", "redis", "prometheus", "loki", "promtail", "grafana", "app")

    foreach ($container in $containers) {
        Test-DockerContainer -ContainerName "deoham-be-$container-1" -HealthUrl $null | Out-Null
    }
} catch {
    Write-Error "Docker check failed: $($_.Exception.Message)"
}

# 2. Prometheus Validation
Write-Host "`n[2] PROMETHEUS METRICS VALIDATION" -ForegroundColor Yellow
Write-Host "─" * 64

if (Test-ServiceHealth -ServiceName "Prometheus" -Url "http://localhost:19090" -ExpectedStatus 200) {
    # Check metrics endpoint
    try {
        $metrics = Invoke-WebRequest -Uri "http://localhost:19090/api/v1/query?query=up" -TimeoutSec 5 -ErrorAction SilentlyContinue
        $metricsData = $metrics.Content | ConvertFrom-Json

        if ($metricsData.status -eq "success") {
            $resultCount = $metricsData.data.result.Count
            Write-Success "Prometheus metrics endpoint responding (found $resultCount series)"

            # Check application metrics
            $appMetrics = Invoke-WebRequest -Uri "http://localhost:19090/api/v1/query?query=process_cpu_usage" -TimeoutSec 5 -ErrorAction SilentlyContinue
            $appData = $appMetrics.Content | ConvertFrom-Json

            if ($appData.data.result.Count -gt 0) {
                Write-Success "Application metrics are being collected"
            } else {
                Write-Warning "Application metrics not found (app may not have started yet)"
            }
        }
    } catch {
        Write-Error "Failed to query Prometheus metrics: $($_.Exception.Message)"
    }

    # Check alert rules
    try {
        $rules = Invoke-WebRequest -Uri "http://localhost:19090/api/v1/rules" -TimeoutSec 5 -ErrorAction SilentlyContinue
        $rulesData = $rules.Content | ConvertFrom-Json
        $ruleCount = $rulesData.data.groups.rules.Count
        Write-Success "Alert rules loaded: $ruleCount rules"
    } catch {
        Write-Error "Failed to fetch alert rules: $($_.Exception.Message)"
    }
} else {
    Write-Error "Prometheus is not responding"
}

# 3. Loki Log Aggregation Validation
Write-Host "`n[3] LOKI LOG AGGREGATION VALIDATION" -ForegroundColor Yellow
Write-Host "─" * 64

if (Test-ServiceHealth -ServiceName "Loki" -Url "http://localhost:3100/ready" -ExpectedStatus 200) {
    # Check log ingestion
    try {
        $lokiQuery = '{job="promtail"}'
        $logQuery = Invoke-WebRequest -Uri "http://localhost:3100/loki/api/v1/query?query=$([System.Uri]::EscapeDataString($lokiQuery))" -TimeoutSec 5 -ErrorAction SilentlyContinue
        $logData = $logQuery.Content | ConvertFrom-Json

        if ($logData.data.result.Count -gt 0) {
            Write-Success "Logs are being ingested into Loki"

            # Check log volume
            $streamCount = $logData.data.result.Count
            Write-Success "Found $streamCount log streams"
        } else {
            Write-Warning "No logs found in Loki yet (check if application is running)"
        }
    } catch {
        Write-Error "Failed to query Loki logs: $($_.Exception.Message)"
    }
} else {
    Write-Error "Loki is not responding"
}

# 4. Promtail Log Collection Validation
Write-Host "`n[4] PROMTAIL LOG COLLECTION VALIDATION" -ForegroundColor Yellow
Write-Host "─" * 64

if (Test-DockerContainer -ContainerName "deoham-be-promtail-1" -HealthUrl $null) {
    # Check if logs file exists
    $logFile = "./logs/application.log"
    if (Test-Path $logFile) {
        $fileSize = (Get-Item $logFile).Length / 1MB
        Write-Success "Application log file exists (size: $([Math]::Round($fileSize, 2)) MB)"

        # Check log content
        $logLines = @(Get-Content $logFile -Tail 5)
        if ($logLines.Count -gt 0) {
            Write-Success "Recent log entries found:"
            $logLines | ForEach-Object { Write-Info "  $_" }
        }
    } else {
        Write-Warning "Application log file not found at $logFile"
    }
} else {
    Write-Error "Promtail container is not running"
}

# 5. Grafana Dashboard Validation
Write-Host "`n[5] GRAFANA DASHBOARD VALIDATION" -ForegroundColor Yellow
Write-Host "─" * 64

if (Test-ServiceHealth -ServiceName "Grafana" -Url "http://localhost:3001/api/health" -ExpectedStatus 200) {
    # Check dashboards
    try {
        $dashboards = Invoke-WebRequest -Uri "http://localhost:3001/api/search?query=" `
                                       -Headers @{"Authorization" = "Basic $(([Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes('admin:admin'))))"} `
                                       -TimeoutSec 5 -ErrorAction SilentlyContinue
        $dashData = $dashboards.Content | ConvertFrom-Json
        $dashCount = $dashData.Count
        Write-Success "Dashboards found: $dashCount"

        $dashData | ForEach-Object {
            Write-Info "  - $($_.title)"
        }
    } catch {
        Write-Error "Failed to fetch Grafana dashboards: $($_.Exception.Message)"
    }

    # Check data sources
    try {
        $datasources = Invoke-WebRequest -Uri "http://localhost:3001/api/datasources" `
                                        -Headers @{"Authorization" = "Basic $(([Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes('admin:admin'))))"} `
                                        -TimeoutSec 5 -ErrorAction SilentlyContinue
        $dsData = $datasources.Content | ConvertFrom-Json
        Write-Success "Data sources configured: $($dsData.Count)"

        $dsData | ForEach-Object {
            Write-Info "  - $($_.name) [$($_.type)]"
        }
    } catch {
        Write-Error "Failed to fetch Grafana data sources: $($_.Exception.Message)"
    }
} else {
    Write-Error "Grafana is not responding"
}

# 6. Application Metrics Validation
Write-Host "`n[6] APPLICATION METRICS VALIDATION" -ForegroundColor Yellow
Write-Host "─" * 64

try {
    $metrics = Invoke-WebRequest -Uri "http://localhost:8080/actuator/prometheus" -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($metrics.StatusCode -eq 200) {
        $metricLines = $metrics.Content -split "`n"
        $activeMetrics = @($metricLines | Where-Object { $_ -notmatch "^#" -and $_ -ne "" })
        Write-Success "Application metrics endpoint is responding"
        Write-Success "Total metrics: $($activeMetrics.Count)"

        # Sample metrics
        Write-Info "Sample metrics:"
        $activeMetrics | Select-Object -First 5 | ForEach-Object {
            Write-Info "  $_"
        }
    }
} catch {
    Write-Error "Application metrics endpoint not available: $($_.Exception.Message)"
}

# 7. Resource Usage Check
Write-Host "`n[7] RESOURCE USAGE CHECK" -ForegroundColor Yellow
Write-Host "─" * 64

$containers = @("postgres", "redis", "prometheus", "loki", "promtail", "grafana", "app")

foreach ($container in $containers) {
    try {
        $containerName = "deoham-be-$container-1"
        $running = & docker ps --format "{{.Names}}" | Select-String $containerName

        if ($running) {
            $stats = & docker stats --no-stream --format "{{.CPUPerc}} CPU, {{.MemUsage}} Memory" $containerName 2>$null
            Write-Info "$containerName: $stats"
        }
    } catch {
        # Silently continue if docker stats fails
    }
}

# 8. Data Flow Validation
Write-Host "`n[8] DATA FLOW VALIDATION" -ForegroundColor Yellow
Write-Host "─" * 64

Write-Info "Data flow paths:"
Write-Info "  [App] → /actuator/prometheus → [Prometheus]"
Write-Info "  [App] → logs/application.log → [Promtail] → [Loki]"
Write-Info "  [Prometheus] → Alert Rules → [Alerts]"
Write-Info "  [Prometheus/Loki] → [Grafana] → Dashboards"

try {
    # Test complete flow
    $promTest = Invoke-WebRequest -Uri "http://localhost:19090/api/v1/query?query=up" -TimeoutSec 5 -ErrorAction SilentlyContinue
    $lokiTest = Invoke-WebRequest -Uri "http://localhost:3100/ready" -TimeoutSec 5 -ErrorAction SilentlyContinue
    $grafanaTest = Invoke-WebRequest -Uri "http://localhost:3001/api/health" -TimeoutSec 5 -ErrorAction SilentlyContinue

    if ($promTest.StatusCode -eq 200 -and $lokiTest.StatusCode -eq 200 -and $grafanaTest.StatusCode -eq 200) {
        Write-Success "Complete data flow path is operational"
    }
} catch {
    Write-Error "Data flow validation failed: $($_.Exception.Message)"
}

# 9. Performance Baseline
Write-Host "`n[9] PERFORMANCE BASELINE" -ForegroundColor Yellow
Write-Host "─" * 64

try {
    # Query response time
    $start = Get-Date
    $query = Invoke-WebRequest -Uri "http://localhost:19090/api/v1/query?query=up" -TimeoutSec 5 -ErrorAction SilentlyContinue
    $elapsed = (Get-Date) - $start
    Write-Info "Prometheus query time: $($elapsed.TotalMilliseconds)ms"

    if ($elapsed.TotalSeconds -lt 2) {
        Write-Success "Query response time is optimal (< 2s)"
    } else {
        Write-Warning "Query response time is higher than optimal"
    }
} catch {
    Write-Error "Performance baseline check failed"
}

# Summary
Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                      VALIDATION SUMMARY                        ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Host "`nResults:"
Write-Host "  Passed:  $SuccessCount" -ForegroundColor Green
Write-Host "  Failed:  $FailureCount" -ForegroundColor Red
Write-Host "  Total:   $($SuccessCount + $FailureCount)"

if ($FailureCount -eq 0) {
    Write-Host "`n✓ All validation checks passed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n✗ Some validation checks failed. Please review the errors above." -ForegroundColor Red
    exit 1
}
