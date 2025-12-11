package com.grow.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadMessageCountResponse {

    private Long chatRoomId;
    private Long unreadCount;
}
