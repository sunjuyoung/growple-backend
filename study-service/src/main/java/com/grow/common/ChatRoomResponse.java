package com.grow.common;

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


}
