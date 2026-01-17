package com.grow.favorite.adapter.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyViewCountRepository {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_FORMAT = "view::study::%s::view_count";

    public Long read(Long studyId){
        String result = stringRedisTemplate.opsForValue().get(generateKey(studyId));
        return result == null ? 0 :  Long.valueOf(result);
    }


    public Long increase(Long studyId){
        return stringRedisTemplate.opsForValue().increment(generateKey(studyId));
    }

    private String generateKey(Long studyId) {
        return KEY_FORMAT.formatted(studyId);
    }
}
