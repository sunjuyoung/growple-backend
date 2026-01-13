package com.grow.chat.dto;

import com.grow.chat.domain.ChatMessage;
import com.grow.chat.domain.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String senderNickname;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message, String senderNickname) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderNickname(senderNickname)
                .senderId(message.getSender())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderNickname(message.getSenderNickname())
                .senderId(message.getSender())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
