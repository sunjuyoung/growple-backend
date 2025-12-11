package com.grow.chat.dto;

import com.grow.chat.domain.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {

    private Long id;
    private Long studyId;
    private String name;
    private Boolean isActive;
    private Integer memberCount;
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .studyId(chatRoom.getStudyId())
                .name(chatRoom.getName())
                .isActive(chatRoom.getIsActive())
                .memberCount(chatRoom.getMembers() != null ? chatRoom.getMembers().size() : 0)
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
