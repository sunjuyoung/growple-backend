# Chat Service

Growple 플랫폼의 실시간 채팅 마이크로서비스

## Tech Stack

| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 3.4.x |
| WebSocket | STOMP over WebSocket + SockJS |
| Message Broker | Redis Pub/Sub (멀티 인스턴스 동기화) |
| Database | PostgreSQL |
| Event Streaming | Apache Kafka |
| Service Discovery | Netflix Eureka |

---

## Architecture

### 멀티 인스턴스 메시지 동기화 (k3s 환경)

```
┌─────────────┐     WebSocket      ┌─────────────────────┐
│   Client A  │ ─────────────────▶ │   Pod A (chat-svc)  │
└─────────────┘                    │   - Controller      │
                                   │   - RedisPublisher  │
                                   └──────────┬──────────┘
                                              │ publish
                                              ▼
                                   ┌─────────────────────┐
                                   │   Redis Pub/Sub     │
                                   │   Topic: chat:messages
                                   └──────────┬──────────┘
                                              │ subscribe
                         ┌────────────────────┼────────────────────┐
                         ▼                    ▼                    ▼
              ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
              │  Pod A          │  │  Pod B          │  │  Pod N          │
              │  RedisSubscriber│  │  RedisSubscriber│  │  RedisSubscriber│
              │       │         │  │       │         │  │       │         │
              │       ▼         │  │       ▼         │  │       ▼         │
              │  STOMP Broker   │  │  STOMP Broker   │  │  STOMP Broker   │
              └────────┬────────┘  └────────┬────────┘  └────────┬────────┘
                       │                    │                    │
                       ▼                    ▼                    ▼
                  [Clients]            [Clients]            [Clients]
```

---

## Domain Model

### ERD

```
┌─────────────────────┐       ┌─────────────────────┐       ┌─────────────────────┐
│     chat_rooms      │       │  chat_room_members  │       │    chat_messages    │
├─────────────────────┤       ├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │◀──┐   │ id (PK)             │       │ id (PK)             │
│ study_id (UNIQUE)   │   │   │ chat_room_id (FK)   │───────│ chat_room_id (FK)   │
│ name                │   └───│ member_id           │       │ sender_id           │
│ is_active           │       │ last_read_message_id│       │ content             │
│ created_at          │       │ joined_at           │       │ message_type        │
└─────────────────────┘       │ left_at             │       │ created_at          │
                              └─────────────────────┘       └─────────────────────┘
```

### MessageType

| Type | 설명 |
|------|------|
| `CHAT` | 일반 채팅 메시지 |
| `JOIN` | 입장 시스템 메시지 |
| `LEAVE` | 퇴장 시스템 메시지 |

---

## API Endpoints

### REST API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/chat/rooms` | 채팅방 생성 |
| `POST` | `/api/chat/rooms/member` | 채팅방 멤버 추가 |
| `GET` | `/api/chat/rooms/study/{studyId}` | 스터디별 채팅방 조회 |
| `POST` | `/api/chat/rooms/{chatRoomId}/join` | 채팅방 입장 |
| `POST` | `/api/chat/rooms/{chatRoomId}/leave` | 채팅방 퇴장 |
| `GET` | `/api/chat/rooms/{chatRoomId}/messages` | 채팅 히스토리 조회 (페이징) |
| `POST` | `/api/chat/rooms/{chatRoomId}/read` | 메시지 읽음 처리 |
| `GET` | `/api/chat/rooms/{chatRoomId}/unread` | 읽지 않은 메시지 수 조회 |
| `POST` | `/api/chat/internal/rooms/member` | (내부) 스터디 생성 시 채팅방/멤버 생성 |

### WebSocket Endpoints

| Endpoint | 설명 |
|----------|------|
| `/ws-chat` | WebSocket 연결 (SockJS fallback 지원) |

### STOMP Destinations

| Type | Destination | 설명 |
|------|-------------|------|
| Subscribe | `/topic/chatroom/{chatRoomId}` | 채팅방 메시지 구독 |
| Subscribe | `/user/{userId}/queue/errors` | 에러 메시지 수신 |
| Send | `/app/chat.sendMessage` | 메시지 전송 |
| Send | `/app/chat.join` | 채팅방 입장 알림 |
| Send | `/app/chat.leave` | 채팅방 퇴장 알림 |

---

## Message Flow

### 메시지 전송 흐름

```
1. Client → WebSocket → /app/chat.sendMessage
2. ChatWebSocketController.sendMessage()
3. ChatMessageService.sendMessage() → DB 저장
4. RedisPublisher.publishChatMessage() → Redis 발행
5. RedisSubscriber.onMessage() → 각 Pod에서 수신
6. SimpMessagingTemplate.convertAndSend() → STOMP 브로드캐스트
7. /topic/chatroom/{chatRoomId} 구독자들에게 전달
```

