package com.grow.study.application.required.dto;

import com.grow.study.domain.study.Study;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudyWithMemberCountDto {

    Study study;
   // Long memberCount;

    public static StudyWithMemberCountDto of(Study study) {
        return new StudyWithMemberCountDto(study);
    }
}
