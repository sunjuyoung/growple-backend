package com.grow.study.application.dto;

import java.util.List;

public record StudyMemberListResponse(
        Long studyId,
        String studyTitle,
        int totalCount,
        List<StudyMemberDetailResponse> members
) {
    public static StudyMemberListResponse of(Long studyId, String studyTitle, List<StudyMemberDetailResponse> members) {
        return new StudyMemberListResponse(studyId, studyTitle, members.size(), members);
    }
}
