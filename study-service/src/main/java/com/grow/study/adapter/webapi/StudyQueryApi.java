package com.grow.study.adapter.webapi;

import com.grow.study.application.provided.StudyFinder;
import com.grow.study.application.required.dto.StudyWithMemberCountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study")
public class StudyQueryApi {

    private final StudyFinder studyFinder;

    @GetMapping("/{id}")
    public ResponseEntity<StudyWithMemberCountResponse> getStudyEnrollmentDetail(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        StudyWithMemberCountResponse study = studyFinder.getStudyEnrollmentDetail(id,userId);

        return ResponseEntity.ok(study);
    }
}
