def call(Map config = [:]) {
    def reqFile = config.requirements ?: 'requirements-dev.txt'
    def venv    = config.venv ?: 'venv'
    sh """
      python3 -m venv ${venv}
      ${venv}/bin/pip install --no-cache-dir -r ${reqFile}
      ${venv}/bin/pytest --junitxml=report.xml
    """
}