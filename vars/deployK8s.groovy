def call(Map config) {
    def ns        = config.namespace ?: 'default'
    def manifests = config.manifests ?: 'k8s'
    def credId    = config.kubeconfigId ?: 'kubeconfig-docker-desktop'
    def app       = config.appName
    def image     = "${config.image}:${config.tag}"
    withCredentials([file(credentialsId: credId, variable: 'KUBECONFIG')]) {
        sh """
          kubectl apply -f ${manifests}/namespace.yaml
          kubectl apply -f ${manifests}/service.yaml
          sed 's|IMAGE_PLACEHOLDER|${image}|' ${manifests}/deployment.yaml | kubectl apply -f -
          kubectl annotate deployment/${app} -n ${ns} kubernetes.io/change-cause="${env.JOB_NAME} #${env.BUILD_NUMBER} -> ${image}" --overwrite
          if ! kubectl rollout status deployment/${app} -n ${ns} --timeout=90s; then
            echo "Rollout failed - rolling back"
            kubectl rollout undo deployment/${app} -n ${ns}
            kubectl rollout status deployment/${app} -n ${ns} --timeout=90s
            exit 1
          fi
        """
    }
}