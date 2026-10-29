# Configuration Specification (Puppet)

## Target node
A clean Linux server (Ubuntu 24.04) that contains nothing but the Puppet agent. In this project the node is a
Docker container (`claim-node`), so it can be created and destroyed in seconds. Puppet is run with
`puppet apply` (masterless): the manifest is applied directly on the node.

## Server prerequisites and how Puppet provides them
| Category | Requirement | Puppet resource |
|----------|-------------|-----------------|
| Packages | `openjdk-21-jre-headless` (runs the Spring Boot WAR), `curl` (health check), `ca-certificates` | `package` |
| Group / user | System account `claimapp`, no login shell (`/usr/sbin/nologin`), home `/opt/claim-surveyor` | `group`, `user` |
| Folders | `/opt/claim-surveyor` (root), `releases/`, `bin/` (root), `data/`, `logs/`, `run/` (owned by `claimapp`, mode 0750), `/etc/claim-surveyor` | `file` (directory) |
| Release | `/opt/claim-surveyor/releases/<version>/claim-surveyor.war` copied from the artifact folder | `file` (source) |
| Active release | Symlink `/opt/claim-surveyor/current` -> `releases/<version>` (switching it = deploy or rollback) | `file` (link) |
| Config file | `/etc/claim-surveyor/claim-surveyor.env` with `PORT`, `DB_PATH`, `JAVA_OPTS` | `file` + EPP template |
| Control script | `/opt/claim-surveyor/bin/claim-surveyor.sh start|stop|restart|status` (runs Java as `claimapp`) | `file` + EPP template |
| Port | Application listens on **8080** inside the node; the demo publishes it on host port **8084** | `claimapp::port` parameter, `docker run -p` |
| Service | `claim-surveyor` must be running; restarted when the release link, config or script changes | `service` (provider `base`) |
| Health check | `GET /actuator/health` must return 200 after every start or restart | `exec` (`refreshonly`) |

## Module layout
```
puppet/
  manifests/site.pp                      entry point, reads the release version
  modules/claimapp/
    manifests/init.pp                    parameters and ordering
    manifests/install.pp                 packages, user, folders, release, current link
    manifests/config.pp                  env file and control script
    manifests/service.pp                 service and health check
    templates/claim-surveyor.env.epp
    templates/claim-surveyor.sh.epp
  Dockerfile.node                        the clean node (Ubuntu + Puppet agent only)
  build-artifacts.ps1                    builds the two releases used in the demo
```

## Choosing the release
`site.pp` reads the external fact `release_version` (default `1.0.0`):
```
FACTER_release_version=1.1.0 puppet apply ...
```
Older releases stay on the node in `releases/`, so a rollback is the same command with the earlier version.

## First execution (Task 13)
Run from the project root in PowerShell.

```powershell
# 1. Build the release WARs (1.0.0 = label dev, 1.1.0 = label staging)
.\puppet\build-artifacts.ps1

# 2. Build the clean node image and start the node (app port 8080 published on 8084)
docker build -f puppet/Dockerfile.node -t claim-node-image puppet
docker run -d --name claim-node --hostname claim-node -p 8084:8080 -v "${PWD}\puppet:/puppet:ro" claim-node-image

# 3. First Puppet run: installs everything and starts release 1.0.0 (takes a few minutes: Java is downloaded)
New-Item -ItemType Directory -Force docs\evidence | Out-Null
docker exec -e FACTER_release_version=1.0.0 claim-node puppet apply /puppet/manifests/site.pp --modulepath=/puppet/modules --detailed-exitcodes 2>&1 | Tee-Object -FilePath docs\evidence\puppet-first-run.txt
```
Exit code 2 from `--detailed-exitcodes` means "changes were applied" (that is the expected result of the first run).

## Checking the result
```powershell
docker exec claim-node /opt/claim-surveyor/bin/claim-surveyor.sh status; echo $LASTEXITCODE   # 0 = running
docker exec claim-node ls -l /opt/claim-surveyor /opt/claim-surveyor/releases
docker exec claim-node id claimapp
Invoke-RestMethod http://localhost:8084/actuator/health
```
Open http://localhost:8084/ : the home page shows the environment label of release 1.0.0 (`dev`).

## Evidence to capture
1. The Puppet first-run output (resources created, `Applied catalog in ...`)
2. `claim-surveyor.sh status`, the folder listing with the `current` link, and `id claimapp`
3. The health response and the application open at http://localhost:8084/
