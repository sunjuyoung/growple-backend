package com.grow.study.application;

import com.grow.study.application.required.StudyRepository;
import com.grow.study.domain.study.Study;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 스터디 추천 서비스
 * pgvector 기반 유사 스터디 추천
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyRecommendationService {

    private final StudyVectorService studyVectorService;
    private final StudyRepository studyRepository;

    /**
     * 유사 스터디 추천
     * @param studyId 기준 스터디 ID
     * @param limit 추천할 스터디 개수
     * @return 추천 스터디 리스트
     */
    public List<Study> recommendSimilarStudies(Long studyId, int limit) {
        log.info("Recommending similar studies for studyId: {}, limit: {}", studyId, limit);

        // 1. 벡터 검색으로 유사 스터디 ID 조회
        List<Long> similarStudyIds = studyVectorService.findSimilarStudies(studyId, limit);

        if (similarStudyIds.isEmpty()) {
            log.info("No similar studies found for studyId: {}", studyId);
            return List.of();
        }

        // 2. Study 엔티티 조회
        return similarStudyIds.stream()
                .map(id -> studyRepository.findById(id).orElse(null))
                .filter(study -> study != null)
                .collect(Collectors.toList());
    }

    /**
     * 멤버 관심사 기반 스터디 추천
     * @param userId 유저 ID (캐시 키로 사용)
     * @param memberIntroduction 멤버 소개 (관심사)
     * @param limit 추천할 스터디 개수
     * @return 추천 스터디 리스트
     */
    public List<Study> recommendStudiesByMemberInterest(Long userId, String memberIntroduction, int limit) {

        // 1. 벡터 검색으로 추천 스터디 ID 조회
        List<Long> recommendedStudyIds = studyVectorService.recommendStudiesByMemberInterest(
                userId,
                memberIntroduction,
                limit
        );

        if (recommendedStudyIds.isEmpty()) {
            log.info("No studies recommended for member interest");
            return List.of();
        }

        // 2. Study 엔티티 조회
        return recommendedStudyIds.stream()
                .map(id -> studyRepository.findById(id).orElse(null))
                .filter(study -> study != null)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 유사 스터디 추천
     * @param studyContent 검색 기준 텍스트
     * @param category 카테고리
     * @param limit 추천할 스터디 개수
     * @return 추천 스터디 리스트
     */
    public List<Study> recommendStudiesByCategory(String studyContent, String category, int limit) {
        log.info("Recommending studies by category: {}, limit: {}", category, limit);

        // 1. 벡터 검색으로 유사 스터디 ID 조회
        List<Long> similarStudyIds = studyVectorService.findSimilarStudiesByCategory(
                studyContent,
                category,
                limit
        );

        if (similarStudyIds.isEmpty()) {
            log.info("No similar studies found for category: {}", category);
            return List.of();
        }

        // 2. Study 엔티티 조회
        return similarStudyIds.stream()
                .map(id -> studyRepository.findById(id).orElse(null))
                .filter(study -> study != null)
                .collect(Collectors.toList());
    }
}
