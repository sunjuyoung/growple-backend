package com.grow.favorite.application.provided;

import com.grow.favorite.domain.favorite.Favorite;

import java.util.List;

public interface FavoriteService {

    List<Favorite> read(Long memberId);

    Favorite favorite(Long memberId, Long studyId);

    void unfavorite(Long memberId, Long studyId);
}
