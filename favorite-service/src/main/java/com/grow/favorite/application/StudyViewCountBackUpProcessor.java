package com.grow.favorite.application;

import com.grow.favorite.application.required.StudyViewCountBackupPort;
import com.grow.favorite.domain.view.StudyViewCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudyViewCountBackUpProcessor {

    private final StudyViewCountBackupPort studyViewCountBackupPort;

    public void viewBackup(Long studyId, Long viewCount){

        int result = studyViewCountBackupPort.updateViewCount(studyId, viewCount);
        if(result ==0){
            studyViewCountBackupPort.save(StudyViewCount.init(studyId,viewCount));
        }
    }
}
