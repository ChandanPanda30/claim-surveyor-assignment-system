# Builds two releases of the WAR for the Puppet demo (run from the project root):
#   puppet\artifacts\claim-surveyor-1.0.0.war   (environment label "dev")
#   puppet\artifacts\claim-surveyor-1.1.0.war   (environment label "staging")
# The label shown on the home page tells the two releases apart.
param(
    [string]$Old = "1.0.0",
    [string]$New = "1.1.0"
)
$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force puppet\artifacts | Out-Null

foreach ($release in @(@($Old, "dev"), @($New, "staging"))) {
    $version = $release[0]
    $label = $release[1]
    Write-Host "Building release $version (environment $label)..."
    mvn -B -q clean package "-DskipTests" "-Dapp.environment=$label"
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed for $version" }
    Copy-Item target\claim-surveyor.war "puppet\artifacts\claim-surveyor-$version.war" -Force
}
Get-ChildItem puppet\artifacts
