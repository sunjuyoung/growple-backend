package com.grow.favorite.application.required;

import com.grow.favorite.domain.view.StudyViewCount;

public interface StudyViewCountBackupPort {

    int updateViewCount(Long studyId, Long viewCount);

    StudyViewCount save(StudyViewCount studyViewCount);
}
