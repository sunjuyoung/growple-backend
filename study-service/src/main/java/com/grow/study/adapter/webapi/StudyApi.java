package com.grow.study.adapter.webapi;

import com.grow.study.adapter.security.JwtTokenProvider;
import com.grow.study.application.provided.StudyRegister;
import com.grow.study.application.provided.StudyRegisterResponse;
import com.grow.study.domain.study.dto.StudyRegisterRequest;
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
public class StudyApi {

    private final StudyRegister studyRegister;
    private final JwtTokenProvider jwtTokenProvider;



    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyRegisterResponse> createStudy(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @ModelAttribute StudyRegisterRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        // JWT 토큰에서 사용자 ID 추출
        String token = jwtTokenProvider.extractToken(authorizationHeader);
        Long leaderId = jwtTokenProvider.getUserId(token);

        // 스터디 개설
       StudyRegisterResponse response = studyRegister.register(request, thumbnail, leaderId);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudyRegisterResponse> getStudy(@PathVariable Long id) {
        // Implementation for retrieving study details by ID would go here
        return ResponseEntity.ok().build();
    }
}
