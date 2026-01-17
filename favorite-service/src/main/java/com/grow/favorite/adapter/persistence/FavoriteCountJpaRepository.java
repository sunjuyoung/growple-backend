package com.grow.favorite.adapter.persistence;

import com.grow.favorite.domain.favorite.FavoriteCount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FavoriteCountJpaRepository extends JpaRepository<FavoriteCount,Long> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FavoriteCount> findLockByStudyId(Long studyId);

    @Query(
            value = "update favorite_count set count = count +1 where study_id =:studyId",
            nativeQuery = true
    )
    @Modifying
    int increase(@Param("studyId") Long studyId);

    @Query(
            value = "update favorite_count set count = count -1 where study_id =:studyId",
            nativeQuery = true
    )
    @Modifying
    int decrease(@Param("studyId") Long studyId);
}
