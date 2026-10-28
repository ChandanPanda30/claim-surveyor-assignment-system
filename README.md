# Claim Surveyor Assignment System

A small web application that lets a claimant submit an insurance claim, validates the data,
lets a reviewer approve or reject it, assigns a surveyor, and tracks status end to end.
Built as the running project for a DevOps pipeline (Git/GitHub, Jenkins, Selenium, Docker, Ansible).

# CI: built by Jenkins job claim-surveyor-ci

## Tech stack

| Area | Choice |
|------|--------|
| Language | Java 21 |
| Framework | Spring Boot 3.3 (MVC + Thymeleaf + Spring Data JPA) |
| Build tool | Maven (WAR packaging) |
| Database | H2 (file mode) |
| Deployment target | Apache Tomcat 10.1+ (also runs standalone with embedded Tomcat) |
| CI/CD | Jenkins, Docker, Ansible (added in later tasks) |

## Run locally

```bash
# standalone (embedded Tomcat) -> http://localhost:8080/
mvn spring-boot:run

# run tests
mvn verify

# build the WAR
mvn clean package        # produces target/claim-surveyor.war
```

Deploy to Tomcat: copy `target/claim-surveyor.war` into `<TOMCAT_HOME>/webapps/`, start Tomcat and open
`http://localhost:8080/claim-surveyor/`.

Health check: `/actuator/health`

## Configuration (environment variables)

| Variable | Default | Purpose |
|----------|---------|---------|
| `PORT` | `8080` | HTTP port (standalone mode) |
| `APP_ENV` | `dev` | Environment label shown on the home page |
| `DB_PATH` | `./data/claimdb` | H2 database file location |

## Project structure

```
src/main/java/com/claimsurveyor/
  config/    home page and app configuration
  claim/     claim domain: entity, repository, service, controller
src/main/resources/templates/   Thymeleaf pages
src/test/java/                  unit and web tests
docs/                           branching policy and project notes
.github/ISSUE_TEMPLATE/         issue templates
```

## Workflow

See [docs/BRANCHING.md](docs/BRANCHING.md) for branch names, commit message style and the pull request process.
