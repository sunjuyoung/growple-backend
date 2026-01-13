package com.grow.chat.dto;


import org.apache.kafka.common.protocol.types.Field;

public record ChatRoomMemberRequest(
        Long studyId,
        Long roomId,
        Long userId,
        String nickname
){

}