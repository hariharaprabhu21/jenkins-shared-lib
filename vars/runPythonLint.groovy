def call(Map config = [:]) {
    def venv = config.venv ?: '.venv-lint'
    sh """
      python3 -m venv ${venv}
      ${venv}/bin/pip install --no-cache-dir flake8
      ${venv}/bin/flake8 . --exclude=.venv* --select=E9,F63,F7,F82 --show-source --statistics
    """
}