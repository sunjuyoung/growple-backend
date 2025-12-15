package com.grow.study.adapter.webapi;

import com.grow.study.application.PostCommentService;
import com.grow.study.application.PostService;
import com.grow.study.application.dto.board.CommentResponse;
import com.grow.study.application.dto.board.PostListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/posts")
@Tag(name = "MyPost", description = "내 게시글/댓글 API")
public class MyPostApi {

    private final PostService postService;
    private final PostCommentService commentService;

    @Operation(summary = "내가 작성한 게시글 목록", description = "현재 로그인한 사용자가 작성한 게시글 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<Page<PostListResponse>> getMyPosts(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostListResponse> response = postService.getMyPosts(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내가 작성한 댓글 목록", description = "현재 로그인한 사용자가 작성한 댓글 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/comments")
    public ResponseEntity<Page<CommentResponse>> getMyComments(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CommentResponse> response = commentService.getMyComments(userId, pageable);
        return ResponseEntity.ok(response);
    }
}
