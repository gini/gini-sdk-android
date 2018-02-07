#!/usr/bin/env groovy
pipeline {
    agent any
    environment {
        NEXUS_MAVEN = credentials('external-nexus-maven-repo-credentials')
        GIT = credentials('github')
        GINI_API_CREDENTIALS = credentials('gini-vision-library-android_gini-api-client-credentials')
    }
    stages {
        stage('Import Pipeline Libraries') {
            steps{
                library 'android-tools'
            }
        }
        stage('Build') {
            steps {
                sh './gradlew ginisdk:clean ginisdk:assembleDebug ginisdk:assembleRelease'
            }
        }
        stage('Create AVDs') {
            steps {
                script {
                    avd.deleteCorrupt()
                    avd.create("api-25-nexus-5x", "system-images;android-25;google_apis;x86", "Nexus 5X")
                }
            }
        }
        stage('Instrumentation Tests') {
            steps {
                script {
                    def emulatorPort = emulator.start(avd.createName("api-25-nexus-5x"), "nexus_5x", "-prop persist.sys.language=en -prop persist.sys.country=US -gpu on -camera-back emulated")
                    sh "echo $emulatorPort > emulator_port"
                    adb.setAnimationDurationScale("emulator-$emulatorPort", 0)
                    withEnv(["PATH+TOOLS=$ANDROID_HOME/tools", "PATH+TOOLS_BIN=$ANDROID_HOME/tools/bin", "PATH+PLATFORM_TOOLS=$ANDROID_HOME/platform-tools"]) {
                        sh "./gradlew ginisdk:targetedDebugAndroidTest -PpackageName=net.gini.android -PtestTarget=emulator-$emulatorPort -PtestClientId=$GINI_API_CREDENTIALS_USR -PtestClientSecret=$GINI_API_CREDENTIALS_PSW -PtestApiUri='https://api.gini.net' -PtestUserCenterUri='https://user.gini.net'"
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'ginisdk/build/outputs/androidTest-results/targeted/*.xml'
                    script {
                        def emulatorPort = sh returnStdout:true, script: 'cat emulator_port'
                        emulatorPort = emulatorPort.trim().replaceAll("\r", "").replaceAll("\n", "")
                        emulator.stop(emulatorPort)
                        sh 'rm emulator_port || true'
                    }
                }
            }
        }
        stage('Build Documentation') {
            when {
                branch 'master'
                expression {
                  def tag = sh(returnStdout: true, script: 'git tag --contains $(git rev-parse HEAD)').trim()
                  return !tag.isEmpty()
                }
            }
            steps {
                withEnv(["PATH+=/usr/local/bin"]) {
                    sh './gradlew ginisdk:generateReleaseJavadoc'
                    sh 'scripts/generate-sphinx-doc.sh'
                }
            }
        }
        stage('Release Documentation') {
            when {
                branch 'master'
                expression {
                    def tag = sh(returnStdout: true, script: 'git tag --contains $(git rev-parse HEAD)').trim()
                    return !tag.isEmpty()
                }
                expression {
                    boolean publish = false
                    try {
                        def sha = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                        input "Release documentation from branch ${env.BRANCH_NAME} commit ${sha}?"
                        publish = true
                    } catch (final ignore) {
                        publish = false
                    }
                    return publish
                }
            }
            steps {
                sh 'scripts/release-doc.sh $GIT_USR $GIT_PSW'
            }
        }
        stage('Release Library') {
            when {
                branch 'master'
                expression {
                    def tag = sh(returnStdout: true, script: 'git tag --contains $(git rev-parse HEAD)').trim()
                    return !tag.isEmpty()
                }
                expression {
                    boolean publish = false
                    try {
                        def sha = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                        input "Release from branch ${env.BRANCH_NAME} commit ${sha}?"
                        publish = true
                    } catch (final ignore) {
                        publish = false
                    }
                    return publish
                }
            }
            steps {
                sh './gradlew ginisdk:buildReleaseZip'
                archiveArtifacts 'ginisdk/build/distributions/*.zip'
                sh './gradlew ginisdk:uploadArchives -PmavenOpenRepoUrl=https://repo.gini.net/nexus/content/repositories/open -PrepoUser=$NEXUS_MAVEN_USR -PrepoPassword=$NEXUS_MAVEN_PSW'
            }
        }
    }
}