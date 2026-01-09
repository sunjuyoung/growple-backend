package com.grow.chat.config;

import com.grow.chat.dto.ChatRoomMember;
import com.grow.chat.service.ChatRoomPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ChatRoomPresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * WebSocket 연결 해제 시 호출
     * 브라우저 새로고침, 탭 닫기, 네트워크 끊김 등 모든 종료 상황 처리
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("WebSocket 연결 해제: sessionId={}", sessionId);

        // 세션 매핑을 통해 해당 사용자 제거
        ChatRoomPresenceService.SessionInfo sessionInfo = presenceService.removeMemberBySessionId(sessionId);

        if (sessionInfo != null) {
            // 접속자 목록 변경 브로드캐스트
            broadcastPresenceUpdate(sessionInfo.chatRoomId());
        }
    }

    /**
     * 채팅방 접속자 목록 변경을 구독자들에게 브로드캐스트
     */
    public void broadcastPresenceUpdate(Long chatRoomId) {
        List<ChatRoomMember> members = presenceService.getMembers(chatRoomId);

        messagingTemplate.convertAndSend(
                "/topic/chatroom/" + chatRoomId + "/presence",
                members
        );

        log.debug("접속자 목록 브로드캐스트: chatRoomId={}, count={}", chatRoomId, members.size());
    }
}
