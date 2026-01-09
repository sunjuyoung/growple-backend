# 채팅 서비스 - Redis Pub/Sub 기반 멀티 인스턴스 메시지 동기화

## 개요

| 항목 | 내용 |
|------|------|
| 서비스 | Chat Service (실시간 채팅) |
| 작업 | STOMP 단일 브로커 → Redis Pub/Sub 전환 |
| 환경 | k3s 클러스터, 2개 Pod 운영 |

---

## 1. 문제 상황

### 1.1 기존 아키텍처

```
┌──────────┐      WebSocket       ┌─────────────────┐
│ Client A │ ───────────────────▶ │     Pod A       │
└──────────┘                      │  STOMP Broker   │──▶ Client A, B 수신
                                  └─────────────────┘
┌──────────┐      WebSocket       ┌─────────────────┐
│ Client C │ ───────────────────▶ │     Pod B       │──▶ Client C만 수신
└──────────┘                      │  STOMP Broker   │
                                  └─────────────────┘
```

기존 구현은 Spring의 `SimpleBroker`를 사용한 인메모리 STOMP 브로커 방식이었습니다.

```java
// 기존 ChatWebSocketController.java
@MessageMapping("/chat.sendMessage")
public void sendMessage(@Payload ChatMessageRequest request) {
    ChatMessageResponse response = chatMessageService.sendMessage(request);

    // 로컬 인스턴스의 구독자에게만 전달
    messagingTemplate.convertAndSend(
            "/topic/chatroom/" + request.getChatRoomId(),
            response
    );
}
```

### 1.2 문제점

| 문제 | 설명 |
|------|------|
| **메시지 유실** | Client A가 Pod A에, Client C가 Pod B에 연결된 경우, A가 보낸 메시지를 C가 수신하지 못함 |
| **확장성 제한** | 단일 인스턴스로만 운영 가능, 수평 확장 불가 |
| **가용성 취약** | 단일 장애점(SPOF) 발생 |

### 1.3 재현 시나리오

```
1. k3s 환경에서 chat-service 2개 Pod 배포
2. 사용자 A → Pod A에 WebSocket 연결 → 채팅방 1 구독
3. 사용자 B → Pod B에 WebSocket 연결 → 채팅방 1 구독
4. 사용자 A가 메시지 전송
5. 결과: 사용자 B는 메시지를 수신하지 못함
```

---

## 2. 해결 방안

### 2.1 설계

Redis Pub/Sub을 중앙 메시지 브로커로 활용하여 모든 인스턴스 간 메시지를 동기화합니다.

```
┌──────────┐     WebSocket      ┌─────────────────┐
│ Client A │ ─────────────────▶ │     Pod A       │
└──────────┘                    │  Controller     │
                                │       │         │
                                │       ▼         │
                                │ RedisPublisher  │
                                └────────┬────────┘
                                         │ PUBLISH
                                         ▼
                                ┌─────────────────┐
                                │  Redis Server   │
                                │  chat:messages  │
                                └────────┬────────┘
                                         │ SUBSCRIBE
                        ┌────────────────┴────────────────┐
                        ▼                                 ▼
             ┌─────────────────┐               ┌─────────────────┐
             │     Pod A       │               │     Pod B       │
             │ RedisSubscriber │               │ RedisSubscriber │
             │       │         │               │       │         │
             │       ▼         │               │       ▼         │
             │ STOMP Broadcast │               │ STOMP Broadcast │
             └────────┬────────┘               └────────┬────────┘
                      │                                 │
                      ▼                                 ▼
                 Client A, B                       Client C, D
```

### 2.2 구현

#### RedisConfig.java - Redis Pub/Sub 설정

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    // 채팅 메시지 전용 토픽
    @Bean
    public ChannelTopic chatMessageTopic() {
        return new ChannelTopic("chat:messages");
    }

    // 메시지 리스너 컨테이너
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory,
            MessageListenerAdapter listenerAdapter,
            ChannelTopic chatMessageTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listenerAdapter, chatMessageTopic);
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}
```

#### RedisPublisher.java - 메시지 발행

```java
@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic chatMessageTopic;
    private final ObjectMapper objectMapper;

    public void publishChatMessage(ChatMessagePublish message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(chatMessageTopic.getTopic(), json);
            log.debug("Redis 메시지 발행: chatRoomId={}", message.getChatRoomId());
        } catch (JsonProcessingException e) {
            log.error("메시지 발행 실패", e);
        }
    }
}
```

#### RedisSubscriber.java - 메시지 구독 및 STOMP 전달

```java
@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatMessagePublish chatMessage = objectMapper.readValue(body, ChatMessagePublish.class);

            // 로컬 STOMP 브로커를 통해 해당 Pod의 구독자들에게 전달
            messagingTemplate.convertAndSend(
                    "/topic/chatroom/" + chatMessage.getChatRoomId(),
                    chatMessage
            );
        } catch (JsonProcessingException e) {
            log.error("메시지 역직렬화 실패", e);
        }
    }
}
```

#### ChatWebSocketController.java - Redis 발행으로 변경

```java
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final RedisPublisher redisPublisher;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Valid @Payload ChatMessageRequest request) {
        // 1. DB 저장
        ChatMessageResponse response = chatMessageService.sendMessage(request);

        // 2. Redis Pub/Sub을 통해 모든 인스턴스에 브로드캐스트
        ChatMessagePublish publishMessage = ChatMessagePublish.builder()
                .chatRoomId(response.getChatRoomId())
                .messageId(response.getId())
                .senderId(response.getSenderId())
                .senderNickname(request.getSenderNickname())
                .content(response.getContent())
                .messageType(response.getMessageType())
                .createdAt(response.getCreatedAt())
                .build();

        redisPublisher.publishChatMessage(publishMessage);
    }
}
```

#### ChatMessagePublish.java - Redis 전송용 DTO

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessagePublish {
    private Long chatRoomId;
    private Long messageId;
    private Long senderId;
    private String senderNickname;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;
}
```

