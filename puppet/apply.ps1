# Applies the Puppet manifest on the node for one release version (run from the project root).
#   .\puppet\apply.ps1 -Version 1.1.0 -LogName deploy-1.1.0.txt
# Exit code of Puppet: 0 = nothing changed, 2 = changes were applied, 1/4/6 = errors.
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$LogName
)
$ErrorActionPreference = "Continue"
New-Item -ItemType Directory -Force docs\evidence | Out-Null

$puppetArgs = @("exec", "-e", "FACTER_release_version=$Version", "claim-node",
    "puppet", "apply", "/puppet/manifests/site.pp",
    "--modulepath=/puppet/modules", "--detailed-exitcodes")

Write-Host "Applying release $Version ..."
if ($LogName) {
    docker @puppetArgs 2>&1 | Tee-Object -FilePath "docs\evidence\$LogName"
} else {
    docker @puppetArgs 2>&1
}
$code = $LASTEXITCODE
switch ($code) {
    0 { Write-Host "Puppet exit code 0: node already matches the manifest (nothing changed)." }
    2 { Write-Host "Puppet exit code 2: changes were applied." }
    default { Write-Host "Puppet exit code ${code}: something went wrong, see the output above." }
}
