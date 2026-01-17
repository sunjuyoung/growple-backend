package com.grow.favorite.adapter.persistence;

import com.grow.favorite.domain.favorite.Favorite;
import com.grow.favorite.domain.favorite.FavoriteCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteJpaRepository extends JpaRepository<Favorite, UUID> {

    Optional<Favorite> findByMemberIdAndStudyId(Long memberId, Long studyId);

    List<Favorite> findByMemberId(Long memberId);

    boolean existsByMemberIdAndStudyId(Long memberId, Long studyId);

    long countByStudyId(Long studyId);



}
