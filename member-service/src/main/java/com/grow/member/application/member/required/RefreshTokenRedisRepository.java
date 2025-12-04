package com.grow.member.application.member.required;

import java.util.Optional;

public interface RefreshTokenRedisRepository {

    public void save(Long userId, String deviceId, RefreshTokenData data) ;

    // 조회
    public Optional<RefreshTokenData> find(Long userId, String deviceId) ;

    // 삭제 (로그아웃)
    public void delete(Long userId, String deviceId) ;

    // 토큰으로 검증 (Rotation 시 탈취 감지용)
    public boolean validateToken(Long userId, String deviceId, String token) ;
}
