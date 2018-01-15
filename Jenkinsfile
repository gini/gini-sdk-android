#!/usr/bin/env groovy
pipeline {
    agent any
    environment {
        NEXUS_MAVEN = credentials('external-nexus-maven-repo-credentials')
        GIT = credentials('github')
    }
    stages {
        stage('Build') {
            steps {
                sh './gradlew ginisdk:clean ginisdk:assembleDebug ginisdk:assembleRelease'
            }
        }
        stage('Unit Tests') {
            steps {
                sh './gradlew ginisdk:test'
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
