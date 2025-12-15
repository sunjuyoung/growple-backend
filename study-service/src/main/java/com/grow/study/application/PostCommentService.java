package com.grow.study.application;

import com.grow.study.adapter.persistence.PostCommentJpaRepository;
import com.grow.study.adapter.persistence.PostJpaRepository;
import com.grow.study.application.dto.board.CommentCreateRequest;
import com.grow.study.application.dto.board.CommentResponse;
import com.grow.study.application.dto.board.CommentUpdateRequest;
import com.grow.study.domain.board.Post;
import com.grow.study.domain.board.PostComment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentService {

    private final PostCommentJpaRepository commentRepository;
    private final PostJpaRepository postRepository;

    /**
     * 댓글 작성
     */
    @Transactional
    public CommentResponse create(Long postId, Long memberId, String memberNickname, CommentCreateRequest request) {
        Post post = findPostById(postId);

        // 스터디 멤버 여부 확인
        validateStudyMember(post, memberId);

        PostComment comment = PostComment.create(post, memberId, memberNickname, request.getContent());
        PostComment savedComment = commentRepository.save(comment);

        log.info("댓글 작성 완료 - postId: {}, commentId: {}, memberId: {}", postId, savedComment.getId(), memberId);

        return CommentResponse.from(savedComment, memberId);
    }

    /**
     * 게시글별 댓글 목록 조회
     */
    public List<CommentResponse> getCommentsByPost(Long postId, Long memberId) {
        return commentRepository.findByPostId(postId).stream()
                .map(comment -> CommentResponse.from(comment, memberId))
                .toList();
    }

    /**
     * 게시글별 댓글 목록 조회 (페이징)
     */
    public Page<CommentResponse> getCommentsByPost(Long postId, Long memberId, Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable)
                .map(comment -> CommentResponse.from(comment, memberId));
    }

    /**
     * 내가 작성한 댓글 목록 조회
     */
    public Page<CommentResponse> getMyComments(Long memberId, Pageable pageable) {
        return commentRepository.findByWriterId(memberId, pageable)
                .map(comment -> CommentResponse.from(comment, memberId));
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponse update(Long commentId, Long memberId, CommentUpdateRequest request) {
        PostComment comment = findCommentById(commentId);

        // 수정 권한 확인 (작성자만 수정 가능)
        if (!comment.isWriter(memberId)) {
            throw new IllegalArgumentException("댓글 작성자만 수정할 수 있습니다.");
        }

        comment.update(request.getContent());
        log.info("댓글 수정 완료 - commentId: {}, memberId: {}", commentId, memberId);

        return CommentResponse.from(comment, memberId);
    }

    /**
     * 댓글 삭제 (soft delete)
     */
    @Transactional
    public void delete(Long commentId, Long memberId) {
        PostComment comment = findCommentById(commentId);

        // 삭제 권한 확인 (작성자 또는 스터디장)
        if (!comment.canModify(memberId)) {
            throw new IllegalArgumentException("댓글 삭제 권한이 없습니다.");
        }

        comment.delete();
        log.info("댓글 삭제 완료 - commentId: {}, memberId: {}", commentId, memberId);
    }

    /**
     * 게시글별 댓글 수 조회
     */
    public Long getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    /**
     * 스터디별 회원의 댓글 수 조회 (활동 통계)
     */
    public Long getMemberCommentCount(Long studyId, Long memberId) {
        return commentRepository.countByStudyIdAndWriterId(studyId, memberId);
    }

    // ==================== Private Methods ====================

    private Post findPostById(Long postId) {
        return postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. postId: " + postId));
    }

    private PostComment findCommentById(Long commentId) {
        return commentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. commentId: " + commentId));
    }

    private void validateStudyMember(Post post, Long memberId) {
        if (!post.getStudy().isMember(memberId)) {
            throw new IllegalArgumentException("스터디 멤버만 댓글을 작성할 수 있습니다.");
        }
    }
}
