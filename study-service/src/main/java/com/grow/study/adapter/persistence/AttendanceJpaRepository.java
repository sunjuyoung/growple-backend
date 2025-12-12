package com.grow.study.adapter.persistence;


import com.grow.study.domain.study.Attendance;
import com.grow.study.domain.study.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 출석 JPA Repository
 */
public interface AttendanceJpaRepository extends JpaRepository<Attendance, Long> {

    /**
     * 세션과 회원으로 출석 조회
     */
    @Query("SELECT a FROM Attendance a WHERE a.session.id = :sessionId AND a.memberId = :memberId")
    Optional<Attendance> findBySessionIdAndMemberId(
            @Param("sessionId") Long sessionId,
            @Param("memberId") Long memberId
    );

    /**
     * 세션의 모든 출석 조회
     */
    @Query("SELECT a FROM Attendance a WHERE a.session.id = :sessionId")
    List<Attendance> findBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 회원의 스터디별 출석 조회
     */
    @Query("SELECT a FROM Attendance a " +
            "WHERE a.session.study.id = :studyId AND a.memberId = :memberId " +
            "ORDER BY a.session.sessionNumber")
    List<Attendance> findByStudyIdAndMemberId(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId
    );

    /**
     * 회원의 출석 상태별 개수 조회
     */
    @Query("SELECT COUNT(a) FROM Attendance a " +
            "WHERE a.session.study.id = :studyId " +
            "AND a.memberId = :memberId " +
            "AND a.status = :status")
    Long countByStudyIdAndMemberIdAndStatus(
            @Param("studyId") Long studyId,
            @Param("memberId") Long memberId,
            @Param("status") AttendanceStatus status
    );

    /**
     * 세션에 출석했는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Attendance a " +
            "WHERE a.session.id = :sessionId AND a.memberId = :memberId")
    boolean existsBySessionIdAndMemberId(
            @Param("sessionId") Long sessionId,
            @Param("memberId") Long memberId
    );
}
