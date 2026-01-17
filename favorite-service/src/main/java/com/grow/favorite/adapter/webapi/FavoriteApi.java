package com.grow.favorite.adapter.webapi;

import com.grow.favorite.application.provided.FavoriteService;
import com.grow.favorite.domain.favorite.FavoriteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteApi {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> read(@RequestHeader("X-User-Id") Long memberId) {
        List<FavoriteResponse> responses = favoriteService.read(memberId).stream()
                .map(FavoriteResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/studies/{studyId}")
    public ResponseEntity<FavoriteResponse> favorite(
            @RequestHeader("X-User-Id") Long memberId,
            @PathVariable Long studyId) {
        FavoriteResponse response = FavoriteResponse.from(favoriteService.favorite(memberId, studyId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/studies/{studyId}")
    public ResponseEntity<Void> unfavorite(
            @RequestHeader("X-User-Id") Long memberId,
            @PathVariable Long studyId) {
        favoriteService.unfavorite(memberId, studyId);
        return ResponseEntity.noContent().build();
    }
}
