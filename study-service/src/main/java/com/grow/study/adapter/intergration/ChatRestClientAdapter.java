package com.grow.study.adapter.intergration;

import com.grow.common.ChatRoomMemberRequest;
import com.grow.common.ChatRoomResponse;
import com.grow.study.application.required.ChatRestClient;
import com.grow.study.application.required.dto.MemberSummaryResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ChatRestClientAdapter implements ChatRestClient {


    private final RestClient restClient;


    public ChatRestClientAdapter(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public ChatRoomResponse createChatRoom(Long studyId, String roomName) {
        return restClient.post()
                .uri("http://chat-service/api/chat/rooms?studyId={studyId}&roomName={roomName}",
                        studyId,roomName)
                .retrieve()
                .body(ChatRoomResponse.class);

    }

    @Override
    public ChatRoomResponse createChatRoomMember(Long studyId, Long roomId, Long userId) {
        ChatRoomMemberRequest chatRoomMemberRequest = new ChatRoomMemberRequest(userId, studyId, roomId);
        return restClient.post()
                .uri("http://chat-service/api/chat/rooms/member")
                .body(chatRoomMemberRequest)
                .retrieve()
                .body(ChatRoomResponse.class);

    }
}
