package com.grow.favorite.adapter.persistence;

import com.grow.favorite.application.required.FavoriteRepository;
import com.grow.favorite.domain.favorite.Favorite;
import com.grow.favorite.domain.favorite.FavoriteCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FavoriteRepositoryAdapter implements FavoriteRepository {

    private final FavoriteJpaRepository favoriteJpaRepository;
    private final FavoriteCountJpaRepository favoriteCountJpaRepository;

    @Override
    public Favorite save(Favorite favorite) {
        return favoriteJpaRepository.save(favorite);
    }

    @Override
    public Optional<Favorite> findByMemberIdAndStudyId(Long memberId, Long studyId) {
        return favoriteJpaRepository.findByMemberIdAndStudyId(memberId, studyId);
    }

    @Override
    public List<Favorite> findByMemberId(Long memberId) {
        return favoriteJpaRepository.findByMemberId(memberId);
    }

    @Override
    public void delete(Favorite favorite) {
        favoriteJpaRepository.delete(favorite);
    }

    @Override
    public boolean existsByMemberIdAndStudyId(Long memberId, Long studyId) {
        return favoriteJpaRepository.existsByMemberIdAndStudyId(memberId, studyId);
    }

    @Override
    public long countByStudyId(Long studyId) {
        return favoriteJpaRepository.countByStudyId(studyId);
    }

    @Override
    public int increase(Long studyId) {
        return favoriteCountJpaRepository.increase(studyId);
    }

    @Override
    public int decrease(Long studyId) {
        return favoriteCountJpaRepository.decrease(studyId);
    }

    @Override
    public void countSave(FavoriteCount favoriteCount) {
        favoriteCountJpaRepository.save(favoriteCount);
    }


}
