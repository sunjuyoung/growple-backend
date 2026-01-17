package com.grow.favorite.adapter.persistence;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class StudyViewRedisLockRepository {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_FORMAT = "view::study::%s::user::%s::lock";

    public boolean viewLock(Long studyId, Long userId){
        return redisTemplate.opsForValue().setIfAbsent(generateKey(studyId,userId),"", Duration.ofMinutes(10));
    }

    private String generateKey(Long articleId, Long userId) {
        return KEY_FORMAT.formatted(articleId, userId);
    }
}
