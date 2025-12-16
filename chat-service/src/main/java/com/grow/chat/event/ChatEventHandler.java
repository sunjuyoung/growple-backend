package com.grow.chat.event;

import com.grow.chat.service.ChatRoomService;
import com.grow.common.StudyCreateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatEventHandler {

    private final ChatRoomService chatRoomService;

    @KafkaListener(topics = Topics.STUDY_MEMBER_CREATED, groupId = "chat-service-group")
    public void chatRoomMemberHandler(StudyCreateEvent event){

        chatRoomService.createChatRoomMember(event.studyId(), event.userId());

    }
}
