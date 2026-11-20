def call(Map config) {
    def port = config.port ?: 5000
    sh """
      docker rm -f ${config.name} || true
      docker run -d --name ${config.name} -p ${port}:${port} ${config.image}:${config.tag}
    """
}