package com.grow.study.adapter.persistence;


import com.grow.study.domain.study.Session;
import com.grow.study.domain.study.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 세션 JPA Repository
 */
public interface SessionJpaRepository extends JpaRepository<Session, Long> {

    /**
     * 스터디의 모든 세션 조회
     */
    @Query("SELECT s FROM Session s WHERE s.study.id = :studyId ORDER BY s.sessionNumber")
    List<Session> findByStudyIdOrderBySessionNumber(@Param("studyId") Long studyId);

    /**
     * 스터디의 특정 상태 세션 조회
     */
    @Query("SELECT s FROM Session s WHERE s.study.id = :studyId AND s.status = :status")
    List<Session> findByStudyIdAndStatus(
            @Param("studyId") Long studyId,
            @Param("status") SessionStatus status
    );

    /**
     * 특정 날짜의 세션 조회
     */
    @Query("SELECT s FROM Session s WHERE s.sessionDate = :date")
    List<Session> findBySessionDate(@Param("date") LocalDate date);

    /**
     * 출석 체크 가능한 세션 조회 (현재 진행 중)
     */
    @Query("SELECT s FROM Session s " +
            "WHERE s.study.id = :studyId " +
            "AND s.status = 'IN_PROGRESS' " +
            "AND CURRENT_TIMESTAMP BETWEEN s.attendanceCheckStartTime AND s.attendanceCheckEndTime")
    List<Session> findAttendanceCheckAvailableSessions(@Param("studyId") Long studyId);
}

