package com.grow.hotstudy.application.required;

import com.grow.hotstudy.domain.hotstudy.HotStudy;

import java.util.List;
import java.util.Optional;

public interface HotStudyRepository {

    HotStudy save(HotStudy hotStudy);

    Optional<HotStudy> findByStudyId(Long studyId);

    List<HotStudy> findTopByOrderByScoreDesc(int limit);

    List<HotStudy> findAllByOrderByRankingAsc();

    void deleteAll();
}
