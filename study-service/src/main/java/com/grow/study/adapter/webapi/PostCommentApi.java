package com.grow.study.adapter.webapi;

import com.grow.study.application.PostCommentService;
import com.grow.study.application.dto.board.CommentCreateRequest;
import com.grow.study.application.dto.board.CommentResponse;
import com.grow.study.application.dto.board.CommentUpdateRequest;
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
@RequestMapping("/api/study/{studyId}/posts/{postId}/comments")
@Tag(name = "Comment", description = "게시글 댓글 API")
public class PostCommentApi {

    private final PostCommentService commentService;

    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "댓글 작성 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "스터디 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        CommentResponse response = commentService.create(postId, userId,studyId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId
    ) {
        List<CommentResponse> response = commentService.getCommentsByPost(postId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 목록 조회 (페이징)", description = "게시글의 댓글 목록을 페이징하여 조회합니다.")
    @GetMapping("/paged")
    public ResponseEntity<Page<CommentResponse>> getCommentsPaged(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<CommentResponse> response = commentService.getCommentsByPost(postId, userId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다. 작성자만 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID", required = true)
            @PathVariable Long commentId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        CommentResponse response = commentService.update(commentId, userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다. 작성자 또는 스터디장만 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "스터디 ID", required = true)
            @PathVariable Long studyId,
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID", required = true)
            @PathVariable Long commentId,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId
    ) {
        commentService.delete(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
