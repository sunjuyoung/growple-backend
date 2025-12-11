package com.grow.chat.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_message_chat_room_id", columnList = "chat_room_id"),
                @Index(name = "idx_message_sender_id", columnList = "sender_id"),
                @Index(name = "idx_message_created_at", columnList = "created_at"),
                @Index(name = "idx_message_room_time", columnList = "chat_room_id,created_at"),
        }
)
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
public class ChatMessage extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "sender_id", nullable = false)
    private Long sender;

    @Column(columnDefinition = "TEXT")
    private String content;

}
