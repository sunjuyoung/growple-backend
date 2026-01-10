# CI/CD Pipeline

## Architecture Overview

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   GitHub    │────▶│   Jenkins   │────▶│   AWS ECR   │────▶│    k3s      │
│  (Source)   │     │  (CI/CD)    │     │  (Registry) │     │ (Kubernetes)│
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

## Tech Stack

| Category | Technology |
|----------|------------|
| CI/CD | Jenkins Pipeline |
| Container Registry | AWS ECR |
| Container Runtime | Docker |
| Orchestration | k3s (Lightweight Kubernetes) |
| Build Tool | Gradle |
| Region | ap-northeast-2 (Seoul) |

## Pipeline Features

### Parameterized Build
- 서비스별 선택적 빌드/배포 지원
- 환경별 배포 (dev / prod)

### Parallel Build
5개 마이크로서비스 동시 빌드로 배포 시간 단축

```
┌──────────────────────────────────────────────────────┐
│                   Parallel Build                      │
├───────────┬───────────┬───────────┬────────┬─────────┤
│  Member   │   Study   │  Payment  │  Chat  │ Gateway │
│  Service  │  Service  │  Service  │ Service│ Service │
└───────────┴───────────┴───────────┴────────┴─────────┘
```

## Pipeline Stages

```
1. ECR Login        → AWS ECR 인증
2. Check Selection  → 배포 대상 서비스 확인
3. Build & Deploy   → 병렬 빌드 및 배포
   ├── Gradle Build     (JAR 생성)
   ├── Docker Build     (이미지 빌드)
   ├── ECR Push         (이미지 푸시)
   └── k3s Deploy       (Rolling Update)
```

## Kubernetes Resources

각 서비스별 K8s 리소스 구성:

```
{service}/k8s/
├── {service}-configmap.yaml   # 환경 설정
├── {service}-secret.yaml      # 민감 정보 (암호화)
├── {service}-deployment.yaml  # 배포 설정
└── {service}-service.yaml     # 서비스 노출
```

### Deployment Strategy
- **Rolling Update**: 무중단 배포
- **Image Pull Secret**: ECR 프라이빗 레지스트리 인증
- **ConfigMap/Secret 분리**: 설정과 코드 분리

## Service Ports

| Service | Container Port | NodePort |
|---------|---------------|----------|
| API Gateway | 8080 | 30080 |
| Member | 8081 | 30081 |
| Study | 8082 | 30082 |
| Payment | 8083 | 30083 |
| Chat | 8084 | 30084 |

## Key Implementations

- **선택적 배포**: Boolean 파라미터로 필요한 서비스만 배포
- **이미지 태깅**: `BUILD_NUMBER` 기반 버전 관리
- **배포 검증**: `rollout status`로 배포 완료 확인 (timeout: 300s)
- **리소스 정리**: 빌드 후 미사용 Docker 이미지 자동 정리
