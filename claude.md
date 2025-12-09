# Growple Backend

## Project Overview
Growple은 스터디 그룹 관리를 위한 마이크로서비스 기반 백엔드 애플리케이션입니다.

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.4.12
- **Cloud**: Spring Cloud 2024.0.2
- **Build Tool**: Gradle
- **Database**: PostgreSQL 16
- **ORM**: Spring Data JPA, QueryDSL 5.1.0
- **Service Discovery**: Netflix Eureka
- **API Documentation**: SpringDoc OpenAPI 2.8.4
- **Cloud Storage**: AWS S3

## Project Structure
```
growple/
├── apigateway-service/    # API Gateway (JWT 인증, 라우팅) 
├── discovery-service/     # Eureka Server (서비스 디스커버리)
├── member-service/        # 회원 관리 서비스
├── study-service/         # 스터디 관리 서비스
├── payment-service/       # 결제 서비스 (Toss Payments)
├── chat-service/          # 채팅 서비스
└── build.gradle           # 루트 빌드 설정
```

## Architecture
- 마이크로서비스 아키텍처 (MSA)
- API Gateway 패턴
- 서비스 간 통신: Eureka + LoadBalancer


## Conventions
- 패키지 구조: `com.grow.{service-name}`
- Lombok 사용
- Jakarta Validation 사용

## Environment
- `.env` 파일을 통한 환경변수 관리 (spring-dotenv)
