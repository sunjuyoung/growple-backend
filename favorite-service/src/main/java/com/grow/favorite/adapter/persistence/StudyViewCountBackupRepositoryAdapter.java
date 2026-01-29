package com.grow.favorite.adapter.persistence;

import com.grow.favorite.application.required.StudyViewCountBackupPort;
import com.grow.favorite.domain.view.StudyViewCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyViewCountBackupRepositoryAdapter implements StudyViewCountBackupPort {

    private final StudyViewCountJpaRepository studyViewCountJpaRepository;

    @Override
    public int updateViewCount(Long studyId, Long viewCount) {
        return studyViewCountJpaRepository.updateViewCount(studyId, viewCount);
    }

    @Override
    public StudyViewCount save(StudyViewCount studyViewCount) {
        return studyViewCountJpaRepository.save(studyViewCount);
    }
}
