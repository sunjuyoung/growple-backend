package com.grow.chat.dto;


public record ChatRoomMemberRequest(
        Long studyId,
        Long roomId,
        Long userId
){

}