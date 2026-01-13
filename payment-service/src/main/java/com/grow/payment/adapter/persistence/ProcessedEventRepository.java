package com.grow.payment.adapter.persistence;

import com.grow.payment.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
