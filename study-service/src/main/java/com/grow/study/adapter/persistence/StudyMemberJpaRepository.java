package com.grow.study.adapter.persistence;

import com.grow.study.domain.study.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface StudyMemberJpaRepository extends JpaRepository<StudyMember, Long> {

    /**
     * 스터디와 회원으로 스터디 멤버 조회
     */
    @Query("SELECT sm FROM StudyMember sm WHERE sm.study.id = :studyId AND sm.memberId = :memberId")
    Optional<StudyMember> findByStudyIdAndMemberId(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId
    );

    /**
     * 스터디의 모든 활성화된 멤버 조회
     */
    @Query("SELECT sm FROM StudyMember sm WHERE sm.study.id = :studyId AND sm.status = 'ACTIVE'")
    List<StudyMember> findByStudyId(@Param("studyId") Long studyId);
}
