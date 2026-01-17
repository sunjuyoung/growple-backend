package com.grow.hotstudy.domain.hotstudy;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "hot_studies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HotStudy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "study_id", nullable = false, unique = true)
    private Long studyId;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "view_count")
    private Long viewCount;

    @Column(name = "like_count")
    private Long likeCount;

    @Column(name = "enrollment_count")
    private Long enrollmentCount;

    @Column(name = "ranking")
    private Integer ranking;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Builder
    public HotStudy(Long studyId, Double score, Long viewCount, Long likeCount, Long enrollmentCount, Integer ranking) {
        this.studyId = studyId;
        this.score = score;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.enrollmentCount = enrollmentCount;
        this.ranking = ranking;
        this.calculatedAt = LocalDateTime.now();
    }

    public void updateScore(Double score, Long viewCount, Long likeCount, Long enrollmentCount, Integer ranking) {
        this.score = score;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.enrollmentCount = enrollmentCount;
        this.ranking = ranking;
        this.calculatedAt = LocalDateTime.now();
    }
}
