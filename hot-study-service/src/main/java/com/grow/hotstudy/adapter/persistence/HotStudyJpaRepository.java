package com.grow.hotstudy.adapter.persistence;

import com.grow.hotstudy.domain.hotstudy.HotStudy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HotStudyJpaRepository extends JpaRepository<HotStudy, Long> {

    Optional<HotStudy> findByStudyId(Long studyId);

    List<HotStudy> findTopNByOrderByScoreDesc(int limit);

    List<HotStudy> findAllByOrderByRankingAsc();
}
