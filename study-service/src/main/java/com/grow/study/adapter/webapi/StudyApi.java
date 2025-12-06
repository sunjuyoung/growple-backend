package com.grow.study.adapter.webapi;

import com.grow.study.application.provided.StudyRegister;
import com.grow.study.application.provided.dto.StudyRegisterResponse;
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

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudyRegisterResponse> createStudy(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @ModelAttribute StudyRegisterRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
       StudyRegisterResponse response = studyRegister.register(request, thumbnail, userId);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


}
