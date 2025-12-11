package com.grow.chat.service;

import com.grow.chat.domain.ChatMessage;
import com.grow.chat.domain.ChatRoom;
import com.grow.chat.domain.ChatRoomMember;
import com.grow.chat.domain.MessageType;
import com.grow.chat.dto.ChatMessageRequest;
import com.grow.chat.dto.ChatMessageResponse;
import com.grow.chat.dto.UnreadMessageCountResponse;
import com.grow.chat.repository.ChatMessageRepository;
import com.grow.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomService chatRoomService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String UNREAD_COUNT_KEY_PREFIX = "chat:unread:";

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomService.getChatRoomEntity(request.getChatRoomId());

        // 발신자가 채팅방 멤버인지 확인
        boolean isMember = chatRoomMemberRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(
                request.getChatRoomId(), request.getSenderId());

        if (!isMember) {
            throw new IllegalArgumentException("채팅방 멤버가 아닙니다.");
        }

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(request.getSenderId())
                .content(request.getContent())
                .messageType(request.getMessageType())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("메시지 전송: messageId={}, chatRoomId={}, senderId={}",
                savedMessage.getId(), request.getChatRoomId(), request.getSenderId());

        // Redis 캐시 무효화 (읽지 않은 메시지 카운트)
        invalidateUnreadCountCache(request.getChatRoomId());

        return ChatMessageResponse.from(savedMessage);
    }

    @Transactional
    public ChatMessageResponse sendSystemMessage(Long chatRoomId, String content, MessageType messageType) {
        ChatRoom chatRoom = chatRoomService.getChatRoomEntity(chatRoomId);

        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(0L) // 시스템 메시지는 sender를 0으로 설정
                .content(content)
                .messageType(messageType)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        invalidateUnreadCountCache(chatRoomId);

        return ChatMessageResponse.from(savedMessage);
    }

    public Page<ChatMessageResponse> getChatHistory(Long chatRoomId, Pageable pageable) {
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(chatRoomId, pageable);
        return messages.map(ChatMessageResponse::from);
    }

    @Transactional
    public void markAsRead(Long chatRoomId, Long memberId, Long lastMessageId) {
        ChatRoomMember member = chatRoomMemberRepository.findActiveMember(chatRoomId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버를 찾을 수 없습니다."));

        member.updateLastReadMessage(lastMessageId);
        log.info("메시지 읽음 처리: chatRoomId={}, memberId={}, lastMessageId={}",
                chatRoomId, memberId, lastMessageId);

        // Redis 캐시에서 해당 멤버의 읽지 않은 메시지 카운트 삭제
        String cacheKey = getUnreadCountCacheKey(chatRoomId, memberId);
        redisTemplate.delete(cacheKey);
    }

    public UnreadMessageCountResponse getUnreadMessageCount(Long chatRoomId, Long memberId) {
        // Redis 캐시 확인
        String cacheKey = getUnreadCountCacheKey(chatRoomId, memberId);
        Object cachedCount = redisTemplate.opsForValue().get(cacheKey);

        if (cachedCount != null) {
            log.debug("Redis 캐시에서 읽지 않은 메시지 수 조회: {}", cachedCount);
            return UnreadMessageCountResponse.builder()
                    .chatRoomId(chatRoomId)
                    .unreadCount(Long.valueOf(cachedCount.toString()))
                    .build();
        }

        // 캐시 미스 시 DB 조회
        ChatRoomMember member = chatRoomMemberRepository.findActiveMember(chatRoomId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버를 찾을 수 없습니다."));

        Long lastReadMessageId = member.getLastReadMessageId() != null ? member.getLastReadMessageId() : 0L;
        Long unreadCount = chatMessageRepository.countUnreadMessages(chatRoomId, lastReadMessageId);

        // Redis 캐시에 저장 (TTL: 5분)
        redisTemplate.opsForValue().set(cacheKey, unreadCount, 5, TimeUnit.MINUTES);

        return UnreadMessageCountResponse.builder()
                .chatRoomId(chatRoomId)
                .unreadCount(unreadCount)
                .build();
    }

    private void invalidateUnreadCountCache(Long chatRoomId) {
        // 해당 채팅방의 모든 멤버의 읽지 않은 메시지 카운트 캐시 삭제
        String pattern = UNREAD_COUNT_KEY_PREFIX + chatRoomId + ":*";
        redisTemplate.keys(pattern).forEach(redisTemplate::delete);
    }

    private String getUnreadCountCacheKey(Long chatRoomId, Long memberId) {
        return UNREAD_COUNT_KEY_PREFIX + chatRoomId + ":" + memberId;
    }
}
