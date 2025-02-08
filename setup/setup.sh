#!/bin/bash

SHARED_PREFIX="dg0100"  #실습 시 tiu-dgga로 변경

# ===========================================
# ACL과 Strangler Fig 패턴 실습환경 구성 스크립트
# ===========================================

# 사용법 출력
print_usage() {
	cat << EOF
사용법:
	$0 <userid>

설명:
	ACL과 Strangler Fig 패턴 실습을 위한 Azure 리소스를 생성합니다.
	리소스 이름이 중복되지 않도록 userid를 prefix로 사용합니다.

예제:
	$0 dg0100
EOF
}

# 유틸리티 함수
log() {
	local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
	echo "[$timestamp] $1" | tee -a $LOG_FILE
}

check_error() {
	local status=$?
	if [ $status -ne 0 ]; then
		log "Error: $1 (Exit Code: $status)"
		exit $status
	fi
}

# Azure CLI 로그인 체크
check_azure_cli() {
	log "Azure CLI 로그인 상태 확인 중..."
	if ! az account show &> /dev/null; then
		log "Azure CLI 로그인이 필요합니다."
		az login --use-device-code
		check_error "Azure 로그인 실패"
	fi
}

# 환경 변수 설정
setup_environment() {
   USERID=$1
   NAME="${USERID}-usage"
   NAMESPACE="${NAME}-ns"
   RESOURCE_GROUP="${SHARED_PREFIX}-rg"
   LOCATION="koreacentral"
	 AKS_NAME="${USERID}-aks"
	 ACR_NAME="${USERID}cr"

   # Database 설정
   MONGODB_HOST="mongodb"
   MONGODB_PORT="27017"
   MONGODB_DATABASE="usagedb"
   MONGODB_USER="root"
   MONGODB_PASSWORD="Passw0rd"

   # Service Bus 설정
   SB_NAMESPACE="sb-${NAME}"
   SB_USAGE_TOPIC="usage"
   SB_NOTIFY_TOPIC="notify"

   # Secret 이름
   SB_SECRET_NAME="servicebus-${NAME}"

   LOG_FILE="deployment_${NAME}.log"
}

# Namespace 생성 추가
check_namespace() {
   # Namespace 존재 여부 확인
   if ! kubectl get namespace $NAMESPACE &>/dev/null; then
       kubectl create namespace $NAMESPACE
       check_error "Namespace 생성 실패"
       log "네임스페이스 $NAMESPACE 생성됨"
   else
       log "네임스페이스 $NAMESPACE 이미 존재함"
   fi

   # 현재 컨텍스트의 네임스페이스 변경
   kubectl config set-context --current --namespace=$NAMESPACE
   check_error "네임스페이스 컨텍스트 변경 실패"
}

# Service Bus 생성
setup_servicebus() {
   log "Service Bus 설정 중..."

   # Namespace 생성
   if ! az servicebus namespace show --name $SB_NAMESPACE -g $RESOURCE_GROUP &>/dev/null; then
       az servicebus namespace create \
           --name $SB_NAMESPACE \
           --resource-group $RESOURCE_GROUP \
           --location $LOCATION \
           --sku Standard
       check_error "Service Bus Namespace 생성 실패"
   fi

   # Topic 생성
   for topic in $SB_USAGE_TOPIC $SB_NOTIFY_TOPIC; do
       if ! az servicebus topic show --name $topic --namespace $SB_NAMESPACE -g $RESOURCE_GROUP &>/dev/null; then
           az servicebus topic create \
               --name $topic \
               --namespace $SB_NAMESPACE \
               --resource-group $RESOURCE_GROUP
           check_error "Topic $topic 생성 실패"
       fi
   done

   # Subscription 생성
   az servicebus topic subscription create \
       --name sync-sub \
       --namespace $SB_NAMESPACE \
       --resource-group $RESOURCE_GROUP \
       --topic-name $SB_USAGE_TOPIC

   az servicebus topic subscription create \
       --name notification-sub \
       --namespace $SB_NAMESPACE \
       --resource-group $RESOURCE_GROUP \
       --topic-name $SB_NOTIFY_TOPIC

   # Connection String 가져오기
   SB_CONNECTION_STRING=$(az servicebus namespace authorization-rule keys list \
       --name RootManageSharedAccessKey \
       --namespace $SB_NAMESPACE \
       --resource-group $RESOURCE_GROUP \
       --query primaryConnectionString -o tsv)
}

# Service Bus Secret 생성
setup_servicebus_secret() {
   kubectl create secret generic $SB_SECRET_NAME \
       --namespace $NAMESPACE \
       --from-literal=azure.servicebus.connection-string=$SB_CONNECTION_STRING \
       --from-literal=azure.servicebus.usage-topic=$SB_USAGE_TOPIC \
       --from-literal=azure.servicebus.notify-topic=$SB_NOTIFY_TOPIC \
       2>/dev/null || true
}

