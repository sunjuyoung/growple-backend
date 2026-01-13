package com.grow.chat.event;

import com.grow.chat.service.ChatRoomService;
import com.grow.common.StudyCreateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatEventHandler {

    private final ChatRoomService chatRoomService;

    @KafkaListener(topics =
            Topics.STUDY_MEMBER_CREATED,
            groupId = "chat-service-group",
            concurrency = "2"
    )
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlt"
    )
    public void chatRoomMemberHandler(StudyCreateEvent event){

        chatRoomService.createChatRoomMember(event.studyId(), event.userId(),event.nickname());

    }

    @DltHandler
    public void handleDlt(StudyCreateEvent message,
                          @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
                          @Header(KafkaHeaders.EXCEPTION_MESSAGE) String error) {

        // 같은 클래스 내에서 DLT 처리
        log.error("DLT 도착: {}", message);
    }

}
