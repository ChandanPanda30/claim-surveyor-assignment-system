# Provisioning, Idempotency, Health Check and Rollback

All commands are PowerShell, run from the project root. The Puppet module and the node image are described in
`docs/CONFIG_SPEC.md`. Two helper scripts keep the commands short:

| Script | Purpose |
|--------|---------|
| `.\puppet\apply.ps1 -Version <v> [-LogName file.txt]` | Runs Puppet on the node for release `<v>`; saves the output to `docs\evidence\file.txt`; explains the exit code |
| `.\puppet\check.ps1` | Shows service status, active release, health endpoint result and the environment label |

Release 1.0.0 is the previous stable release (label `dev`); release 1.1.0 is the new release (label `staging`).

## A. Provision a clean node
Throw the old node away and build a new one from nothing but the Puppet agent image:
```powershell
docker rm -f claim-node
docker run -d --name claim-node --hostname claim-node -p 8084:8080 -v "${PWD}\puppet:/puppet:ro" claim-node-image
.\puppet\apply.ps1 -Version 1.0.0 -LogName 1-provision-1.0.0.txt
.\puppet\check.ps1
```
Expected: exit code 2 (changes applied), the health check line in the Puppet output, `status: UP`, label `dev`.

Optional: open http://localhost:8084/ and submit a claim, so you can show later that data survives releases.

## B. Idempotency: run the same automation again
```powershell
.\puppet\apply.ps1 -Version 1.0.0 -LogName 2-idempotency-rerun.txt
```
Expected: **exit code 0** and no resource lines (only `Compiled catalog` and `Applied catalog`). Puppet found
everything already in the desired state, so it changed nothing and did not restart the application.

## C. Deploy the new release
```powershell
.\puppet\apply.ps1 -Version 1.1.0 -LogName 3-deploy-1.1.0.txt
.\puppet\check.ps1
```
Expected: the `current` link changes to `releases/1.1.0`, the service restarts, the health check passes, label `staging`.
`ls -l /opt/claim-surveyor/releases` inside the node now shows both releases. Your claim from step A is still listed
(the database folder is not part of a release).

## D. Rollback to the previous stable release
```powershell
.\puppet\apply.ps1 -Version 1.0.0 -LogName 4-rollback-1.0.0.txt
.\puppet\check.ps1
```
Expected: `current` points to `releases/1.0.0` again, the service restarts, health is `UP`, label back to `dev`.
No rebuild is needed because the old release is still on the node.

## E. Recovery: Puppet repairs a broken node
Simulate a crash:
```powershell
docker exec claim-node /opt/claim-surveyor/bin/claim-surveyor.sh stop
.\puppet\check.ps1                       # shows NOT running and the health check FAILED
.\puppet\apply.ps1 -Version 1.0.0 -LogName 5-recovery-service.txt
.\puppet\check.ps1                       # running and UP again
```
Simulate configuration drift (someone edits the config by hand):
```powershell
docker exec claim-node sh -c "echo '# edited by hand' >> /etc/claim-surveyor/claim-surveyor.env"
.\puppet\apply.ps1 -Version 1.0.0 -LogName 6-recovery-config.txt
docker exec claim-node cat /etc/claim-surveyor/claim-surveyor.env
```
Expected: Puppet rewrites the file from its template (the hand edit is gone) and restarts the service.

## Evidence checklist
| # | Evidence | File / screenshot |
|---|----------|-------------------|
| 1 | Clean node provisioned, health UP | `1-provision-1.0.0.txt`, `check.ps1` output |
| 2 | Idempotency: second run changes nothing | `2-idempotency-rerun.txt` (exit code 0) |
| 3 | New release deployed | `3-deploy-1.1.0.txt`, home page shows `staging` |
| 4 | Rollback to previous stable release | `4-rollback-1.0.0.txt`, home page shows `dev` |
| 5 | Recovery of a stopped service and of config drift | `5-recovery-service.txt`, `6-recovery-config.txt` |
