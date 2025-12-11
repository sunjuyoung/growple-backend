package com.grow.chat.repository;

import com.grow.chat.domain.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    @Query("SELECT crm FROM ChatRoomMember crm WHERE crm.chatRoom.id = :chatRoomId AND crm.memberId = :memberId AND crm.leftAt IS NULL")
    Optional<ChatRoomMember> findActiveMember(@Param("chatRoomId") Long chatRoomId, @Param("memberId") Long memberId);

    @Query("SELECT crm FROM ChatRoomMember crm WHERE crm.chatRoom.id = :chatRoomId AND crm.leftAt IS NULL")
    List<ChatRoomMember> findActiveMembers(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT crm FROM ChatRoomMember crm WHERE crm.memberId = :memberId AND crm.leftAt IS NULL")
    List<ChatRoomMember> findActiveMembersByMemberId(@Param("memberId") Long memberId);

    boolean existsByChatRoomIdAndMemberIdAndLeftAtIsNull(Long chatRoomId, Long memberId);
}
