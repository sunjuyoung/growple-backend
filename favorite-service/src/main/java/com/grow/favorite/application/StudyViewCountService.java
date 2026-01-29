package com.grow.favorite.application;


import com.grow.favorite.application.required.StudyViewCountPort;
import com.grow.favorite.application.required.StudyViewLockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyViewCountService {

    private final StudyViewCountPort studyViewCountPort;
    private final StudyViewCountBackUpProcessor studyViewCountBackUpProcessor;
    private final StudyViewLockPort studyViewLockPort;

    private static final int BACK_UP_BATCH_SIZE = 10;

    @Transactional
    public Long increase(Long studyId, Long userId){

        if(!studyViewLockPort.viewLock(studyId,userId)){
            return studyViewCountPort.read(studyId);
        }


        Long count = studyViewCountPort.increase(studyId);
        if(count % BACK_UP_BATCH_SIZE == 0){
            studyViewCountBackUpProcessor.viewBackup(studyId, count);
        }

        return count;
    }

    public Long count(Long articleId) {
        return studyViewCountPort.read(articleId);
    }
}
