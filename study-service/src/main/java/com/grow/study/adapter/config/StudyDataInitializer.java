package com.grow.study.adapter.config;

import com.grow.study.adapter.persistence.StudyJpaRepository;
import com.grow.study.application.StudyVectorService;
import com.grow.study.domain.event.StudyCreatedEvent;
import com.grow.study.domain.study.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
//@Profile("dev")
@RequiredArgsConstructor
public class StudyDataInitializer implements ApplicationRunner {

    private final StudyJpaRepository studyRepository;
    private final StudyVectorService studyVectorService;

    private static final String THUMBNAIL_URL = "https://sun-product-growplebucket.s3.amazonaws.com/images/study/e3533ad2-38ca-4498-b1e4-3d23685ce89b.jpg";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (studyRepository.count() > 2) {
            log.info("스터디 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("스터디 더미 데이터 생성 시작...");

        List<StudyData> studyDataList = createStudyDataList();
        List<Study> savedStudies = new ArrayList<>();

        for (StudyData data : studyDataList) {
            Study study = createStudy(data);
            Study savedStudy = studyRepository.save(study);
            savedStudies.add(savedStudy);
            log.info("스터디 생성 완료: {}", study.getTitle());
        }

        log.info("스터디 더미 데이터 생성 완료. 총 {}개", studyDataList.size());

        // 벡터 임베딩 생성
        createVectorEmbeddings(savedStudies);
    }

    private void createVectorEmbeddings(List<Study> studies) {
        log.info("벡터 임베딩 생성 시작...");

        int successCount = 0;
        int failCount = 0;

        for (Study study : studies) {
            try {
                StudyCreatedEvent event = new StudyCreatedEvent(
                        study.getId(),
                        study.getTitle(),
                        study.getIntroduction(),
                        study.getCurriculum(),
                        study.getLeaderMessage(),
                        study.getCategory(),
                        study.getLevel(),
                        study.getStatus(),
                        study.getMinParticipants(),
                        study.getMaxParticipants(),
                        study.getSchedule().getStartDate(),
                        study.getSchedule().getEndDate()
                );

                studyVectorService.createStudyDocument(study.getId(), event);
                successCount++;
                log.debug("벡터 임베딩 생성 완료: {} (ID: {})", study.getTitle(), study.getId());
            } catch (Exception e) {
                failCount++;
                log.error("벡터 임베딩 생성 실패: {} (ID: {})", study.getTitle(), study.getId(), e);
            }
        }

        log.info("벡터 임베딩 생성 완료. 성공: {}, 실패: {}", successCount, failCount);
    }

    private List<StudyData> createStudyDataList() {
        LocalDate today = LocalDate.now();

        return List.of(
                // ===== 개발 카테고리 =====
                new StudyData(
                        "Java 스프링부트 마스터",
                        StudyCategory.DEVELOPMENT,
                        StudyLevel.INTERMEDIATE,
                        "스프링부트 3.x 버전을 활용한 백엔드 개발 스터디입니다. JPA, Security, Cloud까지 다룹니다.",
                        "1주차: Spring Boot 기초\n2주차: Spring Data JPA\n3주차: Spring Security\n4주차: Spring Cloud",
                        "함께 성장하는 스터디가 되었으면 좋겠습니다!",
                        20000, 3, 8,
                        today.plusDays(7), today.plusDays(35), today.plusDays(5),
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                        LocalTime.of(20, 0), LocalTime.of(22, 0),
                        1L, "스터디장1", 8
                ),
                new StudyData(
                        "React + TypeScript 프론트엔드",
                        StudyCategory.DEVELOPMENT,
                        StudyLevel.BASIC,
                        "React와 TypeScript를 활용한 모던 프론트엔드 개발을 배웁니다.",
                        "1주차: React 기초\n2주차: TypeScript 적용\n3주차: 상태관리\n4주차: 프로젝트",
                        "프론트엔드 개발자를 꿈꾸는 분들 환영합니다!",
                        15000, 2, 6,
                        today.plusDays(10), today.plusDays(38), today.plusDays(8),
                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                        LocalTime.of(19, 30), LocalTime.of(21, 30),
                        2L, "스터디장2", 6
                ),
                new StudyData(
                        "Python 알고리즘 스터디",
                        StudyCategory.DEVELOPMENT,
                        StudyLevel.BEGINNER,
                        "코딩테스트 대비 알고리즘 문제풀이 스터디입니다. 매주 10문제씩 풀어봅니다.",
                        "매주 백준/프로그래머스 문제 풀이 및 코드 리뷰",
                        "알고리즘 기초부터 차근차근 함께해요!",
                        10000, 4, 10,
                        today.plusDays(3), today.plusDays(31), today.plusDays(1),
                        Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY),
                        LocalTime.of(10, 0), LocalTime.of(12, 0),
                        3L, "스터디장3", 4
                ),
                new StudyData(
                        "Docker & Kubernetes 입문",
                        StudyCategory.DEVELOPMENT,
                        StudyLevel.INTERMEDIATE,
                        "컨테이너 기술과 오케스트레이션을 실습 중심으로 배웁니다.",
                        "1주차: Docker 기초\n2주차: Docker Compose\n3주차: Kubernetes 입문\n4주차: 배포 실습",
                        "DevOps에 관심 있는 분들 모여주세요!",
                        25000, 3, 8,
                        today.plusDays(14), today.plusDays(42), today.plusDays(12),
                        Set.of(DayOfWeek.SATURDAY),
                        LocalTime.of(14, 0), LocalTime.of(17, 0),
                        1L, "스터디장4", 4
                ),
                new StudyData(
                        "Flutter 앱 개발 입문",
                        StudyCategory.DEVELOPMENT,
                        StudyLevel.BEGINNER,
                        "Flutter로 iOS/Android 앱을 동시에 개발하는 방법을 배웁니다.",
                        "1주차: Dart 기초\n2주차: Flutter 위젯\n3주차: 상태관리\n4주차: 미니 프로젝트",
                        "크로스플랫폼 앱 개발에 도전해보세요!",
                        15000, 2, 6,
                        today.plusDays(5), today.plusDays(33), today.plusDays(3),
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                        LocalTime.of(21, 0), LocalTime.of(23, 0),
                        2L, "스터디장5", 4
                ),

                // ===== 외국어 카테고리 =====
                new StudyData(
                        "영어 회화 스터디 - 중급",
                        StudyCategory.LANGUAGE,
                        StudyLevel.INTERMEDIATE,
                        "원어민 수준의 영어 회화를 목표로 프리토킹 위주로 진행합니다.",
                        "매주 주제를 정해 영어로 토론하고 피드백을 주고받습니다.",
                        "영어로 자유롭게 대화하고 싶은 분들 환영!",
                        20000, 3, 6,
                        today.plusDays(7), today.plusDays(49), today.plusDays(5),
                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                        LocalTime.of(7, 0), LocalTime.of(8, 0),
                        3L, "스터디장6", 12
                ),
                new StudyData(
                        "일본어 JLPT N2 대비반",
                        StudyCategory.LANGUAGE,
                        StudyLevel.INTERMEDIATE,
                        "JLPT N2 시험 대비 문법, 어휘, 독해를 집중적으로 공부합니다.",
                        "문법 정리 → 문제 풀이 → 오답 분석",
                        "7월 시험 합격을 목표로 함께 달려요!",
                        25000, 2, 5,
                        today.plusDays(10), today.plusDays(66), today.plusDays(8),
                        Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(10, 0), LocalTime.of(12, 0),
                        1L, "스터디장7", 16
                ),
                new StudyData(
                        "중국어 HSK 4급 스터디",
                        StudyCategory.LANGUAGE,
                        StudyLevel.BASIC,
                        "HSK 4급 취득을 목표로 체계적으로 공부합니다.",
                        "어휘 → 문법 → 듣기 → 독해 순서로 진행",
                        "중국어 기초가 있는 분들과 함께하고 싶어요!",
                        20000, 3, 8,
                        today.plusDays(12), today.plusDays(54), today.plusDays(10),
                        Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                        LocalTime.of(19, 0), LocalTime.of(21, 0),
                        2L, "스터디장8", 12
                ),

                // ===== 자격증 카테고리 =====
                new StudyData(
                        "정보처리기사 실기 대비",
                        StudyCategory.CERTIFICATE,
                        StudyLevel.INTERMEDIATE,
                        "정보처리기사 실기 시험 대비 스터디입니다. 기출문제 위주로 학습합니다.",
                        "매주 기출문제 풀이 및 오답 정리, 모의고사 진행",
                        "올해 안에 꼭 합격합시다!",
                        30000, 3, 10,
                        today.plusDays(5), today.plusDays(40), today.plusDays(3),
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                        LocalTime.of(20, 0), LocalTime.of(22, 0),
                        3L, "스터디장9", 15
                ),
                new StudyData(
                        "SQLD 자격증 스터디",
                        StudyCategory.CERTIFICATE,
                        StudyLevel.BASIC,
                        "SQL 개발자 자격증 취득을 위한 스터디입니다.",
                        "이론 학습 → 문제 풀이 → 오답 분석",
                        "DB 기초부터 시작하는 분들도 환영해요!",
                        15000, 2, 6,
                        today.plusDays(8), today.plusDays(36), today.plusDays(6),
                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                        LocalTime.of(21, 0), LocalTime.of(22, 30),
                        1L, "스터디장10", 8
                ),
                new StudyData(
                        "AWS SAA 자격증 스터디",
                        StudyCategory.CERTIFICATE,
                        StudyLevel.INTERMEDIATE,
                        "AWS Solutions Architect Associate 자격증 취득 스터디입니다.",
                        "AWS 공식 문서 학습 + Udemy 강의 + 덤프 풀이",
                        "클라우드 전문가가 되어봅시다!",
                        35000, 2, 5,
                        today.plusDays(14), today.plusDays(56), today.plusDays(12),
                        Set.of(DayOfWeek.SATURDAY),
                        LocalTime.of(10, 0), LocalTime.of(13, 0),
                        1L, "스터디장1", 6
                ),

                // ===== 취미 카테고리 =====
                new StudyData(
                        "독서 토론 모임",
                        StudyCategory.HOBBY,
                        StudyLevel.BEGINNER,
                        "매주 한 권의 책을 읽고 토론하는 독서 모임입니다.",
                        "자기계발, 에세이, 소설 등 다양한 장르의 책을 함께 읽습니다.",
                        "책을 좋아하는 분들과 생각을 나누고 싶어요!",
                        10000, 3, 8,
                        today.plusDays(6), today.plusDays(48), today.plusDays(4),
                        Set.of(DayOfWeek.SUNDAY),
                        LocalTime.of(14, 0), LocalTime.of(16, 0),
                        2L, "스터디장2", 6
                ),
                new StudyData(
                        "사이드 프로젝트 팀빌딩",
                        StudyCategory.HOBBY,
                        StudyLevel.INTERMEDIATE,
                        "함께 사이드 프로젝트를 기획하고 개발하는 스터디입니다.",
                        "아이디어 회의 → 기획 → 개발 → 배포까지",
                        "포트폴리오용 프로젝트를 만들고 싶은 분들 모여주세요!",
                        5000, 4, 8,
                        today.plusDays(10), today.plusDays(80), today.plusDays(8),
                        Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(13, 0), LocalTime.of(17, 0),
                        3L, "스터디장3", 20
                ),

                // ===== 기타 카테고리 =====
                new StudyData(
                        "취업 면접 스터디",
                        StudyCategory.ETC,
                        StudyLevel.BEGINNER,
                        "IT 기업 취업을 위한 면접 준비 스터디입니다.",
                        "자기소개 → 기술 면접 → 인성 면접 모의 면접 진행",
                        "함께 취뽀해요!",
                        15000, 3, 6,
                        today.plusDays(4), today.plusDays(32), today.plusDays(2),
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                        LocalTime.of(20, 0), LocalTime.of(22, 0),
                        1L, "스터디장4", 8
                ),
                new StudyData(
                        "CS 기초 지식 스터디",
                        StudyCategory.ETC,
                        StudyLevel.BASIC,
                        "운영체제, 네트워크, 데이터베이스 등 CS 기초를 학습합니다.",
                        "주제별 발표 및 질의응답, 면접 예상 질문 정리",
                        "탄탄한 CS 지식을 쌓아봅시다!",
                        20000, 2, 8,
                        today.plusDays(9), today.plusDays(51), today.plusDays(7),
                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
                        LocalTime.of(21, 0), LocalTime.of(23, 0),
                        2L, "스터디장5", 18
                )
        );
    }

    private Study createStudy(StudyData data) {
        StudySchedule schedule = new StudySchedule(
                data.startDate,
                data.endDate,
                data.startTime,
                data.endTime,
                data.recruitEndDate,
                data.daysOfWeek
        );

        Study study = Study.create(
                data.title,
                THUMBNAIL_URL,
                data.category,
                data.level,
                StudyVisibility.PUBLIC,
                data.leaderId,
                schedule,
                data.minMembers,
                data.maxMembers,
                data.deposit,
                data.introduction,
                data.curriculum,
                data.leaderMessage,
                StudyStatus.RECRUITING
        );

        // 스터디장을 멤버로 추가
        study.addLeaderAsMember(data.deposit, data.leaderNickname);

        // 세션 생성
        study.createSessions(data.totalSessions);

        return study;
    }

    private record StudyData(
            String title,
            StudyCategory category,
            StudyLevel level,
            String introduction,
            String curriculum,
            String leaderMessage,
            int deposit,
            int minMembers,
            int maxMembers,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate recruitEndDate,
            Set<DayOfWeek> daysOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Long leaderId,
            String leaderNickname,
            int totalSessions
    ) {}
}
