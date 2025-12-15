package com.grow.study.adapter.persistence;

import com.grow.study.domain.board.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostCommentJpaRepository extends JpaRepository<PostComment, Long> {

    /**
     * 댓글 단건 조회 (삭제되지 않은 것만)
     */
    @Query("""
        SELECT c FROM PostComment c
        JOIN FETCH c.post p
        JOIN FETCH p.study
        WHERE c.id = :commentId AND c.deleted = false
    """)
    Optional<PostComment> findByIdAndNotDeleted(@Param("commentId") Long commentId);

    /**
     * 게시글별 댓글 목록 조회 (삭제되지 않은 것만)
     */
    @Query("""
        SELECT c FROM PostComment c
        WHERE c.post.id = :postId AND c.deleted = false
        ORDER BY c.createdAt ASC
    """)
    List<PostComment> findByPostId(@Param("postId") Long postId);

    /**
     * 게시글별 댓글 목록 조회 (페이징)
     */
    @Query("""
        SELECT c FROM PostComment c
        WHERE c.post.id = :postId AND c.deleted = false
        ORDER BY c.createdAt ASC
    """)
    Page<PostComment> findByPostId(@Param("postId") Long postId, Pageable pageable);

    /**
     * 회원이 작성한 댓글 목록 조회
     */
    @Query("""
        SELECT c FROM PostComment c
        JOIN FETCH c.post p
        WHERE c.writerId = :memberId AND c.deleted = false
        ORDER BY c.createdAt DESC
    """)
    Page<PostComment> findByWriterId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 게시글별 댓글 수 조회
     */
    @Query("""
        SELECT COUNT(c) FROM PostComment c
        WHERE c.post.id = :postId AND c.deleted = false
    """)
    Long countByPostId(@Param("postId") Long postId);

    /**
     * 스터디별 회원의 댓글 수 조회 (활동 통계용)
     */
    @Query("""
        SELECT COUNT(c) FROM PostComment c
        WHERE c.post.study.id = :studyId 
          AND c.writerId = :memberId 
          AND c.deleted = false
    """)
    Long countByStudyIdAndWriterId(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId
    );

    /**
     * 게시글 삭제 시 연관 댓글 일괄 삭제 (soft delete)
     */
    @Modifying
    @Query("""
        UPDATE PostComment c 
        SET c.deleted = true 
        WHERE c.post.id = :postId
    """)
    void softDeleteByPostId(@Param("postId") Long postId);
}
