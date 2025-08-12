pipeline {
    agent any

    environment {
        S3_BUCKET = 'younggi-jenkins-deploy-bucket'
        APP_NAME = 'python-web-app'
        BUILD_VERSION = "${env.BUILD_NUMBER}"
        TARGET_EC2_IP = '10.0.19.1'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code from GitHub...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building Python application...'
                sh '''
                    rm -rf build
                    mkdir -p build
                    cp *.py build/
                    cp requirements.txt build/
                    cd build
                    tar -czf ../${APP_NAME}-${BUILD_VERSION}.tar.gz .
                '''
            }
        }

        stage('Upload to S3') {
            steps {
                echo 'Uploading artifact to S3...'
                sh '''
                    aws s3 cp ${APP_NAME}-${BUILD_VERSION}.tar.gz \
                        s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz

                    echo "✅ Uploaded: s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz"
                    echo "📦 Build Version: ${BUILD_VERSION}"
                '''
            }
        }

        stage('Deploy to EC2') {
            steps {
                echo "Deploying to EC2: ${TARGET_EC2_IP}"
                withCredentials([file(credentialsId: 'ec2-user', variable: 'SSH_KEY')]) {
                    sh '''
                        chmod 600 $SSH_KEY

                        ssh -i $SSH_KEY -o StrictHostKeyChecking=no ec2-user@${TARGET_EC2_IP} "
                            cd /home/ec2-user/python-web-app/script &&
                            ./deploy.sh ${BUILD_VERSION}
                        "
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '🎉 Pipeline completed successfully!'
            echo "📦 Artifact: s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz"
            echo "🚀 Deployed to EC2: ${TARGET_EC2_IP}"
        }
        failure {
            echo '❌ Pipeline failed!'
        }
        cleanup {
            sh 'rm -f *.tar.gz'
        }
    }
}