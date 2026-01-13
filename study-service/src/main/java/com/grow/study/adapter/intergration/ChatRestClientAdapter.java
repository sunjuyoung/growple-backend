package com.grow.study.adapter.intergration;

import com.grow.common.ChatRoomMemberRequest;
import com.grow.common.ChatRoomResponse;
import com.grow.common.InternalRequest;
import com.grow.study.application.required.ChatRestClient;
import com.grow.study.application.required.dto.MemberSummaryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ChatRestClientAdapter implements ChatRestClient {


    private final RestClient restClient;


    public ChatRestClientAdapter(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public ChatRoomResponse createChatRoom(Long studyId, String roomName, Long userId,String nickname) {

        InternalRequest request = new InternalRequest(studyId, roomName, userId, nickname);
        return restClient.post()
                .uri("http://chat-service/api/chat/internal/rooms/member")
                .body(request)
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
