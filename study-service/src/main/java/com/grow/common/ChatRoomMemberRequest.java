package com.grow.common;

public record ChatRoomMemberRequest(
        Long studyId,
        Long roomId,
        Long userId
){
    public static ChatRoomMemberRequest create(Long userId,Long studyId,Long roomId){
        return new ChatRoomMemberRequest(studyId,roomId,userId);
    }

}