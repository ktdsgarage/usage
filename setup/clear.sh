#!/bin/bash

SHARED_PREFIX="dg0100"  # 실습 시 'tiu-dgga'로 변경 필요

# ===========================================
# Usage Pattern 실습환경 정리 스크립트
# ===========================================

# 사용법 출력
print_usage() {
    cat << EOF
사용법:
    $0 <userid>

설명:
    Usage 패턴 실습을 위해 생성한 리소스를 정리합니다.

예제:
    $0 dg0100
EOF
}

# 유틸리티 함수
log() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $1"
}

# 리소스 삭제 전 확인
confirm() {
    read -p "모든 리소스를 삭제하시겠습니까? (y/N) " response
    case "$response" in
        [yY][eE][sS]|[yY])
            return 0
            ;;
        *)
            echo "작업을 취소합니다."
            exit 1
            ;;
    esac
}

# 환경 변수 설정
setup_environment() {
    USERID=$1
    NAME="${USERID}-usage"
    NAMESPACE="${NAME}-ns"
    RESOURCE_GROUP="${SHARED_PREFIX}-rg"

    # Service Bus 설정
    SB_NAMESPACE="sb-${NAME}"
    SB_USAGE_TOPIC="usage"
    SB_NOTIFY_TOPIC="notify"
    SB_SECRET_NAME="servicebus-${NAME}"

    # MongoDB 설정
    MONGODB_HOST="mongodb"
}

# 애플리케이션 리소스 정리
cleanup_application() {
    log "애플리케이션 리소스 정리 중..."

    # Deployment 삭제
    log "Deployment 삭제 중..."
    local services=("acl-usage" "notification-mock" "sync" "query")
    for service in "${services[@]}"; do
        kubectl delete deployment $service -n $NAMESPACE 2>/dev/null || true
        kubectl delete service $service -n $NAMESPACE 2>/dev/null || true
    done

    # ConfigMap 삭제
    log "ConfigMap 삭제 중..."
    for service in "${services[@]}"; do
        kubectl delete configmap $service -n $NAMESPACE 2>/dev/null || true
    done

    # Secret 삭제
    log "Secret 삭제 중..."
    kubectl delete secret $SB_SECRET_NAME -n $NAMESPACE 2>/dev/null || true

    log "애플리케이션 리소스 정리 완료"
}

# MongoDB 정리
cleanup_mongodb() {
    log "MongoDB 리소스 정리 중..."

    # StatefulSet 삭제
    kubectl delete statefulset $MONGODB_HOST -n $NAMESPACE 2>/dev/null || true

    # Service 삭제
    kubectl delete service $MONGODB_HOST -n $NAMESPACE 2>/dev/null || true

    # PVC 삭제
    kubectl delete pvc -l app=mongodb -n $NAMESPACE 2>/dev/null || true

    log "MongoDB 리소스 정리 완료"
}

# Service Bus 정리
cleanup_servicebus() {
    log "Service Bus 정리 중..."

    # Subscription 삭제
    az servicebus topic subscription delete \
        --name sync-sub \
        --namespace-name $SB_NAMESPACE \
        --resource-group $RESOURCE_GROUP \
        --topic-name $SB_USAGE_TOPIC \
        2>/dev/null || true

    az servicebus topic subscription delete \
        --name notification-sub \
        --namespace-name $SB_NAMESPACE \
        --resource-group $RESOURCE_GROUP \
        --topic-name $SB_NOTIFY_TOPIC \
        2>/dev/null || true

    # Topic 삭제
    az servicebus topic delete \
        --name $SB_USAGE_TOPIC \
        --namespace-name $SB_NAMESPACE \
        --resource-group $RESOURCE_GROUP \
        2>/dev/null || true

    az servicebus topic delete \
        --name $SB_NOTIFY_TOPIC \
        --namespace-name $SB_NAMESPACE \
        --resource-group $RESOURCE_GROUP \
        2>/dev/null || true

    # Namespace 삭제
    az servicebus namespace delete \
        --name $SB_NAMESPACE \
        --resource-group $RESOURCE_GROUP \
        2>/dev/null || true

    log "Service Bus 정리 완료"
}

# Namespace 정리
cleanup_namespace() {
    log "Namespace 정리 중..."

    if ! kubectl get all -n $NAMESPACE 2>/dev/null | grep -q .; then
        kubectl delete namespace $NAMESPACE 2>/dev/null || true
        log "Namespace 삭제 완료"
    else
        log "경고: Namespace에 아직 리소스가 있어 삭제하지 않습니다"
    fi
}

# 메인 실행 함수
main() {
    # 사전 체크
    confirm

    log "Usage 패턴 실습환경 정리를 시작합니다..."

    # 환경 변수 설정
    setup_environment "$1"

    # 순서대로 정리 진행
    cleanup_application
    cleanup_mongodb
    cleanup_servicebus
    cleanup_namespace

    log "정리가 완료되었습니다."
    log "남은 리소스 확인:"
    kubectl get all -n $NAMESPACE 2>/dev/null || true
}

# 매개변수 검사
if [ $# -ne 1 ]; then
    print_usage
    exit 1
fi

# userid 유효성 검사
if [[ ! $1 =~ ^[a-z0-9]+$ ]]; then
    echo "Error: userid는 영문 소문자와 숫자만 사용할 수 있습니다."
    exit 1
fi

# 실행
main "$1"
