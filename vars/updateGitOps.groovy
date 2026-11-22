def call(Map config) {
    def credId = config.credentialsId ?: 'github-pat'
    def branch = config.branch ?: 'main'
    dir('gitops-repo') {
        withCredentials([usernamePassword(credentialsId: credId,
                usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
            sh """
              git clone --depth 1 --branch ${branch} https://\$GIT_USER:\$GIT_TOKEN@${config.repo} .
              git config user.name  "Jenkins CI"
              git config user.email "jenkins@localhost"
              sed -i 's|newTag: .*|newTag: ${config.tag}|' ${config.path}/kustomization.yaml
              git add ${config.path}/kustomization.yaml
                                 git commit -m "Deploy ${config.image}:${config.tag} to ${config.path} (${env.JOB_NAME} #${env.BUILD_NUMBER})" || echo "No change to commit"
              git push origin HEAD:${branch}
            """
        }
    }
}