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
            DEBUG = 'true'
            appVersion = '' // Global variable, set in first stage
            region = 'us-east-1'
            account_id = '557690626059'
            project = configMap.get('project')
            component = configMap.get('component')
            environment = 'dev'
        }
        parameters{
            booleanParam(name: 'deploy', defaultValue: false, description: 'Toggle this value')
        }
        stages {

            stage('Read the version') {
                steps {
                    script {
                        def packageJson = readJSON file: 'package.json'
                        appVersion = packageJson.version
                        echo "App version: ${appVersion}"
                    }
                }
            }

            stage('Install Dependencies') {
                steps {
                    sh 'npm install'
                }
            }
            // stage('Run Sonarqube') {
            //     environment {
            //         scannerHome = tool 'sonar-scanner-7.1';
            //     }
            //     steps {
            //     withSonarQubeEnv('sonar-scanner-7.1') {
            //         sh "${scannerHome}/bin/sonar-scanner"
            //         // this is generic command works for any language
            //     }
            //     }
            // }
            // stage("Quality Gate") {
            //     steps {
            //     timeout(time: 1, unit: 'HOURS') {
            //         waitForQualityGate abortPipeline: true
            //     }
            //     }
            // } 
            stage('Docker build') {
                steps {
                    withAWS(region: "${region}", credentials: 'aws-creds') {
                        sh """
                            aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.us-east-1.amazonaws.com

                            docker build -t ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${appVersion} .

                        

                            docker push ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${appVersion}
                        """
                    }
                }
            }

        

            stage('Trigger Deploy'){
                when { 
                    expression { params.deploy }
                }
                steps{
                    build job: 'backend-cd', parameters: [string(name: 'version', value: "${appVersion}")], wait: true
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
