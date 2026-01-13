package com.grow.chat.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(
        name = "chat_room_members",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"chat_room_id", "member_id"})
        },
        indexes = {
                @Index(name = "idx_chat_room_member_member_id", columnList = "member_id"),
                @Index(name = "idx_chat_room_member_chat_room_id", columnList = "chat_room_id"),
        }
)
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMember extends AbstractEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    private String senderNickname;

    @Column
    private Long lastReadMessageId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column
    private LocalDateTime leftAt;


    //생성메서드
    public static ChatRoomMember of(ChatRoom chatRoom, Long memberId,String senderNickname) {
        return ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .memberId(memberId)
                .senderNickname(senderNickname)
                .build();  // joinedAt은 @Builder.Default로 자동 설정
    }



    public void assignChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }



    public void updateLastReadMessage(Long messageId) {
        this.lastReadMessageId = messageId;
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    public boolean isActiveMember() {
        return this.leftAt == null;
    }

}
