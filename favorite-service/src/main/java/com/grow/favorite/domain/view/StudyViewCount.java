package com.grow.favorite.domain.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Table
@Getter
@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyViewCount {

    @Id
    private Long studyId;
    private Long count;

    public static StudyViewCount init(Long studyId, Long count){
        StudyViewCount studyViewCount = new StudyViewCount();
        studyViewCount.count = count;
        studyViewCount.studyId = studyId;
        return studyViewCount;
    }
}
