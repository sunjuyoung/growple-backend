package com.grow.study.application;

import com.grow.study.adapter.persistence.PostJpaRepository;
import com.grow.study.adapter.persistence.PostCommentJpaRepository;
import com.grow.study.adapter.persistence.StudyJpaRepository;
import com.grow.study.application.dto.board.*;
import com.grow.study.domain.board.Post;
import com.grow.study.domain.board.PostCategory;
import com.grow.study.domain.study.Study;
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
public class PostService {

    private final PostJpaRepository postRepository;
    private final PostCommentJpaRepository commentRepository;
    private final StudyJpaRepository studyRepository;

    /**
     * 게시글 작성
     */
    @Transactional
    public PostResponse create(Long studyId, Long memberId, String memberNickname, PostCreateRequest request) {
        Study study = findStudyById(studyId);

        // 스터디 멤버 여부 확인
        validateStudyMember(study, memberId);

        Post post;
        if (request.getCategory() == PostCategory.NOTICE) {
            // 공지글은 스터디장만 작성 가능
            post = Post.createNotice(study, memberId, memberNickname, request.getTitle(), request.getContent());
        } else {
            post = Post.create(study, memberId, memberNickname, request.getCategory(), request.getTitle(), request.getContent());
        }

        Post savedPost = postRepository.save(post);

        return PostResponse.from(savedPost, memberId);
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public PostResponse getPost(Long postId, Long memberId) {
        Post post = findPostById(postId);
        // 조회수 증가
        // 레디스 or 필요없는 기능일듯
       // postRepository.incrementViewCount(postId);

        return PostResponse.from(post, memberId);
    }

    /**
     * 게시글 목록 조회 (스터디별)
     */
    public Page<PostListResponse> getPostsByStudy(Long studyId, Pageable pageable) {
        return postRepository.findByStudyId(studyId, pageable)
                .map(PostListResponse::from);
    }

    /**
     * 게시글 목록 조회 (스터디별 + 카테고리별)
     */
    public Page<PostListResponse> getPostsByStudyAndCategory(Long studyId, PostCategory category, Pageable pageable) {
        return postRepository.findByStudyIdAndCategory(studyId, category, pageable)
                .map(PostListResponse::from);
    }

    /**
     * 공지글 목록 조회
     */
    public List<PostListResponse> getNotices(Long studyId) {
        return postRepository.findNoticesByStudyId(studyId).stream()
                .map(PostListResponse::from)
                .toList();
    }

    /**
     * 고정글 목록 조회
     */
    public List<PostListResponse> getPinnedPosts(Long studyId) {
        return postRepository.findPinnedByStudyId(studyId).stream()
                .map(PostListResponse::from)
                .toList();
    }

    /**
     * 내가 작성한 게시글 목록 조회
     */
    public Page<PostListResponse> getMyPosts(Long memberId, Pageable pageable) {
        return postRepository.findByWriterId(memberId, pageable)
                .map(PostListResponse::from);
    }

    /**
     * 게시글 검색
     */
    public Page<PostListResponse> searchPosts(Long studyId, String keyword, Pageable pageable) {
        return postRepository.searchByKeyword(studyId, keyword, pageable)
                .map(PostListResponse::from);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostResponse update(Long postId, Long memberId, PostUpdateRequest request) {
        Post post = findPostById(postId);

        // 수정 권한 확인
        validateModifyPermission(post, memberId);

        post.update(request.getTitle(), request.getContent());
        log.info("게시글 수정 완료 - postId: {}, memberId: {}", postId, memberId);

        return PostResponse.from(post, memberId);
    }

    /**
     * 게시글 삭제 (soft delete)
     */
    @Transactional
    public void delete(Long postId, Long memberId) {
        Post post = findPostById(postId);

        // 삭제 권한 확인
        validateModifyPermission(post, memberId);

        // 연관 댓글 일괄 삭제
        commentRepository.softDeleteByPostId(postId);

        post.delete();
        log.info("게시글 삭제 완료 - postId: {}, memberId: {}", postId, memberId);
    }

    /**
     * 게시글 고정/고정해제 (스터디장만)
     */
    @Transactional
    public PostResponse togglePin(Long postId, Long memberId) {
        Post post = findPostById(postId);

        // 스터디장 여부 확인
        if (!post.getStudy().isLeader(memberId)) {
            throw new IllegalArgumentException("스터디장만 게시글을 고정할 수 있습니다.");
        }

        if (post.getPinned()) {
            post.unpin();
            log.info("게시글 고정 해제 - postId: {}", postId);
        } else {
            post.pin();
            log.info("게시글 고정 - postId: {}", postId);
        }

        return PostResponse.from(post, memberId);
    }

    /**
     * 스터디별 게시글 수 조회
     */
    public Long getPostCount(Long studyId) {
        return postRepository.countByStudyId(studyId);
    }

    // ==================== Private Methods ====================

    private Study findStudyById(Long studyId) {
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("스터디를 찾을 수 없습니다. studyId: " + studyId));
    }

    private Post findPostById(Long postId) {
        return postRepository.findByIdAndNotDeleted(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. postId: " + postId));
    }

    private void validateStudyMember(Study study, Long memberId) {
        if (!study.isMember(memberId)) {
            throw new IllegalArgumentException("스터디 멤버만 게시글을 작성할 수 있습니다.");
        }
    }

    private void validateModifyPermission(Post post, Long memberId) {
        if (!post.canModify(memberId)) {
            throw new IllegalArgumentException("게시글 수정/삭제 권한이 없습니다.");
        }
    }
}
