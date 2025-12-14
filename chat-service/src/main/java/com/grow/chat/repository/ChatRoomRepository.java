package com.grow.chat.repository;

import com.grow.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByStudyId(Long studyId);

    Optional<ChatRoom> findByIdAndStudyId(@Param("id") Long id,  @Param("studyId") Long studyId);

    boolean existsByStudyId(Long studyId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.studyId = :studyId AND cr.isActive = true")
    Optional<ChatRoom> findActiveRoomByStudyId(@Param("studyId") Long studyId);
}
