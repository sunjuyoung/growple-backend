package com.grow.study.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    PRESENT("출석", "정상적으로 출석한 상태"),
    ABSENT("결석", "출석 체크를 하지 않아 결석 처리된 상태");
    
    //미구현
//    LATE("지각", "출석 체크 시간 이후에 출석한 상태 (선택적 기능)"),
//    EXCUSED("사유 결석", "스터디장이 인정한 사유로 결석한 상태 (선택적 기능)");

    private final String displayName;
    private final String description;

    public boolean isPresent() {
        return this == PRESENT ;
    }

    public boolean isAbsent() {
        return this == ABSENT ;
    }
}
