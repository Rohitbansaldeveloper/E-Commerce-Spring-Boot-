pipeline {
    agent any

    triggers {
        githubPush()
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        skipDefaultCheckout(true)
    }

    parameters {
        string(name: 'DOCKERHUB_NAMESPACE', defaultValue: 'YOUR_DOCKERHUB_USERNAME', description: 'Docker Hub namespace/username')
        string(name: 'IMAGE_TAG', defaultValue: '', description: 'Optional image tag. Leave empty to use Jenkins BUILD_NUMBER.')
        booleanParam(name: 'DEPLOY_TO_K8S', defaultValue: true, description: 'Deploy successfully built images to Kubernetes')
    }

    environment {
        DOCKER_CREDENTIALS = 'dockerhub-creds'
        KUBE_CREDENTIALS = 'kubeconfig-ecommerce'
        K8S_NAMESPACE = 'ecommerce'
        K8S_ENV_CREDENTIALS = 'ecommerce-k8s-env'
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build & Test') {
            steps {
                script {
                    def services = [
                        'APIGatewayMar25-main/APIGatewayMar25-main',
                        'UserServiceFeb25-main/UserServiceFeb25-main',
                        'ProductServiceFeb25-main/ProductServiceFeb25-main',
                        'CartServiceFeb25-main/CartServiceFeb25-main',
                        'OrderServiceFeb25-main/OrderServiceFeb25-main',
                        'InventoryServiceFeb25-main/InventoryServiceFeb25-main',
                        'PaymentServiceFeb25-main/PaymentServiceFeb25-main',
                        'EmailServiceFeb25-main/EmailServiceFeb25-main'
                    ]
                    services.each { dir ->
                        sh "cd '${dir}' && chmod +x mvnw && ./mvnw -q clean test"
                    }
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    def images = [
                        'api-gateway:APIGatewayMar25-main/APIGatewayMar25-main',
                        'user-service:UserServiceFeb25-main/UserServiceFeb25-main',
                        'product-service:ProductServiceFeb25-main/ProductServiceFeb25-main',
                        'cart-service:CartServiceFeb25-main/CartServiceFeb25-main',
                        'order-service:OrderServiceFeb25-main/OrderServiceFeb25-main',
                        'inventory-service:InventoryServiceFeb25-main/InventoryServiceFeb25-main',
                        'payment-service:PaymentServiceFeb25-main/PaymentServiceFeb25-main',
                        'email-service:EmailServiceFeb25-main/EmailServiceFeb25-main'
                    ]
                    def tag = params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER
                    images.each { item ->
                        def parts = item.split(':', 2)
                        sh "docker build --pull -t ${params.DOCKERHUB_NAMESPACE}/${parts[0]}:${tag} '${parts[1]}'"
                    }
                    env.IMAGE_TAG_VALUE = tag
                }
            }
        }

        stage('Push Docker Images') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', DOCKER_CREDENTIALS) {
                        ['api-gateway','user-service','product-service','cart-service','order-service','inventory-service','payment-service','email-service'].each { svc ->
                            sh "docker push ${params.DOCKERHUB_NAMESPACE}/${svc}:${env.IMAGE_TAG_VALUE}"
                        }
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when { expression { return params.DEPLOY_TO_K8S } }
            steps {
                withCredentials([
                    file(credentialsId: KUBE_CREDENTIALS, variable: 'KUBECONFIG_FILE'),
                    string(credentialsId: K8S_ENV_CREDENTIALS, variable: 'K8S_ENV')
                ]) {
                    withEnv([
                        "DOCKERHUB_NAMESPACE=${params.DOCKERHUB_NAMESPACE}",
                        "IMAGE_TAG_VALUE=${env.IMAGE_TAG_VALUE}"
                    ]) {
                        sh '''
                            set -eu
                            export KUBECONFIG="$KUBECONFIG_FILE"
                            kubectl apply -f k8s/namespace.yaml
                            kubectl create secret generic microservices-secrets \\
                              --namespace=ecommerce \\
                              --from-env-file=<(printf '%s\\n' "$K8S_ENV") \\
                              --dry-run=client -o yaml | kubectl apply -f -
                            kubectl apply -f k8s/configmap.yaml
                            kubectl apply -f k8s/stateful-infra-persistence.yaml
                            for f in k8s/mysql-user.yaml k8s/mysql-product.yaml k8s/mysql-cart.yaml k8s/mysql-order.yaml k8s/mysql-payment.yaml k8s/mysql-inventory.yaml k8s/redis.yaml k8s/kafka.yaml; do
                              kubectl apply -f "$f"
                            done
                            for f in k8s/api-gateway.yaml k8s/user-service.yaml k8s/product-service.yaml k8s/cart-service.yaml k8s/order-service.yaml k8s/inventory-service.yaml k8s/payment-service.yaml k8s/email-service.yaml; do
                              sed -e "s#__DOCKERHUB_NAMESPACE__#${DOCKERHUB_NAMESPACE}#g" -e "s#__IMAGE_TAG__#${IMAGE_TAG_VALUE}#g" "$f" | kubectl apply -f -
                            done
                            kubectl apply -f k8s/network-policies.yaml
                            kubectl apply -f k8s/ingress.yaml
                            kubectl apply -f k8s/hpa.yaml
                            for svc in api-gateway user-service product-service cart-service order-service inventory-service payment-service email-service; do
                              kubectl rollout status deployment/"$svc" -n ecommerce --timeout=180s
                            done
                            kubectl get pods -n ecommerce
                            kubectl get ingress -n ecommerce
                            kubectl get hpa -n ecommerce
                        '''
                    }
                }
            }
        }
    }

    post {
        success { echo "CI/CD pipeline completed successfully. Image tag: ${env.IMAGE_TAG_VALUE}" }
        failure { echo 'Pipeline failed. Check the failed stage above.' }
        always { sh 'docker image prune -f || true' }
    }
}
