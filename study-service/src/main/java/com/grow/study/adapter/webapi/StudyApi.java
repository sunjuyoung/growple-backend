package com.grow.study.adapter.webapi;

import com.grow.study.application.provided.StudyRegister;
import com.grow.study.application.provided.dto.StudyRegisterResponse;
import com.grow.study.domain.study.dto.StudyRegisterRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study")
@Tag(name = "Study", description = "스터디 관리 API")
public class StudyApi {

    private final StudyRegister studyRegister;


    @Operation(
            summary = "스터디 생성",
            description = "새로운 스터디를 생성합니다. 썸네일 이미지는 선택사항입니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "스터디 생성 성공",
                    content = @Content(schema = @Schema(implementation = StudyRegisterResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content
            )
    })
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyRegisterResponse> createStudy(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Valid @ModelAttribute StudyRegisterRequest request,
            @Parameter(description = "스터디 썸네일 이미지")
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
       StudyRegisterResponse response = studyRegister.register(request, thumbnail, userId);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


}
