package com.grow.favorite.adapter.persistence;

import com.grow.favorite.domain.view.StudyViewCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyViewCountJpaRepository extends JpaRepository<StudyViewCount,Long> {

    @Query(
            value = " update study_view_count set count =:viewCount " +
                    " where studyId =:studyId and count <:viewCount",
            nativeQuery = true
    )
    int updateViewCount(@Param("studyId")Long studyId, @Param("viewCount") Long viewCount);


}
