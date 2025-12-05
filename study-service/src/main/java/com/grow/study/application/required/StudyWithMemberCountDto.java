package com.grow.study.application.required;

import com.grow.study.domain.study.Study;

public record StudyWithMemberCountDto(
        Study study,
        Long memberCount
) {}