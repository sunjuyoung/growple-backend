package com.grow.member.application.member.provided;

import com.grow.member.domain.member.SocialProvider;

public record SocialUserInfo (
         String id,        // 소셜 고유 ID
         String email,
         String name,
         String provider,
         SocialProvider socialProvider
){
}
