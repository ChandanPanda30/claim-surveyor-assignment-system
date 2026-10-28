# Docker

## Image design
| Item | Choice |
|------|--------|
| Build | Multi-stage: Maven + JDK 21 builds the WAR, then only the WAR is copied into the runtime image |
| Runtime | `eclipse-temurin:21-jre-alpine` (small), runs as non-root user `app` |
| Run command | `java -jar claim-surveyor.war` (embedded Tomcat, port 8080 inside the container) |
| Config | Environment variables `PORT`, `APP_ENV`, `DB_PATH` |
| Data | H2 file database in `/app/data`, kept in a Docker volume |
| Health | `HEALTHCHECK` calls `/actuator/health` every 15 s |

Host port **8083** is used in the examples because 8080 (Jenkins), 8081 (Tomcat) and 8082 (dev) are taken.

## Container lifecycle (PowerShell, from the project folder)

```powershell
# 1. Build the image (tagged with a version)
docker build -t claim-surveyor:0.1.0 .

# 2. Create and start a container (detached) with a named volume for the database
docker run -d --name claim-app -p 8083:8080 -e APP_ENV=docker -v claim-data:/app/data claim-surveyor:0.1.0

# 3. Inspect it
docker ps                                   # running containers (STATUS shows health)
docker logs -f claim-app                    # follow logs (Ctrl+C to stop following)
docker inspect --format "{{.State.Health.Status}}" claim-app
docker exec claim-app whoami                # run a command inside (prints: app)

# 4. Use it: http://localhost:8083/   and   http://localhost:8083/actuator/health

# 5. Stop, start, restart
docker stop claim-app
docker start claim-app
docker restart claim-app

# 6. Remove the container (the volume and image remain)
docker stop claim-app
docker rm claim-app

# 7. Data survives: run a new container on the same volume, earlier claims are still listed
docker run -d --name claim-app -p 8083:8080 -v claim-data:/app/data claim-surveyor:0.1.0

# 8. Clean up everything
docker rm -f claim-app
docker rmi claim-surveyor:0.1.0
docker volume rm claim-data
```

## Evidence to capture
1. `docker build` finishing, then `docker images` showing `claim-surveyor 0.1.0`
2. `docker ps` with the container `(healthy)` and the app open at http://localhost:8083/ (home page shows environment `docker`)
3. A claim submitted, then the container removed and recreated, and the claim still visible (volume)
4. `docker logs` and `docker exec claim-app whoami`
