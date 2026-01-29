package com.grow.favorite.application.required;

public interface StudyViewCountPort {

    Long read(Long studyId);

    Long increase(Long studyId);
}
