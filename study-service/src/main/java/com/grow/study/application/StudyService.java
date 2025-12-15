package com.grow.study.application;


import com.grow.study.application.provided.StudyRegister;
import com.grow.study.application.provided.dto.StudyRegisterResponse;
import com.grow.study.application.required.ChatRestClient;
import com.grow.study.application.required.S3FileUpload;
import com.grow.study.application.required.StudyEventPublisher;
import com.grow.study.application.required.StudyRepository;
import com.grow.common.StudyCreateEvent;
import com.grow.study.domain.study.*;
import com.grow.study.domain.study.dto.StudyRegisterRequest;
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

    private final S3FileUpload s3FileUpload;
    private final StudyRepository studyRepository;
    private final ChatRestClient chatRestClient;

    @Override
    public StudyRegisterResponse register(StudyRegisterRequest request, MultipartFile thumbnail, Long leaderId) {

            String thumbnailUrl = null;
            if (thumbnail != null && !thumbnail.isEmpty()) {
                thumbnailUrl = s3FileUpload.uploadImage(thumbnail, "images/study");
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
                    LocalDate.parse(request.getStartDate()).minusDays(1),
                    daysOfWeek
            );

            // 스터디 엔티티 생성
            Study study = Study.create(
                    request.getTitle(),
                    thumbnailUrl,
                    category,
                    level,
                    visibility,
                    leaderId,
                    schedule,
                    request.getMinMembers(),
                    request.getMaxMembers(),
                    request.getDeposit(),
                    request.getIntroduction(),
                    request.getCurriculum(),
                    request.getLeaderMessage(),
                    StudyStatus.PENDING
            );

            // 스터디장을 멤버로 추가
            study.addLeaderAsMember();

            // 스터디 세션 생성
            if (request.getTotalSessions() != null && request.getTotalSessions() > 0) {
                study.createSessions(request.getTotalSessions());
            }

            // 스터디 저장
            Study savedStudy = studyRepository.save(study);

            chatRestClient.createChatRoom(savedStudy.getId(), savedStudy.getTitle());

        return StudyRegisterResponse.from(savedStudy);
    }

    @Override
    public void enrollment(Long studyId, Long memberId, Integer depositAmount) {

        Study study = studyRepository.findStudiesById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("스터디를 찾을 수 없습니다."));

        // 1. 참여 가능 여부 검증
        if (!study.isJoinable()) {
            throw new NonRetryableException("참여할 수 없는 스터디입니다.");
        }

        // 2. 스터디장 본인 참여 방지
        if (study.isLeader(memberId)) {
            throw new NonRetryableException("스터디장은 이미 참여 중입니다.");
        }

        // 3. 보증금 일치 여부 확인
        if (!study.getDepositAmount().equals(depositAmount)) {
            throw new NonRetryableException("보증금이 일치하지 않습니다.");
        }

        // 4. 멤버 추가 (내부에서 중복 체크)
        study.addMember(memberId, depositAmount);


        studyRepository.save(study);

        //todo roomId??? 채팅멤버는 스케쥴로하는게
       // chatRestClient.createChatRoomMember()
    }

    public void changeStudyStatus(Long studyId, Long leaderId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("스터디를 찾을 수 없습니다."));

        study.isLeader(leaderId);

        study.openRecruitment();

        studyRepository.save(study);
    }

}
