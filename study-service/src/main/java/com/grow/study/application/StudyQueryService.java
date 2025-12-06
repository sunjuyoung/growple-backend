package com.grow.study.application;

import com.grow.study.application.provided.StudyFinder;
import com.grow.study.application.required.*;
import com.grow.study.application.required.dto.MemberSummaryResponse;
import com.grow.study.application.required.dto.StudyWithMemberCountDto;
import com.grow.study.application.required.dto.StudyWithMemberCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class StudyQueryService implements StudyFinder {

    private final StudyRepository studyRepository;
    private final MemberRestClient memberRestClient;

    @Override
    public StudyWithMemberCountResponse getStudyEnrollmentDetail(Long studyId, Long userId) {
        StudyWithMemberCountDto studyWithMemberCountDto = studyRepository.findWithMemberCount(studyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스터디가 존재 하지 않습니다. " + studyId));

        //member service 연동 필요
        MemberSummaryResponse memberSummary = memberRestClient.getMemberSummary(userId);



        return StudyWithMemberCountResponse.of(studyWithMemberCountDto, memberSummary);
    }
}
