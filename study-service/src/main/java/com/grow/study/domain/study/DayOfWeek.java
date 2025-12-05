package com.grow.study.domain.study;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DayOfWeek {
    MONDAY("월요일", "월"),
    TUESDAY("화요일", "화"),
    WEDNESDAY("수요일", "수"),
    THURSDAY("목요일", "목"),
    FRIDAY("금요일", "금"),
    SATURDAY("토요일", "토"),
    SUNDAY("일요일", "일");

    private final String displayName;
    private final String shortName;

    public static DayOfWeek from(java.time.DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> MONDAY;
            case TUESDAY -> TUESDAY;
            case WEDNESDAY -> WEDNESDAY;
            case THURSDAY -> THURSDAY;
            case FRIDAY -> FRIDAY;
            case SATURDAY -> SATURDAY;
            case SUNDAY -> SUNDAY;
        };
    }

    //shortName 으로부터 DayOfWeek 찾기
    public static DayOfWeek fromShortName(String shortName) {
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day.getShortName().equals(shortName)) {
                return day;
            }
        }
        throw new IllegalArgumentException("Invalid short name for DayOfWeek: " + shortName);
    }
}
