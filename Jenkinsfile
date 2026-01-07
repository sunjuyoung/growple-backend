pipeline {
    agent any

    // ========== 파라미터 정의 ==========
    parameters {
        booleanParam(
            name: 'MEMBER_SERVICE',
            defaultValue: true,
            description: 'Member Service 빌드/배포'
        )
        booleanParam(
            name: 'STUDY_SERVICE',
            defaultValue: true,
            description: 'Study Service 빌드/배포'
        )
        booleanParam(
            name: 'PAYMENT_SERVICE',
            defaultValue: true,
            description: 'Payment Service 빌드/배포'
        )
        booleanParam(
            name: 'CHAT_SERVICE',
            defaultValue: true,
            description: 'Chat Service 빌드/배포'
        )
        booleanParam(
            name: 'GATEWAY_SERVICE',
            defaultValue: true,
            description: 'Gateway Service 빌드/배포'
        )
        choice(
            name: 'DEPLOY_ENV',
            choices: ['dev', 'prod'],
            description: '배포 환경 선택'
        )
    }

    // ========== 환경 변수 ==========
    environment {
        AWS_REGION = 'ap-northeast-2'
        ECR_REGISTRY = '481207241075.dkr.ecr.ap-northeast-2.amazonaws.com'
    }

    stages {
        // ========== ECR 로그인 ==========
        stage('ECR Login') {
            steps {
                sh '''
                    aws ecr get-login-password --region ${AWS_REGION} | \
                    docker login --username AWS --password-stdin ${ECR_REGISTRY}
                '''
            }
        }

        // ========== 선택 확인 ==========
        stage('Check Selection') {
            steps {
                echo "========================================"
                echo " 배포 시작"
                echo "========================================"
                echo "환경: ${params.DEPLOY_ENV}"
                echo "Member Service: ${params.MEMBER_SERVICE}"
                echo "Study Service: ${params.STUDY_SERVICE}"
                echo "Payment Service: ${params.PAYMENT_SERVICE}"
                echo "Chat Service: ${params.CHAT_SERVICE}"
                echo "Gateway Service: ${params.GATEWAY_SERVICE}"
                echo "========================================"
            }
        }

        // ========== 병렬 빌드 & 배포 ==========
        stage('Build & Deploy') {
            parallel {
                stage('Member Service') {
                    when {
                        expression { params.MEMBER_SERVICE == true }
                    }
                    steps {
                        script {
                            buildAndDeploy('member-service')
                        }
                    }
                }

                stage('Study Service') {
                    when {
                        expression { params.STUDY_SERVICE == true }
                    }
                    steps {
                        script {
                            buildAndDeploy('study-service')
                        }
                    }
                }

                stage('Payment Service') {
                    when {
                        expression { params.PAYMENT_SERVICE == true }
                    }
                    steps {
                        script {
                            buildAndDeploy('payment-service')
                        }
                    }
                }

                stage('Chat Service') {
                    when {
                        expression { params.CHAT_SERVICE == true }
                    }
                    steps {
                        script {
                            buildAndDeploy('chat-service')
                        }
                    }
                }

                stage('Gateway Service') {
                    when {
                        expression { params.GATEWAY_SERVICE == true }
                    }
                    steps {
                        script {
                            buildAndDeploy('apigateway-service')
                        }
                    }
                }
            }
        }
    }

    // ========== 빌드 후 처리 ==========
    post {
        success {
            echo '배포 성공!'
        }
        failure {
            echo '배포 실패!'
        }
        always {
            // Docker 이미지 정리 (디스크 절약)
            sh 'docker image prune -f || true'
        }
    }
}

// ========== 공통 함수 ==========
def buildAndDeploy(String serviceName) {
    echo "====== ${serviceName} 빌드 시작 ======"

      def imageTag = "${ECR_REGISTRY}/growple/${serviceName}:${BUILD_NUMBER}"
      def latestTag = "${ECR_REGISTRY}/growple/${serviceName}:latest"

    dir("${serviceName}") {

        sh 'chmod +x gradlew'
        sh './gradlew clean build -x test --no-daemon'
        // Docker 이미지 빌드
        echo "Docker 이미지 빌드 중..."
        sh "docker build -t ${imageTag} -t ${latestTag} ."

        //  ECR 푸시
        echo " ECR 푸시 중..."
        sh "docker push ${imageTag}"
    }

    // 4. k3s 배포
    echo "k3s 배포 중..."
    withCredentials([file(credentialsId: 'k3s-kubeconfig', variable: 'KUBECONFIG')]) {

            dir("${serviceName}/k8s") {
         sh """
             kubectl --kubeconfig=\$KUBECONFIG apply -f ${serviceName.replace('-service', '')}-configmap.yaml || true
             kubectl --kubeconfig=\$KUBECONFIG apply -f ${serviceName.replace('-service', '')}-secret.yaml || true
             kubectl --kubeconfig=\$KUBECONFIG apply -f ${serviceName.replace('-service', '')}-deployment.yaml
             kubectl --kubeconfig=\$KUBECONFIG apply -f ${serviceName.replace('-service', '')}-service.yaml
         """
            }

        sh """
            kubectl --kubeconfig=\$KUBECONFIG \\
                set image deployment/${serviceName} \\
                ${serviceName}=${imageTag}

            kubectl --kubeconfig=\$KUBECONFIG \\
                rollout status deployment/${serviceName} \\
                --timeout=300s
        """
    }

    echo " ${serviceName} 배포 완료!"
}