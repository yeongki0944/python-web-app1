pipeline {
    agent any

    parameters {
        string(name: 'TARGET_EC2_IP', defaultValue: '10.0.19.1', description: '배포할 EC2 서버 IP (예: 10.0.19.1)')
    }

    environment {
        S3_BUCKET = 'younggi-jenkins-deploy-bucket'
        APP_NAME = 'python-web-app'
        BUILD_VERSION = "${env.BUILD_NUMBER}"
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
                    # 빌드 디렉토리 준비
                    rm -rf build
                    mkdir -p build

                    # 필수 파일들만 복사
                    cp *.py build/
                    cp requirements.txt build/

                    # 압축 파일 생성
                    cd build
                    tar -czf ../${APP_NAME}-${BUILD_VERSION}.tar.gz .
                '''
            }
        }

        stage('Upload to S3') {
            steps {
                echo 'Uploading artifact to S3...'
                sh '''
                    # 빌드 번호로 업로드
                    aws s3 cp ${APP_NAME}-${BUILD_VERSION}.tar.gz \
                        s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz

                    echo "✅ Uploaded: s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz"
                    echo "📦 Build Version: ${BUILD_VERSION}"
                '''
            }
        }

        stage('Deploy to EC2') {
            when {
                not {
                    equals expected: '', actual: params.TARGET_EC2_IP.trim()
                }
            }
            steps {
                echo "Deploying to EC2: ${params.TARGET_EC2_IP}"
                withCredentials([file(credentialsId: 'ec2-user', variable: 'SSH_KEY')]) {
                    sh '''
                        # SSH 키 권한 설정
                        chmod 600 $SSH_KEY

                        # 배포 디렉토리 준비
                        ssh -i $SSH_KEY -o StrictHostKeyChecking=no ec2-user@${TARGET_EC2_IP} "
                            mkdir -p /home/ec2-user/python-web-app/script &&
                            mkdir -p /home/ec2-user/python-web-app/app
                        "

                        # 배포 실행
                        ssh -i $SSH_KEY -o StrictHostKeyChecking=no ec2-user@${TARGET_EC2_IP} "
                            sudo chown -R ec2-user:ec2-user /home/ec2-user/python-web-app/ &&
                            chmod -R 755 /home/ec2-user/python-web-app/ &&
                            cd /home/ec2-user/python-web-app/script &&
                            chmod +x deploy.sh &&
                            echo 'Jenkins Build Number: ${BUILD_VERSION}' &&
                            echo 'Target EC2: ${TARGET_EC2_IP}' &&
                            ./deploy.sh ${BUILD_VERSION}
                        "
                    '''
                }
            }
            }
        }
    }

    post {
        success {
            echo '🎉 Pipeline completed successfully!'
            echo "📦 Artifact: s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz"
            script {
                if (params.TARGET_EC2_IP?.trim()) {
                    echo "🚀 Deployed to EC2: ${params.TARGET_EC2_IP}"
                } else {
                    echo "ℹ️ EC2 deployment skipped (no IP provided)"
                }
            }
        }
        failure {
            echo '❌ Pipeline failed!'
        }
        cleanup {
            sh 'rm -f *.tar.gz'
        }
    }
}