### 2.3 메시지 흐름

```
[Client] ─── WebSocket ───▶ [Pod A: Controller]
                                    │
                                    ▼
                           [ChatMessageService]
                           DB 저장 (PostgreSQL)
                                    │
                                    ▼
                           [RedisPublisher]
                           PUBLISH chat:messages
                                    │
                                    ▼
                            [Redis Server]
                                    │
                    ┌───────────────┴───────────────┐
                    │ SUBSCRIBE                     │ SUBSCRIBE
                    ▼                               ▼
           [Pod A: RedisSubscriber]        [Pod B: RedisSubscriber]
                    │                               │
                    ▼                               ▼
           [STOMP convertAndSend]          [STOMP convertAndSend]
                    │                               │
                    ▼                               ▼
           /topic/chatroom/{id}            /topic/chatroom/{id}
                    │                               │
                    ▼                               ▼
           [Pod A 연결 Clients]            [Pod B 연결 Clients]
```

---

## 3. 결과

### 3.1 개선 효과

| 항목 | Before | After |
|------|--------|-------|
| **메시지 전달** | 동일 Pod 구독자만 수신 | 전체 Pod 구독자 수신 |
| **확장성** | 단일 인스턴스 | N개 인스턴스 수평 확장 가능 |
| **가용성** | SPOF 존재 | Pod 장애 시 다른 Pod로 트래픽 분산 |
| **아키텍처** | Stateful (세션 고정 필요) | Stateless (로드밸런싱 자유) |

### 3.2 검증 시나리오

```
1. k3s 환경에서 chat-service 2개 Pod 배포
2. Redis 단일 인스턴스 배포
3. 사용자 A → Pod A에 WebSocket 연결 → 채팅방 1 구독
4. 사용자 B → Pod B에 WebSocket 연결 → 채팅방 1 구독
5. 사용자 A가 메시지 전송
6. 결과: 사용자 A, B 모두 메시지 수신 성공
```

### 3.3 성능 특성

| 항목 | 설명 |
|------|------|
| **지연시간** | Redis Pub/Sub 추가로 ~1-2ms 증가 (무시 가능) |
| **처리량** | Redis 단일 인스턴스 기준 ~100,000 msg/sec 처리 가능 |
| **메모리** | Pub/Sub은 메시지를 저장하지 않음 (Fire-and-forget) |

### 3.4 k3s 배포 설정

```yaml
# deployment.yaml
spec:
  replicas: 2
  template:
    spec:
      containers:
      - name: chat-service
        env:
        - name: SPRING_DATA_REDIS_HOST
          value: "redis-service"
        - name: SPRING_DATA_REDIS_PORT
          value: "6379"
```

---

## 4. 추가 고려사항

### 4.1 한계점

| 한계 | 설명 | 대안 |
|------|------|------|
| 메시지 유실 가능 | Pub/Sub은 구독자 부재 시 메시지 버림 | Redis Streams 또는 Kafka 고려 |
| Redis SPOF | Redis 장애 시 전체 메시지 동기화 불가 | Redis Sentinel 또는 Cluster 구성 |

### 4.2 향후 개선 방향

- **Redis Cluster**: 고가용성 확보
- **Redis Streams**: 메시지 영속성 및 재처리 지원
- **메시지 압축**: 대용량 메시지 전송 시 네트워크 최적화

---

## 5. 핵심 정리

| 단계 | 내용 |
|------|------|
| **문제** | STOMP SimpleBroker는 인메모리 방식으로 멀티 인스턴스 환경에서 메시지 동기화 불가 |
| **해결** | Redis Pub/Sub을 중앙 메시지 브로커로 도입하여 모든 인스턴스가 동일 채널 구독 |
| **결과** | k3s 2개 Pod 환경에서 실시간 메시지 동기화 달성, Stateless 아키텍처로 수평 확장 가능 |
