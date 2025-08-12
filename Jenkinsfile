pipeline {
    agent any

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
                    aws s3 cp ${APP_NAME}-${BUILD_VERSION}.tar.gz \
                        s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz

                    echo "✅ Uploaded: s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz"
                '''
            }
        }
    }

    post {
        cleanup {
            sh 'rm -f *.tar.gz'
        }
    }
}