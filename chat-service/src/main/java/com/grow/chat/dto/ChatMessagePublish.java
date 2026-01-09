package com.grow.chat.dto;

import com.grow.chat.domain.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessagePublish {
    private Long chatRoomId;
    private Long id;
    private Long senderId;
    private String senderNickname;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;

    public static ChatMessagePublish from(ChatMessageResponse response) {
        return ChatMessagePublish.builder()
                .chatRoomId(response.getChatRoomId())
                .id(response.getId())
                .senderId(response.getSenderId())
                .senderNickname(response.getSenderNickname())
                .content(response.getContent())
                .messageType(response.getMessageType())
                .createdAt(response.getCreatedAt())
                .build();
    }
}