# MongoDB 설정 추가
setup_mongodb() {
   log "MongoDB 설정 중..."

   # MongoDB StatefulSet 생성
   cat << EOF | kubectl apply -f -
apiVersion: apps/v1
kind: StatefulSet
metadata:
 name: $MONGODB_HOST
 namespace: $NAMESPACE
spec:
 serviceName: $MONGODB_HOST
 replicas: 1
 selector:
   matchLabels:
     app: mongodb
 template:
   metadata:
     labels:
       app: mongodb
   spec:
     containers:
     - name: mongodb
       image: mongo:4.4
       env:
       - name: MONGO_INITDB_ROOT_USERNAME
         value: $MONGODB_USER
       - name: MONGO_INITDB_ROOT_PASSWORD
         value: $MONGODB_PASSWORD
       - name: MONGO_INITDB_DATABASE
         value: $MONGODB_DATABASE
       ports:
       - containerPort: 27017
       volumeMounts:
       - name: mongodb-data
         mountPath: /data/db
 volumeClaimTemplates:
 - metadata:
     name: mongodb-data
   spec:
     accessModes: [ "ReadWriteOnce" ]
     resources:
       requests:
         storage: 1Gi
---
apiVersion: v1
kind: Service
metadata:
 name: $MONGODB_HOST
 namespace: $NAMESPACE
spec:
 selector:
   app: mongodb
 ports:
 - port: 27017
   targetPort: 27017
 type: ClusterIP
EOF

   # MongoDB Pod Ready 대기
   kubectl wait --for=condition=ready pod -l app=mongodb -n $NAMESPACE --timeout=300s
   check_error "MongoDB Pod 준비 실패"
}

# k8s object 삭제
clear_resources() {
	# 기존 리소스 삭제
	kubectl delete deploy --all -n $NAMESPACE 2>/dev/null || true
	kubectl delete sts --all -n $NAMESPACE 2>/dev/null || true
	kubectl delete pvc --all -n $NAMESPACE 2>/dev/null || true
	kubectl delete cm --all -n $NAMESPACE 2>/dev/null || true
	kubectl delete secret --all -n $NAMESPACE 2>/dev/null || true
}

# ConfigMap 생성
setup_configmap() {
   # ACL Usage ConfigMap
   cat << EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
 name: acl-usage
 namespace: $NAMESPACE
data:
 APP_NAME: "acl-usage-service"
 SERVER_PORT: "8080"
EOF

   # Notification Mock ConfigMap
   cat << EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
 name: notification-mock
 namespace: $NAMESPACE
data:
 APP_NAME: "notification-mock-service"
 SERVER_PORT: "8080"
EOF

   # Sync ConfigMap
   cat << EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
 name: sync
 namespace: $NAMESPACE
data:
 APP_NAME: "sync-service"
 SERVER_PORT: "8080"
 MONGODB_HOST: "$MONGODB_HOST"
 MONGODB_PORT: "$MONGODB_PORT"
 MONGODB_DATABASE: "$MONGODB_DATABASE"
 MONGODB_USER: "$MONGODB_USER"
 MONGODB_PASSWORD: "$MONGODB_PASSWORD"
EOF

   # Query ConfigMap
   cat << EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
 name: query
 namespace: $NAMESPACE
data:
 APP_NAME: "query-service"
 SERVER_PORT: "8080"
 MONGODB_HOST: "$MONGODB_HOST"
 MONGODB_PORT: "$MONGODB_PORT"
 MONGODB_DATABASE: "$MONGODB_DATABASE"
 MONGODB_USER: "$MONGODB_USER"
 MONGODB_PASSWORD: "$MONGODB_PASSWORD"
EOF
}

# 이미지 빌드/업르도
create_image() {
	local service_name=$1
	log "${service_name} 이미지 빌드/업로드..."

	# JAR 빌드 (멀티프로젝트 빌드)
	./gradlew ${service_name}:clean ${service_name}:build -x test
	check_error "${service_name} jar 빌드 실패"

	# Dockerfile 생성
	cat > "${service_name}/Dockerfile" << EOF
FROM eclipse-temurin:17-jdk-alpine
COPY build/libs/${service_name}.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EOF

	# 이미지 빌드
	cd "${service_name}"
	az acr build \
		--registry $ACR_NAME \
		--image "usage/${service_name}:v1" \
		--file Dockerfile \
		.
	cd ..
	check_error "${service_name} 이미지 빌드 실패"
}

# 서비스 배포
deploy_service() {
   local name=$1
   log "${name} 서비스 배포중 ..."
   local port=$2
   local replicas=1
   local image="${ACR_NAME}.azurecr.io/usage/${name}:v1"

   cat << EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
 name: $name
 namespace: $NAMESPACE
spec:
 replicas: $replicas
 selector:
   matchLabels:
     app: $name
 template:
   metadata:
     labels:
       app: $name
     annotations:
       prometheus.io/scrape: "true"
       prometheus.io/path: "/actuator/prometheus"
       prometheus.io/port: "$port"
   spec:
     containers:
     - name: $name
       image: $image
       imagePullPolicy: Always
       ports:
       - containerPort: $port
       envFrom:
       - configMapRef:
           name: $name
       - secretRef:
           name: $SB_SECRET_NAME
EOF

   cat << EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
 name: $name
 namespace: $NAMESPACE
spec:
 selector:
   app: $name
 ports:
 - port: 80
   targetPort: $port
 type: LoadBalancer
EOF
}

main() {
    if [ $# -ne 1 ]; then
        print_usage
        exit 1
    fi

    if [[ ! $1 =~ ^[a-z0-9]+$ ]]; then
        echo "Error: userid는 영문 소문자와 숫자만 사용할 수 있습니다."
        exit 1
    fi

    setup_environment "$1"
    check_azure_cli
    check_namespace
    clear_resources
    setup_servicebus
    setup_mongodb
    setup_configmap
    setup_servicebus_secret

    # 서비스 배포 루프 수정
    create_image "acl-usage"
    create_image "notification-mock"
    create_image "sync"
    create_image "query"

    services=("acl-usage" "notification-mock" "sync" "query")

    for service in "${services[@]}"; do
        deploy_service "$service" "8080"
    done

    log "모든 서비스 배포 완료"
}



main "$@"
