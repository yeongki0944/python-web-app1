pipeline {
    agent any

    environment {
        S3_BUCKET = 'younggi-jenkins-deploy-bucket'           // S3 버킷
        APP_NAME = 'python-web-app'                          // 앱 이름
        BUILD_VERSION = "${env.BUILD_NUMBER}"                // 자동 생성
        TARGET_EC2_IP = '10.0.28.227'                       // 배포 대상 IP
        TARGET_INSTANCE_ID = 'i-0f6ca754ac4aef668'          // AMI 생성 대상
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

                        # 기존 EC2에 배포
                        ssh -i $SSH_KEY -o StrictHostKeyChecking=no ec2-user@${TARGET_EC2_IP} "
                            cd /home/ec2-user/python-web-app/script &&
                            ./deploy.sh ${BUILD_VERSION}
                        "
                    '''
                }
            }
        }

        stage('Update Launch Template') {
            steps {
                echo 'Creating AMI and updating Launch Template...'
                sh '''
                    # 최신 LT 버전 가져오기
                    LATEST_LT_VERSION=$(aws ec2 describe-launch-template-versions \
                        --launch-template-name python-web-app-lt \
                        --query 'max_by(LaunchTemplateVersions, &VersionNumber).VersionNumber' \
                        --output text)

                    echo "Using Latest Launch Template Version: $LATEST_LT_VERSION"

                    # 1단계: AMI 생성
                    echo "Creating AMI from instance: ${TARGET_INSTANCE_ID}"

                    AMI_ID=$(aws ec2 create-image \
                        --instance-id ${TARGET_INSTANCE_ID} \
                        --name "python-web-app-${BUILD_VERSION}" \
                        --description "Automated AMI for build ${BUILD_VERSION}" \
                        --query "ImageId" --output text)

                    echo "Created AMI: $AMI_ID"

                    # 2단계: AMI 생성 완료 대기
                    echo "Waiting for AMI to be available..."
                    aws ec2 wait image-available --image-ids $AMI_ID

                    # 3단계: Launch Template 새 버전 생성 (최신 버전 기준)
                    echo "Creating new Launch Template version..."
                    aws ec2 create-launch-template-version \
                        --launch-template-name python-web-app-lt \
                        --source-version $LATEST_LT_VERSION \
                        --launch-template-data "{\\"ImageId\\":\\"$AMI_ID\\"}"

                    # 4단계: Default 버전 업데이트
                    echo "Updating Launch Template default version..."
                    aws ec2 modify-launch-template \
                        --launch-template-name python-web-app-lt \
                        --default-version '$Latest'

                    echo "✅ Launch Template updated with AMI: $AMI_ID"
                '''
            }
        }

        stage('Instance Refresh') {
            steps {
                echo 'Starting Instance Refresh...'
                sh '''
                    # Instance Refresh 실행
                    echo "Starting Instance Refresh for ASG..."
                    REFRESH_ID=$(aws autoscaling start-instance-refresh \
                        --auto-scaling-group-name python-web-app-asg \
                        --preferences '{"InstanceWarmup": 120, "MinHealthyPercentage": 50}' \
                        --query "InstanceRefreshId" --output text)

                    echo "✅ Instance Refresh started: $REFRESH_ID"
                    echo "📋 Monitor progress in AWS Console: EC2 → Auto Scaling Groups → python-web-app-asg → Instance refresh"

                    # 초기 상태만 확인하고 백그라운드 실행
                    sleep 30
                    STATUS=$(aws autoscaling describe-instance-refreshes \
                        --auto-scaling-group-name python-web-app-asg \
                        --instance-refresh-ids $REFRESH_ID \
                        --query "InstanceRefreshes[0].Status" --output text)

                    echo "Instance Refresh Status: $STATUS"
                    echo "🎯 Instance Refresh is running in background"
                '''
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