package com.grow.hotstudy.adapter.persistence;

import com.grow.hotstudy.application.required.HotStudyRepository;
import com.grow.hotstudy.domain.hotstudy.HotStudy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HotStudyRepositoryAdapter implements HotStudyRepository {

    private final HotStudyJpaRepository hotStudyJpaRepository;

    @Override
    public HotStudy save(HotStudy hotStudy) {
        return hotStudyJpaRepository.save(hotStudy);
    }

    @Override
    public Optional<HotStudy> findByStudyId(Long studyId) {
        return hotStudyJpaRepository.findByStudyId(studyId);
    }

    @Override
    public List<HotStudy> findTopByOrderByScoreDesc(int limit) {
        return hotStudyJpaRepository.findAll(PageRequest.of(0, limit,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "score")))
                .getContent();
    }

    @Override
    public List<HotStudy> findAllByOrderByRankingAsc() {
        return hotStudyJpaRepository.findAllByOrderByRankingAsc();
    }

    @Override
    public void deleteAll() {
        hotStudyJpaRepository.deleteAll();
    }
}
