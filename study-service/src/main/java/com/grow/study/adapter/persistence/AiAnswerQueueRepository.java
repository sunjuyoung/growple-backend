package com.grow.study.adapter.persistence;

import com.grow.study.domain.llm.AiAnswerQueue;
import com.grow.study.domain.llm.AiQueueStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiAnswerQueueRepository extends JpaRepository<AiAnswerQueue, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT q FROM AiAnswerQueue q
            JOIN FETCH q.post
            WHERE q.status = 'PENDING'
            ORDER BY q.createdAt ASC
            LIMIT :limit
            """)
    List<AiAnswerQueue> findPendingWithLock(@Param("limit") int limit);

    @Query("""
            SELECT CASE WHEN COUNT(q) > 0 THEN true ELSE false END
            FROM AiAnswerQueue q
            WHERE q.post.id = :postId
            """)
    boolean existsByPostId(@Param("postId") Long postId);

    Optional<AiAnswerQueue> findByPostId(Long postId);

    // 통계
    long countByStatus(AiQueueStatus status);

    @Query("""
            SELECT q.status, COUNT(q) 
            FROM AiAnswerQueue q 
            GROUP BY q.status
            """)
    List<Object[]> countGroupByStatus();
}