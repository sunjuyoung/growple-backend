package com.grow.chat.controller;

import com.grow.chat.dto.ChatMessageRequest;
import com.grow.chat.dto.ChatMessageResponse;
import com.grow.chat.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 클라이언트가 /app/chat.sendMessage 로 메시지를 보내면 이 메서드가 처리
     * 처리 후 /topic/chatroom/{chatRoomId} 를 구독하는 모든 클라이언트에게 브로드캐스트
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Valid @Payload ChatMessageRequest request) {
        log.info("WebSocket 메시지 수신: chatRoomId={}, senderId={}",
                request.getChatRoomId(), request.getSenderId());

        try {
            // 메시지 저장 및 전송
            ChatMessageResponse response = chatMessageService.sendMessage(request);

            // 채팅방 구독자들에게 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chatroom/" + request.getChatRoomId(),
                    response
            );

            log.info("메시지 브로드캐스트 완료: messageId={}", response.getId());

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
    public void joinChatRoom(@Payload ChatMessageRequest request) {
        log.info("채팅방 입장: chatRoomId={}, memberId={}",
                request.getChatRoomId(), request.getSenderId());

        try {
            // 입장 시스템 메시지 생성 및 브로드캐스트
            ChatMessageResponse response = chatMessageService.sendSystemMessage(
                    request.getChatRoomId(),
                    request.getContent() + "님이 입장하셨습니다.",
                    request.getMessageType()
            );

            messagingTemplate.convertAndSend(
                    "/topic/chatroom/" + request.getChatRoomId(),
                    response
            );

        } catch (Exception e) {
            log.error("입장 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 사용자가 채팅방에서 퇴장했을 때
     */
    @MessageMapping("/chat.leave")
    public void leaveChatRoom(@Payload ChatMessageRequest request) {
        log.info("채팅방 퇴장: chatRoomId={}, memberId={}",
                request.getChatRoomId(), request.getSenderId());

        try {
            // 퇴장 시스템 메시지 생성 및 브로드캐스트
            ChatMessageResponse response = chatMessageService.sendSystemMessage(
                    request.getChatRoomId(),
                    request.getContent() + "님이 퇴장하셨습니다.",
                    request.getMessageType()
            );

            messagingTemplate.convertAndSend(
                    "/topic/chatroom/" + request.getChatRoomId(),
                    response
            );

        } catch (Exception e) {
            log.error("퇴장 처리 실패: {}", e.getMessage(), e);
        }
    }
}
