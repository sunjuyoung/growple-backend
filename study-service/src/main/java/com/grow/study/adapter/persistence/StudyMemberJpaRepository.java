package com.grow.study.adapter.persistence;

import com.grow.study.domain.study.StudyMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMemberJpaRepository extends JpaRepository<StudyMember, Long> {
}
