package com.grow.favorite.application;


import com.grow.favorite.adapter.persistence.StudyViewCountRepository;
import com.grow.favorite.adapter.persistence.StudyViewRedisLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyViewCountService {

    private final StudyViewCountRepository studyViewCountRepository;
    private final StudyViewCountBackUpProcessor studyViewCountBackUpProcessor;
    private final StudyViewRedisLockRepository studyViewRedisLockRepository;

    private static final int BACK_UP_BATCH_SIZE = 10;

    @Transactional
    public Long increase(Long studyId, Long userId){

        if(!studyViewRedisLockRepository.viewLock(studyId,userId)){
            return studyViewCountRepository.read(studyId);
        }


        Long count = studyViewCountRepository.increase(studyId);
        if(count % BACK_UP_BATCH_SIZE == 0){
            studyViewCountBackUpProcessor.viewBackup(studyId, count);
        }

        return count;
    }

    public Long count(Long articleId) {
        return studyViewCountRepository.read(articleId);
    }
}
