def call(String message) {
    withCredentials([string(credentialsId: 'slack-webhook', variable: 'SLACK_WEBHOOK')]) {
        writeFile file: 'slack.json', text: groovy.json.JsonOutput.toJson([text: message])
        sh '''
          curl -sS -f -X POST -H "Content-type: application/json" --data @slack.json "$SLACK_WEBHOOK" \
            || echo "WARNING: Slack notification failed"
          rm -f slack.json
        '''
    }
}