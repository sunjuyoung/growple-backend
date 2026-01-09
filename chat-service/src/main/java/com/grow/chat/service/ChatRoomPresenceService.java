package com.grow.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grow.chat.dto.ChatRoomMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomPresenceService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PRESENCE_KEY_PREFIX = "chatroom:presence:";
    private static final String SESSION_KEY_PREFIX = "chatroom:session:";
    private static final Duration PRESENCE_TTL = Duration.ofHours(24);

    /**
     * 채팅방 입장 - 접속자 추가
     */
    public void addMember(Long chatRoomId, ChatRoomMember member) {
        String presenceKey = PRESENCE_KEY_PREFIX + chatRoomId;
        String field = member.getMemberId().toString();

        try {
            String value = objectMapper.writeValueAsString(member);
            redisTemplate.opsForHash().put(presenceKey, field, value);
            redisTemplate.expire(presenceKey, PRESENCE_TTL);

            // 세션 -> 채팅방 매핑 저장 (빠른 조회용)
            String sessionKey = SESSION_KEY_PREFIX + member.getSessionId();
            String sessionValue = chatRoomId + ":" + member.getMemberId();
            redisTemplate.opsForValue().set(sessionKey, sessionValue, PRESENCE_TTL);

            log.info("접속자 추가: chatRoomId={}, memberId={}, sessionId={}",
                    chatRoomId, member.getMemberId(), member.getSessionId());
        } catch (JsonProcessingException e) {
            log.error("접속자 추가 실패", e);
        }
    }

    /**
     * 채팅방 퇴장 - 접속자 제거
     */
    public void removeMember(Long chatRoomId, Long memberId, String sessionId) {
        String presenceKey = PRESENCE_KEY_PREFIX + chatRoomId;
        redisTemplate.opsForHash().delete(presenceKey, memberId.toString());

        // 세션 매핑도 제거
        if (sessionId != null) {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
        }

        log.info("접속자 제거: chatRoomId={}, memberId={}", chatRoomId, memberId);
    }

    /**
     * 채팅방 접속자 목록 조회
     */
    public List<ChatRoomMember> getMembers(Long chatRoomId) {
        String presenceKey = PRESENCE_KEY_PREFIX + chatRoomId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(presenceKey);

        return entries.values().stream()
                .map(value -> {
                    try {
                        return objectMapper.readValue(value.toString(), ChatRoomMember.class);
                    } catch (JsonProcessingException e) {
                        log.error("역직렬화 실패", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChatRoomMember::getJoinedAt))
                .toList();
    }

    /**
     * 채팅방 접속자 수 조회
     */
    public long getMemberCount(Long chatRoomId) {
        String presenceKey = PRESENCE_KEY_PREFIX + chatRoomId;
        Long size = redisTemplate.opsForHash().size(presenceKey);
        return size != null ? size : 0;
    }

    /**
     * 특정 멤버가 접속 중인지 확인
     */
    public boolean isMemberOnline(Long chatRoomId, Long memberId) {
        String presenceKey = PRESENCE_KEY_PREFIX + chatRoomId;
        return redisTemplate.opsForHash().hasKey(presenceKey, memberId.toString());
    }

    /**
     * 세션 ID로 접속자 제거 (비정상 종료 대응)
     * 세션 매핑을 통해 O(1)로 조회
     */
    public SessionInfo removeMemberBySessionId(String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        Object sessionValue = redisTemplate.opsForValue().get(sessionKey);

        if (sessionValue == null) {
            log.debug("세션 매핑 없음: sessionId={}", sessionId);
            return null;
        }

        try {
            String[] parts = sessionValue.toString().split(":");
            Long chatRoomId = Long.parseLong(parts[0]);
            Long memberId = Long.parseLong(parts[1]);

            // 접속자 제거
            String presenceKey = PRESENCE_KEY_PREFIX + chatRoomId;
            redisTemplate.opsForHash().delete(presenceKey, memberId.toString());
            redisTemplate.delete(sessionKey);

            log.info("세션 종료로 접속자 제거: sessionId={}, chatRoomId={}, memberId={}",
                    sessionId, chatRoomId, memberId);

            return new SessionInfo(chatRoomId, memberId);
        } catch (Exception e) {
            log.error("세션 제거 중 오류: sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * 세션 정보 반환용 record
     */
    public record SessionInfo(Long chatRoomId, Long memberId) {}
}
