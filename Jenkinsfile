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
                    rm -rf build dist
                    mkdir -p build

                    # 애플리케이션 파일들 복사
                    cp *.py build/
                    cp requirements.txt build/
                    cp *.sh build/ || true
                    cp *.service build/ || true

                    # 빌드 정보 파일 생성
                    echo "BUILD_NUMBER=${BUILD_NUMBER}" > build/build_info.txt
                    echo "BUILD_TIME=$(date)" >> build/build_info.txt
                    echo "GIT_COMMIT=${GIT_COMMIT}" >> build/build_info.txt

                    # 압축 파일 생성
                    cd build
                    tar -czf ../${APP_NAME}-${BUILD_VERSION}.tar.gz .
                    cd ..

                    # 파일 확인
                    ls -la ${APP_NAME}-${BUILD_VERSION}.tar.gz
                '''
            }
        }

        stage('Upload to S3') {
            steps {
                echo 'Uploading artifact to S3...'
                sh '''
                    # S3에 빌드 아티팩트 업로드
                    aws s3 cp ${APP_NAME}-${BUILD_VERSION}.tar.gz \
                        s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz

                    # latest 태그로도 업로드 (롤백용)
                    aws s3 cp ${APP_NAME}-${BUILD_VERSION}.tar.gz \
                        s3://${S3_BUCKET}/artifacts/${APP_NAME}-latest.tar.gz

                    # 업로드 확인
                    aws s3 ls s3://${S3_BUCKET}/artifacts/
                '''
            }
        }

        stage('Verify Upload') {
            steps {
                echo 'Verifying S3 upload...'
                sh '''
                    # S3에서 파일 존재 확인
                    aws s3 ls s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz

                    echo "✅ Build ${BUILD_VERSION} successfully uploaded to S3!"
                    echo "📦 Artifact: s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz"
                '''
            }
        }
    }

    post {
        success {
            echo '🎉 Pipeline completed successfully!'
            echo "Artifact available at: s3://${S3_BUCKET}/artifacts/${APP_NAME}-${BUILD_VERSION}.tar.gz"
        }
        failure {
            echo '❌ Pipeline failed!'
        }
        cleanup {
            echo 'Cleaning up workspace...'
            sh 'rm -f *.tar.gz'
        }
    }
}