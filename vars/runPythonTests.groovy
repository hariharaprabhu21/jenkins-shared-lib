def call(Map config = [:]) {
    def reqFile = config.requirements ?: 'requirements-dev.txt'
    sh """
      python3 -m venv venv
      venv/bin/pip install -r ${reqFile}
      venv/bin/pytest --junitxml=report.xml
    """
}