def call(Map config) {
    def agentLabel   = config.agentLabel   ?: 'docker'
    def pyImage      = config.pyImage      ?: 'python:3.12-slim'
    def deployBranch = config.deployBranch ?: 'main'
    def approvers    = config.approvers    ?: 'admin'
    def devUrl       = config.devUrl       ?: 'http://localhost:30081'
    def prodUrl      = config.prodUrl      ?: 'http://localhost:30080'

    pipeline {
        agent none

        environment {
            IMAGE = "${config.image}"
            TAG   = "${env.BRANCH_NAME.replace('/', '-')}-${env.BUILD_NUMBER}"
        }

        options {
            timeout(time: 1, unit: 'HOURS')
            buildDiscarder(logRotator(numToKeepStr: '10'))
            disableConcurrentBuilds()
        }

        stages {
            stage('Build') {
                agent { label "${agentLabel}" }
                stages {
                    stage('Checks') {
                        failFast true
                        parallel {
                            stage('Unit Tests') {
                                agent { docker { image "${pyImage}"; reuseNode true } }
                                steps { runPythonTests() }
                            }
                            stage('Lint') {
                                agent { docker { image "${pyImage}"; reuseNode true } }
                                steps { runPythonTests(venv: '.venv-test') }
                            }
                        }
                    }
                    stage('Docker Build') {
                        steps { dockerBuild(image: env.IMAGE, tag: env.TAG) }
                    }
                    stage('Docker Push') {
                        when { expression { env.BRANCH_NAME == deployBranch } }
                        steps { dockerPush(image: env.IMAGE, tag: env.TAG) }
                    }
                    stage('Deploy Dev') {
                        when { expression { env.BRANCH_NAME == deployBranch } }
                        steps {
                            updateGitOps(repo: config.gitopsRepo, path: config.devPath,
                                         image: env.IMAGE, tag: env.TAG)
                        }
                    }
                }
                post {
                    always {
                        junit allowEmptyResults: true, testResults: 'report.xml'
                        sh 'docker logout || true'
                        deleteDir()
                    }
                }
            }

            stage('Approve Prod') {
                when { expression { env.BRANCH_NAME == deployBranch } }
                steps {
                    script {
                        node("${agentLabel}") {
                            notifySlack(":hourglass_flowing_sand: *${env.JOB_NAME}* `${env.TAG}` is in dev (${devUrl}). Approve prod: ${env.BUILD_URL}input")
                        }
                    }
                    timeout(time: 30, unit: 'MINUTES') {
                        input message: "Deploy ${env.TAG} to prod?", ok: 'Deploy to prod', submitter: approvers
                    }
                }
            }

            stage('Deploy Prod') {
                when {
                    beforeAgent true
                    expression { env.BRANCH_NAME == deployBranch }
                }
                agent { label "${agentLabel}" }
                options { skipDefaultCheckout() }
                steps {
                    updateGitOps(repo: config.gitopsRepo, path: config.prodPath,
                                 image: env.IMAGE, tag: env.TAG)
                }
                post { always { deleteDir() } }
            }
        }

        post {
            success {
                script {
                    if (env.BRANCH_NAME == deployBranch) {
                        node("${agentLabel}") {
                            notifySlack(":white_check_mark: *${env.JOB_NAME}* `${env.TAG}` is live in prod: ${prodUrl}")
                        }
                    }
                }
            }
            failure {
                script {
                    if (env.BRANCH_NAME == deployBranch) {
                        node("${agentLabel}") {
                            notifySlack(":x: *${env.JOB_NAME}* #${env.BUILD_NUMBER} failed: ${env.BUILD_URL}console")
                        }
                    }
                }
            }
            aborted {
                script {
                    if (env.BRANCH_NAME == deployBranch) {
                        node("${agentLabel}") {
                            notifySlack(":stop_button: *${env.JOB_NAME}* `${env.TAG}` was NOT promoted to prod (rejected or timed out). Dev has it.")
                        }
                    }
                }
            }
        }
    }
}