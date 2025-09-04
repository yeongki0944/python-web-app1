pipeline {
    agent any

    environment {
        AWS_REGION = 'ap-northeast-1'
        ACCOUNT_ID = '626635430480'
        ECR_REPOSITORY = 'python-web-app'
        ECR_REGISTRY = "${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        ECS_CLUSTER = 'ecs-python-web-app'
        ECS_SERVICE = 'python-web-app-task-service-laple3tl'
        ECS_TASK_FAMILY = 'python-web-app-task'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    docker build -t ${ECR_REPOSITORY}:${IMAGE_TAG} .
                    docker build -t ${ECR_REPOSITORY}:latest .
                '''
            }
        }

        stage('Login to ECR') {
            steps {
                sh '''
                    aws ecr get-login-password --region ${AWS_REGION} | \
                        docker login --username AWS --password-stdin ${ECR_REGISTRY}
                '''
            }
        }

        stage('Push to ECR') {
            steps {
                sh '''
                    docker tag ${ECR_REPOSITORY}:${IMAGE_TAG} ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
                    docker tag ${ECR_REPOSITORY}:latest ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest
                    docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
                    docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest
                '''
            }
        }

        stage('Update ECS Task Definition') {
            steps {
                sh '''
                    aws ecs describe-task-definition \
                        --task-definition ${ECS_TASK_FAMILY} \
                        --region ${AWS_REGION} \
                        --query 'taskDefinition' \
                        --output json > current_task_def.json

                    jq --arg IMAGE "${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}" \
                        '.containerDefinitions[0].image = $IMAGE | del(.taskDefinitionArn, .revision, .status, .requiresAttributes, .placementConstraints, .compatibilities, .registeredAt, .registeredBy)' \
                        current_task_def.json > new_task_def.json

                    aws ecs register-task-definition \
                        --region ${AWS_REGION} \
                        --cli-input-json file://new_task_def.json
                '''
            }
        }

        stage('Deploy to ECS') {
            steps {
                sh '''
                    aws ecs update-service \
                        --cluster ${ECS_CLUSTER} \
                        --service ${ECS_SERVICE} \
                        --task-definition ${ECS_TASK_FAMILY} \
                        --region ${AWS_REGION}
                '''
            }
        }
    }

    post {
        cleanup {
            sh '''
                docker rmi ${ECR_REPOSITORY}:${IMAGE_TAG} || true
                docker rmi ${ECR_REPOSITORY}:latest || true
                docker rmi ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG} || true
                docker rmi ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest || true
                rm -f current_task_def.json new_task_def.json || true
            '''
        }
    }
}