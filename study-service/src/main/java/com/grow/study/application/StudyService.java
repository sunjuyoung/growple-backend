package com.grow.study.application;

import com.grow.study.adapter.persistence.StudyJpaRepository;
import com.grow.study.application.provided.StudyRegister;
import com.grow.study.application.provided.dto.StudyRegisterResponse;
import com.grow.study.domain.study.*;
import com.grow.study.domain.study.dto.StudyRegisterRequest;
import com.grow.study.adapter.intergration.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudyService implements StudyRegister {

    private final StudyJpaRepository studyJpaRepository;
    private final S3Service s3Service;

    @Override
    public StudyRegisterResponse register(StudyRegisterRequest request, MultipartFile thumbnail, Long leaderId) {

            String thumbnailUrl = null;
            if (thumbnail != null && !thumbnail.isEmpty()) {
                thumbnailUrl = s3Service.uploadImage(thumbnail, "images/study");
            }

            // 카테고리와 레벨 파싱
            StudyCategory category = StudyCategory.fromDisplayName(request.getCategory());
            StudyLevel level = StudyLevel.fromDisplayName(request.getLevel());

            // 공개 여부
            StudyVisibility visibility = request.getIsPublic()
                    ? StudyVisibility.PUBLIC
                    : StudyVisibility.PRIVATE;

            // 요일 변환
            Set<DayOfWeek> daysOfWeek = request.getDaysOfWeek().stream()
                    .map(shortName -> DayOfWeek.fromShortName(shortName))
                    .collect(Collectors.toSet());

            // 일정 생성
            StudySchedule schedule = new StudySchedule(
                    LocalDate.parse(request.getStartDate()),
                    LocalDate.parse(request.getEndDate()),
                    LocalTime.parse(request.getStartTime()),
                    LocalTime.parse(request.getEndTime()),
                    daysOfWeek
            );

            // 스터디 엔티티 생성
            Study study = Study.builder()
                    .title(request.getTitle())
                    .thumbnailUrl(thumbnailUrl)
                    .category(category)
                    .level(level)
                    .visibility(visibility)
                    .leaderId(leaderId)
                    .schedule(schedule)
                    .minParticipants(request.getMinMembers())
                    .maxParticipants(request.getMaxMembers())
                    .depositAmount(request.getDeposit())
                    .introduction(request.getIntroduction())
                    .curriculum(request.getCurriculum())
                    .leaderMessage(request.getLeaderMessage())
                    .build();

            // 스터디장을 멤버로 추가
            study.addLeaderAsMember();

            // 스터디 세션 생성
            if (request.getTotalSessions() != null && request.getTotalSessions() > 0) {
                study.createSessions(request.getTotalSessions());
            }

            // 스터디 저장
            Study savedStudy = studyJpaRepository.save(study);


        return StudyRegisterResponse.from(savedStudy);
    }
}
