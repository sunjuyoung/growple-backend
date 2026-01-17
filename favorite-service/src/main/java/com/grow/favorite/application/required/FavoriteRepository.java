package com.grow.favorite.application.required;

import com.grow.favorite.domain.favorite.Favorite;
import com.grow.favorite.domain.favorite.FavoriteCount;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository {

    Favorite save(Favorite favorite);

    Optional<Favorite> findByMemberIdAndStudyId(Long memberId, Long studyId);

    List<Favorite> findByMemberId(Long memberId);

    void delete(Favorite favorite);

    boolean existsByMemberIdAndStudyId(Long memberId, Long studyId);

    long countByStudyId(Long studyId);

    int increase(Long studyId);

    int decrease(Long studyId);

    void countSave(FavoriteCount favoriteCount);
}
