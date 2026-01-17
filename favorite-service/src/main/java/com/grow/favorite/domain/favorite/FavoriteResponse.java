package com.grow.favorite.domain.favorite;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FavoriteResponse {

    private UUID id;
    private Long memberId;
    private Long studyId;
    private LocalDateTime createdAt;

    public static FavoriteResponse from(Favorite favorite) {
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .memberId(favorite.getMemberId())
                .studyId(favorite.getStudyId())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
