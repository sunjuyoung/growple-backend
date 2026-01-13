package com.grow.study.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 처리된 이벤트 저장 (멱등성 보장)
 */
@Entity
@Table(name = "processed_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = LocalDateTime.now();
    }

    public static ProcessedEvent of(String eventId, String eventType) {
        return new ProcessedEvent(eventId, eventType);
    }
}
