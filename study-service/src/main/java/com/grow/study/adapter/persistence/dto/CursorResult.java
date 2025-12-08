package com.grow.study.adapter.persistence.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class CursorResult<T> {

    private final List<T> content;
    private final String nextCursor;
    private final boolean hasNext;

    private CursorResult(List<T> content, String nextCursor, boolean hasNext) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static <T> CursorResult<T> of(List<T> content, String nextCursor, boolean hasNext) {
        return new CursorResult<>(content, nextCursor, hasNext);
    }
}
