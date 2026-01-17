package com.grow.favorite.adapter.webapi;

import com.grow.favorite.application.StudyViewCountService;
import com.grow.favorite.domain.view.StudyViewCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/studies")
@RequiredArgsConstructor
public class StudyViewCountApi {

    private final StudyViewCountService studyViewCountService;

    @PostMapping("/{studyId}/views")
    public ResponseEntity<StudyViewCountResponse> increase(
            @PathVariable Long studyId,
            @RequestHeader("X-User-Id") Long userId) {
        Long count = studyViewCountService.increase(studyId, userId);
        return ResponseEntity.ok(StudyViewCountResponse.of(studyId, count));
    }

    @GetMapping("/{studyId}/views")
    public ResponseEntity<StudyViewCountResponse> count(@PathVariable Long studyId) {
        Long count = studyViewCountService.count(studyId);
        return ResponseEntity.ok(StudyViewCountResponse.of(studyId, count));
    }
}