### Request/Response DTO

**ChatMessageRequest** (전송 요청)
```json
{
  "chatRoomId": 1,
  "senderId": 123,
  "senderNickname": "홍길동",
  "content": "안녕하세요!",
  "messageType": "CHAT"
}
```

**ChatMessagePublish** (Redis Pub/Sub 메시지)
```json
{
  "chatRoomId": 1,
  "messageId": 456,
  "senderId": 123,
  "senderNickname": "홍길동",
  "content": "안녕하세요!",
  "messageType": "CHAT",
  "createdAt": "2025-01-09T14:30:00"
}
```

---

## Features

### 1. 채팅방 관리
- 스터디별 1:1 매핑 채팅방 생성
- 채팅방 활성화/비활성화 상태 관리
- 멤버 입장/퇴장 처리

### 2. 실시간 메시징
- STOMP over WebSocket 프로토콜
- SockJS fallback 지원 (브라우저 호환성)
- Redis Pub/Sub 기반 멀티 인스턴스 동기화

### 3. 메시지 읽음 처리
- 마지막 읽은 메시지 ID 추적
- 읽지 않은 메시지 수 조회
- Redis 캐싱 (TTL: 5분)

### 4. 이벤트 기반 연동 (Kafka)
- `study.member.created` 토픽 구독
- 스터디 멤버 가입 시 자동 채팅방 멤버 추가
- 재시도 정책: 3회 시도, 지수 백오프 (1s, 2s, 4s)
- DLT(Dead Letter Topic) 처리

---

## Configuration

### application.yml

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    consumer:
      group-id: chat-service-group
```

### Redis Topic

| Topic | 용도 |
|-------|------|
| `chat:messages` | 채팅 메시지 브로드캐스트 |
| `chat:unread:{chatRoomId}:{memberId}` | 읽지 않은 메시지 수 캐시 |

---

## Class Structure

```
com.grow.chat
├── config
│   ├── RedisConfig.java          # Redis 연결 및 Pub/Sub 설정
│   └── WebSocketConfig.java      # STOMP WebSocket 설정
├── controller
│   ├── ChatController.java       # REST API
│   └── ChatWebSocketController.java  # WebSocket 메시지 핸들러
├── service
│   ├── ChatRoomService.java      # 채팅방 비즈니스 로직
│   ├── ChatMessageService.java   # 메시지 비즈니스 로직
│   ├── RedisPublisher.java       # Redis 메시지 발행
│   └── RedisSubscriber.java      # Redis 메시지 구독 → STOMP 전달
├── domain
│   ├── ChatRoom.java             # 채팅방 엔티티
│   ├── ChatMessage.java          # 메시지 엔티티
│   ├── ChatRoomMember.java       # 채팅방 멤버 엔티티
│   └── MessageType.java          # 메시지 타입 enum
├── dto
│   ├── ChatMessageRequest.java   # 메시지 전송 요청
│   ├── ChatMessageResponse.java  # 메시지 응답
│   ├── ChatMessagePublish.java   # Redis Pub/Sub DTO
│   └── ChatRoomResponse.java     # 채팅방 응답
├── repository
│   ├── ChatRoomRepository.java
│   ├── ChatMessageRepository.java
│   └── ChatRoomMemberRepository.java
└── event
    ├── ChatEventHandler.java     # Kafka 이벤트 핸들러
    └── Topics.java               # Kafka 토픽 상수
```

---

## Client Integration

### JavaScript (SockJS + STOMP)

```javascript
const socket = new SockJS('/ws-chat');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // 채팅방 구독
    stompClient.subscribe('/topic/chatroom/' + chatRoomId, function(message) {
        const chatMessage = JSON.parse(message.body);
        displayMessage(chatMessage);
    });

    // 에러 메시지 구독
    stompClient.subscribe('/user/' + userId + '/queue/errors', function(error) {
        console.error('Error:', error.body);
    });
});

// 메시지 전송
function sendMessage(content) {
    stompClient.send('/app/chat.sendMessage', {}, JSON.stringify({
        chatRoomId: chatRoomId,
        senderId: userId,
        senderNickname: nickname,
        content: content,
        messageType: 'CHAT'
    }));
}

// 입장 알림
function joinRoom() {
    stompClient.send('/app/chat.join', {}, JSON.stringify({
        chatRoomId: chatRoomId,
        senderId: userId,
        content: nickname,
        messageType: 'JOIN'
    }));
}
```

---

## Deployment (k3s)

### 환경변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `SPRING_DATASOURCE_URL` | PostgreSQL URL | - |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 | - |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | - |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | localhost |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | 6379 |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 브로커 | localhost:9092 |

### 스케일링

- Redis Pub/Sub을 통해 N개의 Pod 간 메시지 동기화 지원
- Stateless 설계로 수평 확장 가능
- WebSocket 연결은 각 Pod에서 개별 관리
