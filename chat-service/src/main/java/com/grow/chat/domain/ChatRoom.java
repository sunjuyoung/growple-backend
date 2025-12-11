package com.grow.chat.domain;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.Assert.state;

@Table(
        name = "chat_rooms",
        indexes = {
                @Index(name = "idx_chat_room_created_at", columnList = "created_at"),
                @Index(name = "idx_chat_room_study_id", columnList = "study_id"),
        }
)
@Entity
@Getter
@Builder
@ToString(callSuper = true, exclude = {"members", "messages"})
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoom extends AbstractEntity {

    @Column(name = "study_id", nullable = false, unique = true)
    private Long studyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE)
    private List<ChatRoomMember> members = new ArrayList<>();

    //orphanRemoval 속성 추가 관계를 맺은 ReadStatus도 같이 삭제
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();


    public void active() {
        state(isActive == false, "채팅방은 활성화 상태에서 비활성화 상태로만 변경할 수 있습니다.");
        this.isActive = true;
    }

    public void deActive(){
        state(isActive == true, "채팅방은 비활성화 상태에서 활성화 상태로만 변경할 수 있습니다.");
        this.isActive = false;
    }
}
