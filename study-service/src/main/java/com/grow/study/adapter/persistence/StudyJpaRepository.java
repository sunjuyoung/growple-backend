package com.grow.study.adapter.persistence;

import com.grow.study.application.required.dto.StudyWithMemberCountDto;
import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyStatus;
import com.grow.study.domain.study.StudyVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyJpaRepository extends JpaRepository<Study, Long> {


    @Query("""
        SELECT new com.grow.study.application.required.dto.StudyWithMemberCountDto(
            s, 
            COUNT(sm)
        )
        FROM Study s
        LEFT JOIN StudyMember sm ON sm.study = s AND sm.status = 'ACTIVE'
        WHERE s.id = :studyId
        GROUP BY s
        """)
    Optional<StudyWithMemberCountDto> findWithMemberCount(@Param("studyId") Long studyId);

    /**
     * 리더 ID로 스터디 목록 조회
     */
    List<Study> findByLeaderId(Long leaderId);

    /**
     * 카테고리로 스터디 목록 조회
     */
    List<Study> findByCategory(StudyCategory category);

    /**
     * 상태로 스터디 목록 조회
     */
    List<Study> findByStatus(StudyStatus status);

    /**
     * 공개 여부로 스터디 목록 조회
     */
    List<Study> findByVisibility(StudyVisibility visibility);
}
