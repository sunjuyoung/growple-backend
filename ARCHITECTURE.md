# System Architecture

## Overview

스터디 그룹 관리를 위한 마이크로서비스 기반 백엔드 시스템

```
                                    ┌─────────────────┐
                                    │   Discovery     │
                                    │    (Eureka)     │
                                    │     :8761       │
                                    └────────┬────────┘
                                             │ Service Registry
        ┌────────────────────────────────────┼────────────────────────────────────┐
        │                                    │                                    │
        ▼                                    ▼                                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│    Member     │   │    Study      │   │   Payment     │   │     Chat      │   │    Gateway    │
│   Service     │   │   Service     │   │   Service     │   │   Service     │   │   Service     │
│    :8081      │   │    :8082      │   │    :8083      │   │    :8084      │   │    :8080      │
└───────┬───────┘   └───────┬───────┘   └───────┬───────┘   └───────┬───────┘   └───────────────┘
        │                   │                   │                   │                   ▲
        ▼                   ▼                   ▼                   ▼                   │
   PostgreSQL          PostgreSQL          PostgreSQL          PostgreSQL          Client
                       + pgvector                              + Redis
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.12 |
| Cloud | Spring Cloud 2024.0.2 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA, QueryDSL 5.1.0 |
| Service Discovery | Netflix Eureka |
| API Docs | SpringDoc OpenAPI 2.8.4 |

## Microservices

### API Gateway Service
| | |
|---|---|
| **Role** | 인증/인가, 라우팅, CORS, 로드밸런싱 |
| **Key Tech** | Spring Cloud Gateway, JWT |
| **Features** | - JWT 토큰 검증<br>- 서비스 라우팅 (`/member-service/**` → member-service)<br>- Swagger UI 통합 |

### Discovery Service
| | |
|---|---|
| **Role** | 서비스 레지스트리 |
| **Key Tech** | Netflix Eureka Server |
| **Features** | - 서비스 등록/발견<br>- 헬스 체크<br>- 로드밸런싱 지원 |

### Member Service
| | |
|---|---|
| **Role** | 회원 관리 |
| **Key Tech** | Spring Data JPA, PostgreSQL |
| **Features** | - 회원 CRUD<br>- 인증/인가<br>- 프로필 관리 |

### Study Service
| | |
|---|---|
| **Role** | 스터디 관리, AI 기반 추천 |
| **Key Tech** | Spring AI, OpenAI, pgvector, AWS S3 |
| **Features** | - 스터디 CRUD<br>- 게시판<br>- **LLM 기반 스터디 추천**<br>- **벡터 유사도 검색** |

### Payment Service
| | |
|---|---|
| **Role** | 결제 처리 |
| **Key Tech** | Toss Payments API |
| **Features** | - 결제 요청/승인<br>- 결제 내역 관리<br>- 환불 처리 |

### Chat Service
| | |
|---|---|
| **Role** | 실시간 채팅 |
| **Key Tech** | WebSocket STOMP, Redis Pub/Sub |
| **Features** | - 실시간 메시징<br>- 채팅방 관리<br>- 메시지 영속화 |

## Communication Patterns

### Service-to-Service
```
Client → Gateway → Eureka(LB) → Target Service
```
- **Gateway Routing**: Path prefix 기반 라우팅
- **Load Balancing**: Eureka + Spring Cloud LoadBalancer
- **Service Discovery**: 동적 서비스 등록/발견

### Real-time Communication
```
Client ←→ WebSocket(STOMP) ←→ Chat Service ←→ Redis Pub/Sub
```

## API Gateway Routing

| Path Pattern | Target Service |
|--------------|----------------|
| `/member-service/**` | member-service |
| `/study-service/**` | study-service |
| `/payment-service/**` | payment-service |
| `/chat-service/**` | chat-service |
| `/ws-chat/**` | chat-service (WebSocket) |

## Key Features

### AI-Powered Study Recommendation
```
User Query → OpenAI Embedding → pgvector 유사도 검색 → 추천 결과
```

### Distributed Chat System
```
Message → Chat Service → Redis Pub/Sub → All Subscribers
                ↓
           PostgreSQL (영속화)
```

## External Integrations

| Service | Provider | Purpose |
|---------|----------|---------|
| LLM | OpenAI | 스터디 추천, 임베딩 생성 |
| Payment | Toss Payments | 결제 처리 |
| Storage | AWS S3 | 파일 업로드 |
| Vector DB | pgvector | 벡터 유사도 검색 |
