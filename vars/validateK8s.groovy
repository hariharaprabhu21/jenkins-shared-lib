def call(Map config) {
    def manifests = config.manifests ?: 'k8s'
    def credId    = config.kubeconfigId ?: 'kubeconfig-docker-desktop'
    withCredentials([file(credentialsId: credId, variable: 'KUBECONFIG')]) {
        sh """
          for f in ${manifests}/*.yaml; do
            sed 's|IMAGE_PLACEHOLDER|${config.image}:${config.tag}|' "\$f" | kubectl apply --dry-run=server -f -
          done
        """
    }
}