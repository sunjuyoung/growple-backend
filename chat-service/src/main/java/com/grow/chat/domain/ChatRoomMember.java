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

    @Column
    private Long lastReadMessageId;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column
    private LocalDateTime leftAt;

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
