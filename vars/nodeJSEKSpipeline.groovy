def call(Map configmap) {
    pipeline {
        agent {
            label 'AGENT-1'
        }

        options {
            timeout(time: 30, unit: 'MINUTES')
            disableConcurrentBuilds()
        }

        environment {
            greeting = configMap.get('greeting')
        }
        parameters{
            booleanParam(name: 'deploy', defaultValue: false, description: 'Toggle this value')
        }
        stages {

            stage('print greeting') {
                steps {
                    script {
                       
                        echo "Version is : $greeting"
                    }
                }
            }

            
        post {
            always {
                echo "This section runs always"
                deleteDir()
            }
            success {
                echo "This section runs when the pipeline succeeds"
            }
            failure {
                echo "This section runs when the pipeline fails"
            }
        }
    }

}