package com.grow.chat.controller;

import com.grow.chat.dto.ChatMessageResponse;
import com.grow.chat.dto.ChatRoomResponse;
import com.grow.chat.dto.UnreadMessageCountResponse;
import com.grow.chat.service.ChatMessageService;
import com.grow.chat.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat API", description = "채팅 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @Operation(summary = "채팅방 생성", description = "스터디별 채팅방을 생성합니다")
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @RequestParam Long studyId,
            @RequestParam String roomName) {

        ChatRoomResponse response = chatRoomService.createChatRoom(studyId, roomName);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "채팅방 조회", description = "스터디 ID로 채팅방을 조회합니다")
    @GetMapping("/rooms/study/{studyId}")
    public ResponseEntity<ChatRoomResponse> getChatRoomByStudyId(@PathVariable Long studyId) {
        ChatRoomResponse response = chatRoomService.getChatRoomByStudyId(studyId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "채팅방 입장", description = "채팅방에 입장합니다")
    @PostMapping("/rooms/{chatRoomId}/join")
    public ResponseEntity<Void> joinChatRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long memberId) {

        chatRoomService.joinChatRoom(chatRoomId, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "채팅방 퇴장", description = "채팅방에서 퇴장합니다")
    @PostMapping("/rooms/{chatRoomId}/leave")
    public ResponseEntity<Void> leaveChatRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long memberId) {

        chatRoomService.leaveChatRoom(chatRoomId, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "채팅 히스토리 조회", description = "채팅방의 메시지 히스토리를 페이징 조회합니다")
    @GetMapping("/rooms/{chatRoomId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getChatHistory(
            @PathVariable Long chatRoomId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ChatMessageResponse> messages = chatMessageService.getChatHistory(chatRoomId, pageable);
        return ResponseEntity.ok(messages);
    }

    @Operation(summary = "메시지 읽음 처리", description = "마지막 읽은 메시지를 업데이트합니다")
    @PostMapping("/rooms/{chatRoomId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long chatRoomId,
            @RequestParam Long memberId,
            @RequestParam Long lastMessageId) {

        chatMessageService.markAsRead(chatRoomId, memberId, lastMessageId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "읽지 않은 메시지 개수 조회", description = "읽지 않은 메시지 개수를 조회합니다")
    @GetMapping("/rooms/{chatRoomId}/unread")
    public ResponseEntity<UnreadMessageCountResponse> getUnreadMessageCount(
            @PathVariable Long chatRoomId,
            @RequestParam Long memberId) {

        UnreadMessageCountResponse response = chatMessageService.getUnreadMessageCount(chatRoomId, memberId);
        return ResponseEntity.ok(response);
    }
}
