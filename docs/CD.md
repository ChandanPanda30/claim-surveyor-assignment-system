# Continuous Delivery with Jenkins and Docker

## Pipeline flow
```
Checkout -> Build -> Unit Tests -> Package -> UI Tests (Selenium)
        -> Docker Build -> Docker Push -> Deploy Container -> Verify
```
A failure in any test stage stops the pipeline, so no image is built, pushed or deployed.

## Image versioning
Every pipeline run builds the image as `localhost:5000/claim-surveyor:0.1.<build number>` and also tags it `latest`.
The exact tag is recorded in `image-tag.txt` (archived with the build) and printed in the final log message.
Older versions stay in the registry, which is what rollback (Task 14) uses.

## Registry
A private registry runs as a container (`registry:2`, name `local-registry`, port 5000). The pipeline creates it on the
first run. `localhost:5000` is treated as secure by Docker, so no certificate setup is needed.

List stored versions:
```powershell
Invoke-RestMethod http://localhost:5000/v2/claim-surveyor/tags/list
Invoke-RestMethod http://localhost:5000/v2/_catalog
```

## Deployment
`Deploy Container` pulls the exact tag from the registry, removes the old `claim-app` container and starts a new one
on host port 8083 (`-p 8083:8080`) with the `claim-data` volume, so claims survive every deployment.

## Parameters
| Parameter | Meaning |
|-----------|---------|
| DEPLOY_ENV | dev / qa / staging, shown on the home page |
| DEPLOY_TARGET | `docker` (default) or `tomcat` (the Task 8 WAR deployment) |
| APP_PORT | host port of the container (8083) |

## Evidence to capture
1. Jenkins stage view with all stages green, including Docker Build, Docker Push, Deploy Container
2. Console log lines showing the image tag and `Health check OK`
3. `docker images` and `docker ps` showing the versioned tag running
4. The registry tag list (`Invoke-RestMethod ...tags/list`)
5. A second run that produces the next version (e.g. `0.1.15`, then `0.1.16`), with the home page unchanged and the data still there
