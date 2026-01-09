package com.grow.chat.service;

import com.grow.chat.dto.ChatMessagePublish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final GenericJackson2JsonRedisSerializer serializer;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatMessagePublish chatMessage = (ChatMessagePublish) serializer.deserialize(message.getBody());

            log.debug("Redis 메시지 수신: chatRoomId={}", chatMessage.getChatRoomId());

            messagingTemplate.convertAndSend(
                    "/topic/chatroom/" + chatMessage.getChatRoomId(),
                    chatMessage
            );

        } catch (Exception e) {
            log.error("메시지 역직렬화 실패", e);
        }
    }
}