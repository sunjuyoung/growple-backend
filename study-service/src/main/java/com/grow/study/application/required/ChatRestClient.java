package com.grow.study.application.required;

import com.grow.common.ChatRoomResponse;

public interface ChatRestClient {

     ChatRoomResponse createChatRoom(Long studyId, String roomName);

     ChatRoomResponse createChatRoomMember(Long studyId, Long roomId, Long userId);
}
