package com.grow.member.adapter.persistence;

import com.grow.member.application.member.required.RefreshTokenData;
import com.grow.member.application.member.required.RefreshTokenRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository implements RefreshTokenRedisRepository {

    private final RedisTemplate<String, RefreshTokenData> refreshTokenRedisTemplate;

    @Value("${jwt.refresh-token-ttl-days}")
    private int ttlDays;

    private static final String KEY_PREFIX = "refresh_token:";

    // 저장
    @Override
    public void save(Long userId, String deviceId, RefreshTokenData data) {
        String key = generateKey(userId, deviceId);
        refreshTokenRedisTemplate.opsForValue().set(key, data, ttlDays, TimeUnit.DAYS);
    }

    // 조회
    @Override
    public Optional<RefreshTokenData> find(Long userId, String deviceId) {
        String key = generateKey(userId, deviceId);
        RefreshTokenData data = refreshTokenRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(data);
    }

    // 삭제 (로그아웃)
    @Override
    public void delete(Long userId, String deviceId) {
        String key = generateKey(userId, deviceId);
        refreshTokenRedisTemplate.delete(key);
    }

    // 토큰으로 검증 (Rotation 시 탈취 감지용)
    @Override
    public boolean validateToken(Long userId, String deviceId, String token) {
        return find(userId, deviceId)
                .map(data -> data.token().equals(token))
                .orElse(false);
    }

    private String generateKey(Long userId, String deviceId) {
        return KEY_PREFIX + userId + ":" + deviceId;
    }
}