package com.grow.study.application;

import com.grow.study.domain.event.StudyCreatedEvent;
import com.grow.study.domain.study.StudyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 스터디 벡터 검색 서비스
 * pgvector를 활용한 유사 스터디 추천
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyVectorService {

    private final VectorStore vectorStore;

    /**
     * 스터디 생성 시 Document 생성 및 저장
     * 스터디 내용 기반 임베딩 생성
     */
    @Transactional
    public void createStudyDocument(Long studyId, StudyCreatedEvent event) {
        log.info("Creating vector document for study: {}", studyId);

        // 스터디 정보를 하나의 텍스트로 결합 (임베딩 대상)
        String content = buildStudyContent(event);

        // 메타데이터 설정
        Map<String, Object> metadata = buildMetadata(studyId, event);

        // Document 생성 및 저장
        Document document = new Document(String.valueOf(studyId), content, metadata);
        vectorStore.add(List.of(document));

        log.info("Vector document created successfully for study: {}", studyId);
    }

    /**
     * 유사 스터디 검색
     * @param studyId 기준 스터디 ID
     * @param topK 반환할 유사 스터디 개수
     * @return 유사 스터디 ID 리스트
     */
    public List<Long> findSimilarStudies(Long studyId, int topK) {
        log.info("Finding similar studies for study: {}, topK: {}", studyId, topK);

        // 1. 기준 스터디의 Document 조회
        SearchRequest searchRequest = SearchRequest.query(String.valueOf(studyId))
                .withTopK(1);

        List<Document> baseDocuments = vectorStore.similaritySearch(searchRequest);

        if (baseDocuments.isEmpty()) {
            log.warn("Study document not found: {}", studyId);
            return List.of();
        }

        Document baseDocument = baseDocuments.get(0);
        String baseContent = baseDocument.getContent();

        // 2. 유사 스터디 검색 (모집 중인 스터디만)
        SearchRequest similarityRequest = SearchRequest.query(baseContent)
                .withTopK(topK + 1) // 자기 자신 제외를 위해 +1
                .withSimilarityThreshold(0.7) // 유사도 70% 이상
                .withFilterExpression("status == 'RECRUITING'"); // 모집 중인 스터디만

        List<Document> similarDocuments = vectorStore.similaritySearch(similarityRequest);

        // 3. 자기 자신 제외 및 Study ID 추출
        return similarDocuments.stream()
                .map(doc -> Long.parseLong(doc.getId()))
                .filter(id -> !id.equals(studyId))
                .limit(topK)
                .toList();
    }

    /**
     * 멤버의 관심사 기반 스터디 추천
     * @param memberIntroduction 멤버 소개 (관심사)
     * @param topK 추천할 스터디 개수
     * @return 추천 스터디 ID 리스트
     */
    public List<Long> recommendStudiesByMemberInterest(String memberIntroduction, int topK) {
        log.info("Recommending studies based on member interest, topK: {}", topK);

        // 멤버 관심사로 유사 스터디 검색 (모집 중인 스터디만)
        SearchRequest searchRequest = SearchRequest.query(memberIntroduction)
                .withTopK(topK)
                .withSimilarityThreshold(0.6) // 유사도 60% 이상
                .withFilterExpression("status == 'RECRUITING'");

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        return documents.stream()
                .map(doc -> Long.parseLong(doc.getId()))
                .toList();
    }

    /**
     * 카테고리별 유사 스터디 검색
     * @param studyContent 검색 기준 텍스트
     * @param category 카테고리 필터
     * @param topK 반환할 개수
     * @return 유사 스터디 ID 리스트
     */
    public List<Long> findSimilarStudiesByCategory(String studyContent, String category, int topK) {
        log.info("Finding similar studies by category: {}, topK: {}", category, topK);

        SearchRequest searchRequest = SearchRequest.query(studyContent)
                .withTopK(topK)
                .withSimilarityThreshold(0.6)
                .withFilterExpression(String.format("category == '%s' && status == 'RECRUITING'", category));

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        return documents.stream()
                .map(doc -> Long.parseLong(doc.getId()))
                .toList();
    }

    /**
     * Document 삭제 (스터디 삭제 시)
     */
    @Transactional
    public void deleteStudyDocument(Long studyId) {
        log.info("Deleting vector document for study: {}", studyId);
        vectorStore.delete(List.of(String.valueOf(studyId)));
    }

    /**
     * 스터디 내용을 하나의 텍스트로 결합
     */
    private String buildStudyContent(StudyCreatedEvent event) {
        StringBuilder content = new StringBuilder();

        content.append("제목: ").append(event.title()).append("\n");

        if (event.introduction() != null) {
            content.append("소개: ").append(event.introduction()).append("\n");
        }

        if (event.curriculum() != null) {
            content.append("커리큘럼: ").append(event.curriculum()).append("\n");
        }

        if (event.leaderMessage() != null) {
            content.append("스터디장 메시지: ").append(event.leaderMessage()).append("\n");
        }

        content.append("카테고리: ").append(event.category().getDescription()).append("\n");
        content.append("난이도: ").append(event.level().getDescription());

        return content.toString();
    }

    /**
     * 메타데이터 생성
     */
    private Map<String, Object> buildMetadata(Long studyId, StudyCreatedEvent event) {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("studyId", studyId);
        metadata.put("title", event.title());
        metadata.put("category", event.category().name());
        metadata.put("level", event.level().name());
        metadata.put("status", event.status().name());
        metadata.put("minParticipants", event.minParticipants());
        metadata.put("maxParticipants", event.maxParticipants());
        metadata.put("startDate", event.startDate().toString());
        metadata.put("endDate", event.endDate().toString());

        return metadata;
    }
}
