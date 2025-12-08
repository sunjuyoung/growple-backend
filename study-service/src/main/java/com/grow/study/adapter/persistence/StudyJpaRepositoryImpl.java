package com.grow.study.adapter.persistence;

import com.grow.study.adapter.persistence.dto.CursorResult;
import com.grow.study.adapter.persistence.dto.StudyListResponse;
import com.grow.study.adapter.persistence.dto.StudySearchCondition;
import com.grow.study.adapter.persistence.dto.StudySearchCondition.StudySortType;
import com.grow.study.domain.study.Study;
import com.grow.study.domain.study.StudyCategory;
import com.grow.study.domain.study.StudyLevel;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.grow.study.domain.study.QStudy.study;

@RequiredArgsConstructor
public class StudyJpaRepositoryImpl implements StudyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<StudyListResponse> searchStudyList(StudySearchCondition condition, Pageable pageable) {

        List<Study> content = queryFactory
                .selectFrom(study)
                .leftJoin(study.schedule.daysOfWeek).fetchJoin()
                .where(
                        levelEq(condition.getLevel()),
                        categoryEq(condition.getCategory()),
                        depositAmountBetween(condition.getMinDepositAmount(), condition.getMaxDepositAmount())
                )
                .orderBy(getOrderSpecifier(condition.getSortType()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(study.count())
                .from(study)
                .where(
                        levelEq(condition.getLevel()),
                        categoryEq(condition.getCategory()),
                        depositAmountBetween(condition.getMinDepositAmount(), condition.getMaxDepositAmount())
                );

        List<StudyListResponse> responses = content.stream()
                .map(StudyListResponse::from)
                .toList();

        return PageableExecutionUtils.getPage(responses, pageable, countQuery::fetchOne);
    }

    private BooleanExpression levelEq(StudyLevel level) {
        return level != null ? study.level.eq(level) : null;
    }

    private BooleanExpression categoryEq(StudyCategory category) {
        return category != null ? study.category.eq(category) : null;
    }

    private BooleanExpression depositAmountBetween(Integer minAmount, Integer maxAmount) {
        if (minAmount != null && maxAmount != null) {
            return study.depositAmount.between(minAmount, maxAmount);
        } else if (minAmount != null) {
            return study.depositAmount.goe(minAmount);
        } else if (maxAmount != null) {
            return study.depositAmount.loe(maxAmount);
        }
        return null;
    }

    private OrderSpecifier<?> getOrderSpecifier(StudySortType sortType) {
        if (sortType == null || sortType == StudySortType.LATEST) {
            return study.createdAt.desc();
        }
        return study.schedule.startDate.asc();
    }

    @Override
    public CursorResult<StudyListResponse> searchStudyListByCursor(
            StudySearchCondition condition,
            String cursor,
            int size
    ) {
        StudySortType sortType = condition.getSortType() != null ? condition.getSortType() : StudySortType.LATEST;

        List<Study> content = queryFactory
                .selectFrom(study)
                .leftJoin(study.schedule.daysOfWeek).fetchJoin()
                .where(
                        cursorCondition(cursor, sortType),
                        levelEq(condition.getLevel()),
                        categoryEq(condition.getCategory()),
                        depositAmountBetween(condition.getMinDepositAmount(), condition.getMaxDepositAmount())
                )
                .orderBy(getOrderSpecifiers(sortType))
                .limit(size + 1)
                .fetch();

        boolean hasNext = content.size() > size;
        if (hasNext) {
            content = content.subList(0, size);
        }

        List<StudyListResponse> responses = content.stream()
                .map(StudyListResponse::from)
                .toList();

        String nextCursor = hasNext && !content.isEmpty()
                ? generateCursor(content.get(content.size() - 1), sortType)
                : null;

        return CursorResult.of(responses, nextCursor, hasNext);
    }

    private BooleanExpression cursorCondition(String cursor, StudySortType sortType) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        String[] parts = cursor.split("_");
        if (parts.length != 2) {
            return null;
        }

        Long id = Long.parseLong(parts[1]);

        if (sortType == StudySortType.LATEST) {
            LocalDateTime createdAt = LocalDateTime.parse(parts[0]);
            return study.createdAt.lt(createdAt)
                    .or(study.createdAt.eq(createdAt).and(study.id.lt(id)));
        }else if(sortType == StudySortType.POPULARITY){
            LocalDate startDate = LocalDate.parse(parts[0]);
            return study.schedule.startDate.gt(startDate)
                    .or(study.schedule.startDate.eq(startDate).and(study.id.gt(id)));
        } else {
            LocalDate startDate = LocalDate.parse(parts[0]);
            return study.schedule.startDate.gt(startDate)
                    .or(study.schedule.startDate.eq(startDate).and(study.id.gt(id)));
        }
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(StudySortType sortType) {
        if (sortType == StudySortType.LATEST) {
            return new OrderSpecifier<?>[] {
                    study.createdAt.desc(),
                    study.id.desc()
            };
        }
        return new OrderSpecifier<?>[] {
                study.schedule.startDate.asc(),
                study.id.asc()
        };
    }

    private String generateCursor(Study lastStudy, StudySortType sortType) {
        if (sortType == StudySortType.LATEST) {
            return lastStudy.getCreatedAt().toString() + "_" + lastStudy.getId();
        }
        return lastStudy.getSchedule().getStartDate().toString() + "_" + lastStudy.getId();
    }
}
