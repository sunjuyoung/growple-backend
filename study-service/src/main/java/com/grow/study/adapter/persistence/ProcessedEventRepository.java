package com.grow.study.adapter.persistence;

import com.grow.study.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
