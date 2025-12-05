package com.grow.study.application.required;

import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudyRepository {

    Optional<StudyWithMemberCountDto> findWithMemberCount(@Param("studyId") Long studyId);
}
