package com.grow.chat.controller;

import com.grow.chat.config.WebSocketEventListener;
import com.grow.chat.dto.ChatMessagePublish;
import com.grow.chat.dto.ChatMessageRequest;
import com.grow.chat.dto.ChatMessageResponse;
import com.grow.chat.dto.ChatRoomMember;
import com.grow.chat.service.ChatMessageService;
import com.grow.chat.service.ChatRoomPresenceService;
import com.grow.chat.service.RedisPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisPublisher redisPublisher;
    private final ChatRoomPresenceService presenceService;
    private final WebSocketEventListener eventListener;

    /**
     * 클라이언트가 /app/chat.sendMessage 로 메시지를 보내면 이 메서드가 처리
     * 처리 후 /topic/chatroom/{chatRoomId} 를 구독하는 모든 클라이언트에게 브로드캐스트
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Valid @Payload ChatMessageRequest request) {
        log.info("WebSocket 메시지 수신: chatRoomId={}, senderId={}",
                request.getChatRoomId(), request.getSenderId());

        try {
            // 메시지 저장
            ChatMessageResponse response = chatMessageService.sendMessage(request);

            // Redis Pub/Sub을 통해 모든 인스턴스에 브로드캐스트
            ChatMessagePublish publishMessage = ChatMessagePublish.builder()
                    .chatRoomId(response.getChatRoomId())
                    .id(response.getId())
                    .senderId(response.getSenderId())
                    .senderNickname(request.getSenderNickname())
                    .content(response.getContent())
                    .messageType(response.getMessageType())
                    .createdAt(response.getCreatedAt())
                    .build();

            redisPublisher.publishChatMessage(publishMessage);
            log.info("Redis 메시지 발행 완료: id={}", response.getId());

        } catch (Exception e) {
            log.error("메시지 전송 실패: {}", e.getMessage(), e);
            // 에러 메시지를 발신자에게만 전송
            messagingTemplate.convertAndSendToUser(
                    request.getSenderId().toString(),
                    "/queue/errors",
                    "메시지 전송에 실패했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 사용자가 채팅방에 입장했을 때
     */
    @MessageMapping("/chat.join")
    public void joinChatRoom(@Payload ChatMessageRequest request,
                             SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("채팅방 입장: chatRoomId={}, memberId={}, sessionId={} senderNickname={}",
                request.getChatRoomId(), request.getSenderId(), sessionId , request.getSenderNickname());

        try {
            // 접속자 목록에 추가
            ChatRoomMember member = ChatRoomMember.builder()
                    .memberId(request.getSenderId())
                    .nickname(request.getSenderNickname())
                    .sessionId(sessionId)
                    .joinedAt(LocalDateTime.now())
                    .build();
            presenceService.addMember(request.getChatRoomId(), member);

            // 입장 시스템 메시지 생성
            ChatMessageResponse response = chatMessageService.sendSystemMessage(
                    request.getChatRoomId(),
                    request.getSenderNickname() + "님이 입장하셨습니다.",
                    request.getMessageType()
            );

            // Redis Pub/Sub을 통해 모든 인스턴스에 브로드캐스트
            ChatMessagePublish publishMessage = ChatMessagePublish.builder()
                    .chatRoomId(response.getChatRoomId())
                    .id(response.getId())
                    .senderId(response.getSenderId())
                    .senderNickname(request.getSenderNickname())
                    .content(response.getContent())
                    .messageType(response.getMessageType())
                    .createdAt(response.getCreatedAt())
                    .build();

            redisPublisher.publishChatMessage(publishMessage);

            // 접속자 목록 변경 브로드캐스트
            eventListener.broadcastPresenceUpdate(request.getChatRoomId());

        } catch (Exception e) {
            log.error("입장 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 사용자가 채팅방에서 퇴장했을 때
     */
    @MessageMapping("/chat.leave")
    public void leaveChatRoom(@Payload ChatMessageRequest request,
                              SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("채팅방 퇴장: chatRoomId={}, memberId={}, sessionId={}",
                request.getChatRoomId(), request.getSenderId(), sessionId);

        try {
            // 접속자 목록에서 제거
            presenceService.removeMember(request.getChatRoomId(), request.getSenderId(), sessionId);

            // 퇴장 시스템 메시지 생성
            ChatMessageResponse response = chatMessageService.sendSystemMessage(
                    request.getChatRoomId(),
                    request.getSenderNickname() + "님이 퇴장하셨습니다.",
                    request.getMessageType()
            );

            // Redis Pub/Sub을 통해 모든 인스턴스에 브로드캐스트
            ChatMessagePublish publishMessage = ChatMessagePublish.builder()
                    .chatRoomId(response.getChatRoomId())
                    .id(response.getId())
                    .senderId(response.getSenderId())
                    .senderNickname(request.getSenderNickname())
                    .content(response.getContent())
                    .messageType(response.getMessageType())
                    .createdAt(response.getCreatedAt())
                    .build();

            redisPublisher.publishChatMessage(publishMessage);

            // 접속자 목록 변경 브로드캐스트
            eventListener.broadcastPresenceUpdate(request.getChatRoomId());

        } catch (Exception e) {
            log.error("퇴장 처리 실패: {}", e.getMessage(), e);
        }
    }
}
