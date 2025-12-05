package com.grow.study.application;

import com.grow.study.application.required.StudyRepository;
import com.grow.study.application.required.StudyWithMemberCountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Service
@RequiredArgsConstructor
public class StudyQueryService {

    private final StudyRepository studyRepository;

    public StudyWithMemberCountDto findWithMemberCount(@Param("studyId") Long studyId) {
        return studyRepository.findWithMemberCount(studyId);
    }
}
