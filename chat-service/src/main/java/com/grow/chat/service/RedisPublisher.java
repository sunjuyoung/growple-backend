package com.grow.chat.service;

import com.grow.chat.dto.ChatMessagePublish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic chatMessageTopic;

    public void publishChatMessage(ChatMessagePublish message) {
        redisTemplate.convertAndSend(chatMessageTopic.getTopic(), message);
        log.debug("Redis 메시지 발행: chatRoomId={}", message.getChatRoomId());
    }
}