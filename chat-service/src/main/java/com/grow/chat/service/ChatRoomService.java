package com.grow.chat.service;

import com.grow.chat.domain.ChatRoom;
import com.grow.chat.domain.ChatRoomMember;
import com.grow.chat.dto.ChatRoomResponse;
import com.grow.chat.repository.ChatRoomMemberRepository;
import com.grow.chat.repository.ChatRoomRepository;
import com.grow.common.InternalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional
    public ChatRoomResponse createChatRoom(Long studyId, String roomName) {
        if (chatRoomRepository.existsByStudyId(studyId)) {
            throw new IllegalArgumentException("해당 스터디의 채팅방이 이미 존재합니다.");
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .studyId(studyId)
                .name(roomName)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        return ChatRoomResponse.from(savedRoom);
    }

    @Transactional
    public ChatRoomResponse createChatRoomMember(Long studyId, Long roomId, Long userId) {

        ChatRoom chatRoom = chatRoomRepository.findByIdAndStudyId(roomId, studyId).orElseThrow();

        ChatRoom savedRoom = getChatRoom(userId, chatRoom);

        return ChatRoomResponse.from(savedRoom);
    }

    @Transactional
    public ChatRoomResponse createChatRoomMember(Long studyId,  Long userId) {

        ChatRoom chatRoom = chatRoomRepository.findByStudyId(studyId).orElseThrow();

        ChatRoom savedRoom = getChatRoom(userId, chatRoom);

        return ChatRoomResponse.from(savedRoom);
    }

    private ChatRoom getChatRoom(Long userId, ChatRoom chatRoom) {
        ChatRoomMember chatRoomMember = ChatRoomMember.of(chatRoom, userId);

        chatRoom.getMembers().add(chatRoomMember);
        chatRoomMember.assignChatRoom(chatRoom);

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        chatRoomMemberRepository.save(chatRoomMember);
        return savedRoom;
    }

    @Transactional
    public ChatRoom joinChatRoom(Long chatRoomId, Long memberId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        if (chatRoomMemberRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)) {
            log.warn("이미 입장한 채팅방입니다: chatRoomId={}, memberId={}", chatRoomId, memberId);
            return chatRoom;
        }

        ChatRoomMember member = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .memberId(memberId)
                .build();

        ChatRoomMember save = chatRoomMemberRepository.save(member);
        log.info("채팅방 입장: chatRoomId={}, memberId={}", chatRoomId, memberId);
        return chatRoom;
    }

    @Transactional
    public void leaveChatRoom(Long chatRoomId, Long memberId) {
        ChatRoomMember member = chatRoomMemberRepository.findActiveMember(chatRoomId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버를 찾을 수 없습니다."));

        member.leave();
        log.info("채팅방 퇴장: chatRoomId={}, memberId={}", chatRoomId, memberId);
    }

    public ChatRoomResponse getChatRoomByStudyId(Long studyId) {
        ChatRoom chatRoom = chatRoomRepository.findByStudyId(studyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        return ChatRoomResponse.from(chatRoom);
    }

    public ChatRoom getChatRoomEntity(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
    }

    @Transactional
    public ChatRoomResponse internalRequest(InternalRequest request) {
        ChatRoom chatRoom = createChatRoomEntity(request.studyId(), request.roomName());

        ChatRoomMember chatRoomMember = addMember(chatRoom, request.userId());

        chatRoomMemberRepository.save(chatRoomMember);

        return ChatRoomResponse.from(chatRoom);
    }

    private ChatRoom createChatRoomEntity(Long studyId, String roomName) {

        ChatRoom chatRoom = ChatRoom.builder()
                .studyId(studyId)
                .name(roomName)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    private ChatRoomMember addMember(ChatRoom chatRoom, Long userId) {
        ChatRoomMember member = ChatRoomMember.of(chatRoom, userId);
        chatRoom.getMembers().add(member);
        member.assignChatRoom(chatRoom);
        return member;
    }
}
