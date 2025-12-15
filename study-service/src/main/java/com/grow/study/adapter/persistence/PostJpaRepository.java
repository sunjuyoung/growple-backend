package com.grow.study.adapter.persistence;

import com.grow.study.domain.board.Post;
import com.grow.study.domain.board.PostCategory;
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
public interface PostJpaRepository extends JpaRepository<Post, Long> {

    /**
     * 게시글 단건 조회 (삭제되지 않은 것만)
     */
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.study
        WHERE p.id = :postId AND p.deleted = false
    """)
    Optional<Post> findByIdAndNotDeleted(@Param("postId") Long postId);

    /**
     * 스터디별 게시글 목록 조회 (페이징, 고정글 우선)
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.study.id = :studyId AND p.deleted = false
        ORDER BY p.pinned DESC, p.createdAt DESC
    """)
    Page<Post> findByStudyId(@Param("studyId") Long studyId, Pageable pageable);

    /**
     * 스터디별 카테고리 필터 게시글 목록 조회
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.study.id = :studyId 
          AND p.category = :category 
          AND p.deleted = false
        ORDER BY p.pinned DESC, p.createdAt DESC
    """)
    Page<Post> findByStudyIdAndCategory(
            @Param("studyId") Long studyId,
            @Param("category") PostCategory category,
            Pageable pageable
    );

    /**
     * 스터디별 공지글 목록 조회
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.study.id = :studyId 
          AND p.category = 'NOTICE' 
          AND p.deleted = false
        ORDER BY p.pinned DESC, p.createdAt DESC
    """)
    List<Post> findNoticesByStudyId(@Param("studyId") Long studyId);

    /**
     * 스터디별 고정글 목록 조회
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.study.id = :studyId 
          AND p.pinned = true 
          AND p.deleted = false
        ORDER BY p.createdAt DESC
    """)
    List<Post> findPinnedByStudyId(@Param("studyId") Long studyId);

    /**
     * 회원이 작성한 게시글 목록 조회
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.writerId = :memberId AND p.deleted = false
        ORDER BY p.createdAt DESC
    """)
    Page<Post> findByWriterId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * 스터디별 게시글 수 조회
     */
    @Query("""
        SELECT COUNT(p) FROM Post p
        WHERE p.study.id = :studyId AND p.deleted = false
    """)
    Long countByStudyId(@Param("studyId") Long studyId);

    /**
     * 스터디별 카테고리별 게시글 수 조회
     */
    @Query("""
        SELECT COUNT(p) FROM Post p
        WHERE p.study.id = :studyId 
          AND p.category = :category 
          AND p.deleted = false
    """)
    Long countByStudyIdAndCategory(
            @Param("studyId") Long studyId,
            @Param("category") PostCategory category
    );

    /**
     * 조회수 증가
     */
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    /**
     * 키워드 검색 (제목 + 내용)
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.study.id = :studyId 
          AND p.deleted = false
          AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)
        ORDER BY p.createdAt DESC
    """)
    Page<Post> searchByKeyword(
            @Param("studyId") Long studyId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
