package com.grow.chat.controller;

import com.grow.chat.domain.ChatRoom;
import com.grow.chat.dto.ChatMessageResponse;
import com.grow.chat.dto.ChatRoomMemberRequest;
import com.grow.chat.dto.ChatRoomResponse;
import com.grow.chat.dto.UnreadMessageCountResponse;
import com.grow.chat.service.ChatMessageService;
import com.grow.chat.service.ChatRoomService;
import com.grow.common.InternalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "채팅방 멤버 생성", description = "채팅방 멤버 생성합니다")
    @PostMapping("/rooms/member")
    public ResponseEntity<ChatRoomResponse> createChatRoomMember(
           @RequestBody ChatRoomMemberRequest request) {

        log.info(request.toString());
        ChatRoomResponse response = chatRoomService.createChatRoomMember(request.studyId(), request.roomId(), request.userId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "채팅방 조회", description = "스터디 ID로 채팅방을 조회합니다")
    @GetMapping("/rooms/study/{studyId}")
    public ResponseEntity<ChatRoomResponse> getChatRoomByStudyId(@PathVariable Long studyId) {
        ChatRoomResponse response = chatRoomService.getChatRoomByStudyId(studyId);
        return  new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "채팅방 입장", description = "채팅방에 입장합니다")
    @PostMapping("/rooms/{chatRoomId}/join")
    public ResponseEntity<ChatRoomResponse> joinChatRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long memberId) {

        ChatRoom chatRoom = chatRoomService.joinChatRoom(chatRoomId, memberId);
        return new ResponseEntity<>(ChatRoomResponse.from(chatRoom), HttpStatus.OK);
    }

    @Operation(summary = "채팅방 퇴장", description = "채팅방에서 퇴장합니다")
    @PostMapping("/rooms/{chatRoomId}/leave")
    public ResponseEntity<ChatRoomResponse> leaveChatRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long memberId) {

        chatRoomService.leaveChatRoom(chatRoomId, memberId);
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "채팅 히스토리 조회", description = "채팅방의 메시지 히스토리를 페이징 조회합니다")
    @GetMapping("/rooms/{chatRoomId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getChatHistory(
            @PathVariable Long chatRoomId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ChatMessageResponse> messages = chatMessageService.getChatHistory(chatRoomId, pageable);
        return  new ResponseEntity<>(messages, HttpStatus.OK);
    }

    @Operation(summary = "메시지 읽음 처리", description = "마지막 읽은 메시지를 업데이트합니다")
    @PostMapping("/rooms/{chatRoomId}/read")
    public ResponseEntity<ChatRoomResponse> markAsRead(
            @PathVariable Long chatRoomId,
            @RequestParam Long memberId,
            @RequestParam Long lastMessageId) {

        chatMessageService.markAsRead(chatRoomId, memberId, lastMessageId);
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @Operation(summary = "읽지 않은 메시지 개수 조회", description = "읽지 않은 메시지 개수를 조회합니다")
    @GetMapping("/rooms/{chatRoomId}/unread")
    public ResponseEntity<UnreadMessageCountResponse> getUnreadMessageCount(
            @PathVariable Long chatRoomId,
            @RequestParam Long memberId) {

        UnreadMessageCountResponse response = chatMessageService.getUnreadMessageCount(chatRoomId, memberId);
        return  new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "채팅방, 채팅멤버 생성", description = " 스터디 생성시 해당 채팅방 과 멤버 생성합니다")
    @PostMapping("/internal/rooms/member")
    public ResponseEntity<ChatRoomResponse> createChatRoomMemberInternal(
            @RequestBody InternalRequest request) {

        ChatRoomResponse response = chatRoomService.internalRequest(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}




