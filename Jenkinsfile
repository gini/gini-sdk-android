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
            post {
                always {
                    publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'ginisdk/build/reports/tests/testDebugUnitTest', reportFiles: 'index.html', reportName: 'Unit Test Results', reportTitles: ''])
                }
            }
        }
        stage('Code Coverage') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    allOf {
                        branch 'master'
                        expression {
                            def tag = sh(returnStdout: true, script: 'git tag --contains $(git rev-parse HEAD)').trim()
                            return !tag.isEmpty()
                        }
                    }
                }
            }
            steps {
                sh './gradlew ginisdk:unifyTargetedTestCoverage ginisdk:jacocoTestDebugUnitTestReport'
                publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'ginisdk/build/reports/jacoco/jacocoTestDebugUnitTestReport/html', reportFiles: 'index.html', reportName: 'Code Coverage Report', reportTitles: ''])
            }
        }
        stage('Javadoc Coverage') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    allOf {
                        branch 'master'
                        expression {
                            def tag = sh(returnStdout: true, script: 'git tag --contains $(git rev-parse HEAD)').trim()
                            return !tag.isEmpty()
                        }
                    }
                }
            }
            steps {
                sh './gradlew ginisdk:generateJavadocCoverage'
                publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'ginisdk/build/reports/javadoc-coverage', reportFiles: 'index.html', reportName: 'Javadoc Coverage Report', reportTitles: ''])
            }
        }
        stage('Build Documentation') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    allOf {
                        branch 'master'
                        expression {
                            def tag = sh(returnStdout: true, script: 'git tag --contains $(git rev-parse HEAD)').trim()
                            return !tag.isEmpty()
                        }
                    }
                }
            }
            steps {
                withEnv(["PATH+=/usr/local/bin"]) {
                    sh 'scripts/build-sphinx-doc.sh'
                }
                publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'ginisdk/src/doc/build/html', reportFiles: 'index.html', reportName: 'Documentation', reportTitles: ''])
            }
        }
        stage('Generate Javadoc') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    allOf {
                        branch 'master'
                        expression {
                            def tag = sh(returnStdout: true, script: 'git tag --contains $(git rev-parse HEAD)').trim()
                            return !tag.isEmpty()
                        }
                    }
                }
            }
            steps {
                sh './gradlew ginisdk:generateJavadoc'
                publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'ginisdk/build/docs/javadoc', reportFiles: 'index.html', reportName: 'Javadoc', reportTitles: ''])
            }
        }
        stage('Archive Artifacts') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    allOf {
                        branch 'master'
                        expression {
                            def tag = sh(returnStdout: true, script: 'git tag --contains $(git rev-parse HEAD)').trim()
                            return !tag.isEmpty()
                        }
                    }
                }
            }
            steps {
                sh 'cd ginisdk/build/reports/jacoco/jacocoTestDebugUnitTestReport && zip -r testCoverage.zip html && cd -'
                sh 'cd ginisdk/build/reports && zip -r javadocCoverage.zip javadoc-coverage && cd -'
                archiveArtifacts 'ginisdk/build/outputs/aar/*.aar,ginisdk/build/reports/jacoco/jacocoTestDebugUnitTestReport/testCoverage.zip,ginisdk/build/reports/javadocCoverage.zip'
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
                        def version = sh(returnStdout: true, script: './gradlew -q printLibraryVersion').trim()
                        def sha = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                        input "Release documentation for ${version} from branch ${env.BRANCH_NAME} commit ${sha}?"
                        publish = true
                    } catch (final ignore) {
                        publish = false
                    }
                    return publish
                }
            }
            steps {
                sh 'scripts/release-javadoc.sh $GIT_USR $GIT_PSW'
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
            }
            steps {
                sh './gradlew ginisdk:uploadArchives -PmavenOpenRepoUrl=https://repo.gini.net/nexus/content/repositories/open -PrepoUser=$NEXUS_MAVEN_USR -PrepoPassword=$NEXUS_MAVEN_PSW'
            }
        }
    }
}
