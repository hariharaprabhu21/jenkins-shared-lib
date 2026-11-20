def call(String url) {
    retry(3) {
        sh "sleep 5 && curl -f ${url}"
    }
}