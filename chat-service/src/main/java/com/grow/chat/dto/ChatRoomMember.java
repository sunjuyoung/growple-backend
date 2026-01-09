package com.grow.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomMember {
    private Long memberId;
    private String nickname;
    private String sessionId;
    private LocalDateTime joinedAt;
}
