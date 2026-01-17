package com.grow.favorite.domain.favorite;

import com.grow.favorite.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "study_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite extends AbstractEntity {


    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "study_id", nullable = false)
    private Long studyId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Favorite(Long memberId, Long studyId) {
        this.memberId = memberId;
        this.studyId = studyId;
        this.createdAt = LocalDateTime.now();
    }

    public static Favorite create(Long memberId, Long studyId) {
        return Favorite.builder()
                .memberId(memberId)
                .studyId(studyId)
                .build();
    }
}
