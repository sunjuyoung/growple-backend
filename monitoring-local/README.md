# Growple 로컬 모니터링

로컬 개발 환경용 Prometheus + Grafana 모니터링 스택

## 구조

```
monitoring-local/
├── docker-compose.yml
├── prometheus/
│   ├── prometheus.yml
│   └── alert-rules.yml
└── grafana/
    └── provisioning/
        └── datasources/
            └── datasource.yml
```

## 실행

```bash
cd monitoring-local
docker-compose up -d
```

## 접속

| 서비스 | URL | 계정 |
|--------|-----|------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |

## 서비스 포트

| 서비스 | 포트 |
|--------|------|
| Discovery Service | 8761 |
| API Gateway | 8080 |
| Member Service | 8081 |
| Study Service | 8082 |
| Payment Service | 8083 |
| Chat Service | 8084 |


## 종료

```bash
docker-compose down
```
