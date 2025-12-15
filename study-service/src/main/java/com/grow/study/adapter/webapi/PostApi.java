package com.grow.study.adapter.webapi;

import com.grow.study.application.PostService;
import com.grow.study.application.dto.board.*;
import com.grow.study.domain.board.PostCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies/{studyId}/posts")
@Tag(name = "Post", description = "스터디 게시판 API")
public class PostApi {

    private final PostService postService;

    @Operation(summary = "게시글 작성", description = "스터디 게시판에 새 글을 작성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "게시글 작성 성공",
                    content = @Content(schema = @Schema(implementation = PostResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "스터디 멤버가 아님")
    })
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "사용자 닉네임", required = true)
            @RequestHeader("X-User-Nickname") String userNickname,
            @Valid @RequestBody PostCreateRequest request
    ) {
        PostResponse response = postService.create(studyId, userId, userNickname, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 내용을 조회합니다. 조회수가 증가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PostResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId
    ) {
        PostResponse response = postService.getPost(postId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 목록 조회", description = "스터디의 게시글 목록을 조회합니다. 고정글이 상단에 표시됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<Page<PostListResponse>> getPosts(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "카테고리 필터 (선택)")
            @RequestParam(required = false) PostCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostListResponse> response;
        if (category != null) {
            response = postService.getPostsByStudyAndCategory(studyId, category, pageable);
        } else {
            response = postService.getPostsByStudy(studyId, pageable);
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "공지글 목록 조회", description = "스터디의 공지글 목록을 조회합니다.")
    @GetMapping("/notices")
    public ResponseEntity<List<PostListResponse>> getNotices(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId
    ) {
        List<PostListResponse> response = postService.getNotices(studyId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "고정글 목록 조회", description = "스터디의 고정글 목록을 조회합니다.")
    @GetMapping("/pinned")
    public ResponseEntity<List<PostListResponse>> getPinnedPosts(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId
    ) {
        List<PostListResponse> response = postService.getPinnedPosts(studyId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 검색", description = "제목과 내용에서 키워드를 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<Page<PostListResponse>> searchPosts(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostListResponse> response = postService.searchPosts(studyId, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다. 작성자 또는 스터디장만 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        PostResponse response = postService.update(postId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다. 작성자 또는 스터디장만 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId
    ) {
        postService.delete(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 고정/해제", description = "게시글을 고정하거나 해제합니다. 스터디장만 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "고정/해제 성공"),
            @ApiResponse(responseCode = "403", description = "스터디장이 아님"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @PatchMapping("/{postId}/pin")
    public ResponseEntity<PostResponse> togglePin(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId
    ) {
        PostResponse response = postService.togglePin(postId, userId);
        return ResponseEntity.ok(response);
    }
}
