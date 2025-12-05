package com.grow.study.adapter.persistence;

import com.grow.study.application.required.StudyRepository;
import com.grow.study.application.required.StudyWithMemberCountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StudyRepositoryAdapter implements StudyRepository {

    private final StudyJpaRepository studyJpaRepository;

    @Override
    public Optional<StudyWithMemberCountDto> findWithMemberCount(Long studyId) {
        return studyJpaRepository.findWithMemberCount(studyId);
    }
}
