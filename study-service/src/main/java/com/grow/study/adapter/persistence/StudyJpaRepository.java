package com.grow.study.adapter.persistence;

import com.grow.study.application.required.dto.StudyWithMemberCountDto;
import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyStatus;
import com.grow.study.domain.study.StudyVisibility;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyJpaRepository extends JpaRepository<Study, Long>, StudyRepositoryCustom {

    @Query("""
    SELECT s FROM Study s
    LEFT JOIN FETCH s.schedule.daysOfWeek
    WHERE s.id = :studyId
    """)
    Optional<Study> findWithSchedule(@Param("studyId") Long studyId);

    // 2. Member Count 별도 조회
    @Query("""
    SELECT COUNT(sm) FROM StudyMember sm
    WHERE sm.study.id = :studyId AND sm.status = 'ACTIVE'
    """)
    Long countActiveMembers(@Param("studyId") Long studyId);

    @EntityGraph(attributePaths = {"members"})
    Optional<Study> findStudiesById(Long studyId);
}
