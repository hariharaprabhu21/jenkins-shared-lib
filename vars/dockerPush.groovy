def call(Map config) {
    def credId = config.credentialsId ?: 'dockerhub'
    withCredentials([usernamePassword(credentialsId: credId,
            usernameVariable: 'USER', passwordVariable: 'PASS')]) {
        sh 'echo $PASS | docker login -u $USER --password-stdin'
        retry(3) {
            sh "docker push ${config.image}:${config.tag}"
        }
    }
}