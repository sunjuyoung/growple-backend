package com.grow.chat.repository;

import com.grow.chat.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId AND cm.id > :lastReadMessageId ORDER BY cm.createdAt ASC")
    List<ChatMessage> findUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("lastReadMessageId") Long lastReadMessageId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId AND cm.id > :lastReadMessageId")
    Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("lastReadMessageId") Long lastReadMessageId);
}
