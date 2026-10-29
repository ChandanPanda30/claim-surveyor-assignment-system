# Reports the state of the node: service, active release, health endpoint and the environment label.
#   .\puppet\check.ps1
param([int]$Port = 8084)

Write-Host "== Service status (inside the node)"
docker exec claim-node /opt/claim-surveyor/bin/claim-surveyor.sh status
if ($LASTEXITCODE -eq 0) { Write-Host "running" } else { Write-Host "NOT running" }

Write-Host "== Active release"
docker exec claim-node readlink /opt/claim-surveyor/current

Write-Host "== Health endpoint (http://localhost:$Port/actuator/health)"
try {
    $health = Invoke-RestMethod "http://localhost:$Port/actuator/health" -TimeoutSec 10
    Write-Host "status: $($health.status)"
} catch {
    Write-Host "health check FAILED: $($_.Exception.Message)"
}

Write-Host "== Environment label on the home page (dev = release 1.0.0, staging = release 1.1.0)"
try {
    $page = Invoke-WebRequest -UseBasicParsing "http://localhost:$Port/" -TimeoutSec 10
    if ($page.Content -match 'id="env"[^>]*>([^<]+)<') { Write-Host $Matches[1] } else { Write-Host "label not found" }
} catch {
    Write-Host "home page FAILED: $($_.Exception.Message)"
}
