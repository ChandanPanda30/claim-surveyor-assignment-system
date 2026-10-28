// Declarative pipeline for the Claim Surveyor Assignment System (Jenkins on Windows).
// Stages: Checkout -> Build -> Unit Tests -> Package -> UI Tests (Selenium) -> Deploy -> Verify
// If any test stage fails, the pipeline stops and nothing is deployed.

pipeline {
    agent any

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'qa', 'staging'],
               description: 'Environment label baked into the build (shown on the home page)')
        string(name: 'TOMCAT_HOME', defaultValue: 'C:\\Program Files\\Apache Software Foundation\\Tomcat 11.0',
               description: 'Tomcat install folder')
        string(name: 'TOMCAT_PORT', defaultValue: '8081',
               description: 'HTTP port Tomcat listens on')
    }

    environment {
        JAVA_HOME = 'C:\\Program Files\\Java\\jdk-21.0.12'
        MVN_HOME  = 'C:\\DEVTOOLS\\apache-maven-3.9.16'
        PATH      = "${MVN_HOME}\\bin;${JAVA_HOME}\\bin;${PATH}"
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

        stage('Deploy') {
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
                // Tomcat unpacks the WAR by itself; wait up to ~90 s for the health check to answer
                bat '''
                    powershell -NoProfile -Command "$url='http://localhost:%TOMCAT_PORT%/claim-surveyor/actuator/health'; $ok=$false; for($i=0;$i -lt 30;$i++){ try { $r=Invoke-WebRequest -UseBasicParsing $url; if($r.StatusCode -eq 200){ $c=$r.Content; if($c -is [byte[]]){ $c=[Text.Encoding]::UTF8.GetString($c) }; Write-Host ('Health check OK: ' + $c); $ok=$true; break } } catch { Start-Sleep -Seconds 3 } }; if(-not $ok){ Write-Host 'Application did not become healthy'; exit 1 }"
                '''
            }
        }
    }

    post {
        success {
            echo "Deployed to http://localhost:${params.TOMCAT_PORT}/claim-surveyor/ (environment: ${params.DEPLOY_ENV})"
        }
        failure {
            echo 'Pipeline failed - see the failing stage. Nothing was deployed if a test stage failed.'
        }
    }
}
