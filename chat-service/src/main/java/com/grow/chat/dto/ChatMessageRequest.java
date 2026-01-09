package com.grow.chat.dto;

import com.grow.chat.domain.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @NotNull(message = "채팅방 ID는 필수입니다")
    private Long chatRoomId;

    @NotNull(message = "발신자 ID는 필수입니다")
    private Long senderId;

    @NotBlank(message = "메시지 내용은 필수입니다")
    private String content;

    private String senderNickname;

    @Builder.Default
    private MessageType messageType = MessageType.CHAT;
}
