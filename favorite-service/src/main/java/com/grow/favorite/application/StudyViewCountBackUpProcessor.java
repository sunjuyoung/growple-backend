package com.grow.favorite.application;

import com.grow.favorite.adapter.persistence.StudyViewCountJpaRepository;
import com.grow.favorite.domain.view.StudyViewCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudyViewCountBackUpProcessor {

    private final StudyViewCountJpaRepository studyViewCountJpaRepository;

    public void viewBackup(Long studyId, Long viewCount){

        int result = studyViewCountJpaRepository.updateViewCount(studyId, viewCount);
        if(result ==0){
            studyViewCountJpaRepository.save(StudyViewCount.init(studyId,viewCount));
        }
    }
}
