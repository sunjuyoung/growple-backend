package com.grow.member.application.member.provided;

import com.grow.member.application.member.required.LoginResponse;
import com.grow.member.application.member.required.TokenResponse;

public interface MemberAuth {

     LoginResponse signIn(String email, String password,String deviceId  ,String userAgent, String ip) ;

     void logout(Long memberId, String deviceId);

     TokenResponse refresh(String refreshToken, String deviceId, String userAgent, String ip);
}
