package com.grow.chat.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Builder.Default
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE)
    private List<ChatRoomMember> members = new ArrayList<>();

    //orphanRemoval 속성 추가 관계를 맺은 ReadStatus도 같이 삭제
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void active() {
        state(isActive == false, "채팅방은 활성화 상태에서 비활성화 상태로만 변경할 수 있습니다.");
        this.isActive = true;
    }

    public void deActive(){
        state(isActive == true, "채팅방은 비활성화 상태에서 활성화 상태로만 변경할 수 있습니다.");
        this.isActive = false;
    }



    /**
     * 멤버 퇴장 처리
     */
    public void removeMember(Long memberId) {
        findMember(memberId)
                .filter(ChatRoomMember::isActiveMember)
                .ifPresent(ChatRoomMember::leave);
    }

    /**
     * 멤버 조회
     */
    public Optional<ChatRoomMember> findMember(Long memberId) {
        return this.members.stream()
                .filter(member -> member.getMemberId().equals(memberId))
                .findFirst();
    }


    /**
     * 활성 멤버인지 확인
     */
    public boolean hasMember(Long memberId) {
        return findMember(memberId)
                .map(ChatRoomMember::isActiveMember)
                .orElse(false);
    }

    /**
     * 활성 멤버 수
     */
    public int getActiveMemberCount() {
        return (int) this.members.stream()
                .filter(ChatRoomMember::isActiveMember)
                .count();
    }

    // === 상태 변경 메서드 ===

    public void activate() {
        if (this.isActive) {
            throw new IllegalStateException("이미 활성화된 채팅방입니다.");
        }
        this.isActive = true;
    }

    public void deactivate() {
        if (!this.isActive) {
            throw new IllegalStateException("이미 비활성화된 채팅방입니다.");
        }
        this.isActive = false;
    }
}
