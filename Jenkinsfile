// Declarative pipeline for the Claim Surveyor Assignment System (Jenkins on Windows).
// CI : Checkout -> Build -> Unit Tests -> Package -> UI Tests (Selenium)
// CD : Docker Build (versioned image) -> Docker Push (registry) -> Deploy Container -> Verify
// If any test stage fails, the pipeline stops and nothing is built into an image or deployed.
// DEPLOY_TARGET=tomcat keeps the older "copy the WAR into Tomcat" deployment available.

pipeline {
    agent any

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'qa', 'staging'],
               description: 'Environment label baked into the build (shown on the home page)')
        choice(name: 'DEPLOY_TARGET', choices: ['docker', 'tomcat'],
               description: 'docker = build image, push to registry, run container; tomcat = copy WAR to Tomcat')
        string(name: 'APP_PORT', defaultValue: '8083',
               description: 'Host port for the Docker container (container listens on 8080)')
        string(name: 'TOMCAT_HOME', defaultValue: 'C:\\Program Files\\Apache Software Foundation\\Tomcat 11.0',
               description: 'Tomcat install folder (only used when DEPLOY_TARGET=tomcat)')
        string(name: 'TOMCAT_PORT', defaultValue: '8081',
               description: 'HTTP port Tomcat listens on (only used when DEPLOY_TARGET=tomcat)')
    }

    environment {
        JAVA_HOME  = 'C:\\Program Files\\Java\\jdk-21.0.12'
        MVN_HOME   = 'C:\\DEVTOOLS\\apache-maven-3.9.16'
        PATH       = "${MVN_HOME}\\bin;${JAVA_HOME}\\bin;C:\\Program Files\\Docker\\Docker\\resources\\bin;${PATH}"
        // Jenkins runs as the Windows service account, which has no Docker Desktop context of its own,
        // so point the docker CLI straight at the Docker Desktop engine.
        DOCKER_HOST = 'npipe:////./pipe/dockerDesktopLinuxEngine'
        REGISTRY   = 'localhost:5000'
        IMAGE_NAME = 'claim-surveyor'
        IMAGE_TAG  = "0.1.${BUILD_NUMBER}"
        CONTAINER  = 'claim-app'
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn -B -V clean compile'
            }
        }

        stage('Unit Tests') {
            steps {
                bat 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                bat 'mvn -B package -DskipTests -Dapp.environment=%DEPLOY_ENV%'
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }

        stage('UI Tests (Selenium)') {
            steps {
                bat 'mvn -B failsafe:integration-test failsafe:verify -Dselenium.noSandbox=true'
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'target/screenshots/**', allowEmptyArchive: true
                }
            }
        }

        // ---------------- Continuous delivery with Docker ----------------

        stage('Docker Build') {
            when { expression { params.DEPLOY_TARGET == 'docker' } }
            steps {
                bat '''
                    docker version --format "Docker engine {{.Server.Version}}"
                    docker build ^
                        --build-arg APP_ENV=%DEPLOY_ENV% ^
                        --build-arg APP_VERSION=%IMAGE_TAG% ^
                        -t %REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG% ^
                        -t %REGISTRY%/%IMAGE_NAME%:latest .
                    echo %REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG%> image-tag.txt
                '''
                archiveArtifacts artifacts: 'image-tag.txt', allowEmptyArchive: true
            }
        }

        stage('Docker Push') {
            when { expression { params.DEPLOY_TARGET == 'docker' } }
            steps {
                // The registry is itself a container (registry:2). Create it on the first run, start it if stopped.
                bat '''
                    docker container inspect local-registry >nul 2>&1
                    if errorlevel 1 (
                        docker run -d --restart always -p 5000:5000 --name local-registry registry:2
                    ) else (
                        docker start local-registry
                    )
                    ping -n 4 127.0.0.1 >nul
                    docker push %REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG%
                    docker push %REGISTRY%/%IMAGE_NAME%:latest
                '''
            }
        }

        stage('Deploy Container') {
            when { expression { params.DEPLOY_TARGET == 'docker' } }
            steps {
                // Pull the exact version from the registry, replace the old container, keep the data volume.
                bat '''
                    docker pull %REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG%
                    docker rm -f %CONTAINER% >nul 2>&1 || ver >nul
                    docker run -d --name %CONTAINER% --restart unless-stopped -p %APP_PORT%:8080 -e APP_ENV=%DEPLOY_ENV% -v claim-data:/app/data %REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG%
                '''
            }
        }

        stage('Deploy to Tomcat') {
            when { expression { params.DEPLOY_TARGET == 'tomcat' } }
            steps {
                bat '''
                    if not exist "%TOMCAT_HOME%\\webapps" (
                        echo Tomcat webapps folder not found: %TOMCAT_HOME%\\webapps
                        exit /b 1
                    )
                    copy /Y "target\\claim-surveyor.war" "%TOMCAT_HOME%\\webapps\\claim-surveyor.war"
                '''
            }
        }

        stage('Verify') {
            steps {
                script {
                    // Container: http://localhost:<APP_PORT>/   Tomcat: http://localhost:<TOMCAT_PORT>/claim-surveyor/
                    env.HEALTH_URL = (params.DEPLOY_TARGET == 'docker')
                        ? "http://localhost:${params.APP_PORT}/actuator/health"
                        : "http://localhost:${params.TOMCAT_PORT}/claim-surveyor/actuator/health"
                }
                // Poll for up to ~2 minutes; each request has its own 5 s timeout so it can never hang.
                bat '''
                    powershell -NoProfile -Command "$url=$env:HEALTH_URL; $ok=$false; for($i=0;$i -lt 40;$i++){ try { $r=Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 $url; if($r.StatusCode -eq 200){ $c=$r.Content; if($c -is [byte[]]){ $c=[Text.Encoding]::UTF8.GetString($c) }; Write-Host ('Health check OK: ' + $c); $ok=$true; break } } catch { Start-Sleep -Seconds 3 } }; if(-not $ok){ Write-Host ('Application did not become healthy at ' + $url); exit 1 }"
                '''
                script {
                    if (params.DEPLOY_TARGET == 'docker') {
                        bat 'docker ps --filter "name=%CONTAINER%" --format "{{.Names}}  {{.Image}}  {{.Status}}"'
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                if (params.DEPLOY_TARGET == 'docker') {
                    echo "Deployed image ${env.REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG} -> http://localhost:${params.APP_PORT}/ (environment: ${params.DEPLOY_ENV})"
                } else {
                    echo "Deployed to http://localhost:${params.TOMCAT_PORT}/claim-surveyor/ (environment: ${params.DEPLOY_ENV})"
                }
            }
        }
        failure {
            echo 'Pipeline failed - see the failing stage. A failing test stage means no image was built and nothing was deployed.'
        }
    }
}
