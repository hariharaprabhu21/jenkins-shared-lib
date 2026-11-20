def call(Map config) {
    def deployBranch = config.deployBranch ?: 'main'
    def port         = config.port ?: 5000
    def healthPath   = config.healthPath ?: '/health'

    pipeline {
        agent any

        environment {
            IMAGE = "${config.image}"
            TAG   = "${env.BRANCH_NAME.replace('/', '-')}-${env.BUILD_NUMBER}"
        }

        options {
            timeout(time: 20, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: '10'))
            disableConcurrentBuilds()
        }

        stages {
            stage('Install & Test') {
                steps { runPythonTests() }
            }
            stage('Docker Build') {
                steps { dockerBuild(image: env.IMAGE, tag: env.TAG) }
            }
            stage('Docker Push') {
                when { expression { env.BRANCH_NAME == deployBranch } }
                steps { dockerPush(image: env.IMAGE, tag: env.TAG) }
            }
            stage('Deploy') {
                when { expression { env.BRANCH_NAME == deployBranch } }
                steps { deployContainer(name: config.appName, image: env.IMAGE, tag: env.TAG, port: port) }
            }
            stage('Smoke Test') {
                when { expression { env.BRANCH_NAME == deployBranch } }
                steps { smokeTest("http://host.docker.internal:${port}${healthPath}") }
            }
        }

        post {
            always {
                junit allowEmptyResults: true, testResults: 'report.xml'
                sh 'docker logout || true'
                deleteDir()
            }
            success { echo "Build OK: ${env.IMAGE}:${env.TAG}" }
            failure { echo 'Pipeline failed - check the stage in red' }
        }
    }
}