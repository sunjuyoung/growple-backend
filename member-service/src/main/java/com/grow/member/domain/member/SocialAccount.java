package com.grow.member.domain.member;


import com.grow.member.application.member.required.SocialUserInfo;
import com.grow.member.domain.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_accounts", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_social_provider_id",
                columnNames = {"provider", "provider_id"}
        )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "member")
public class SocialAccount extends AbstractEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @Comment("회원 ID")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("소셜 로그인 제공자")
    private SocialProvider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    @Comment("소셜 제공자의 사용자 ID")
    private String providerId;

    @Column(length = 100)
    @Comment("소셜 계정 이메일")
    private String email;

    @Column(length = 100)
    @Comment("소셜 계정 이름")
    private String name;

    @Column(length = 500)
    @Comment("소셜 프로필 이미지 URL")
    private String profileImageUrl;

    @Column(nullable = false, updatable = false)
    @Comment("연동일시")
    private LocalDateTime createdAt;

    @Builder
    public SocialAccount(SocialProvider provider, String providerId,
                         String email, String name, String profileImageUrl) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = LocalDateTime.now();
    }

    public static SocialAccount of(SocialUserInfo socialUserInfo){
        return SocialAccount.builder()
                .providerId(socialUserInfo.id())
                .email(socialUserInfo.email())
                .name(socialUserInfo.name())
                .provider(socialUserInfo.socialProvider())
                .build();
    }


    public void settingProvider(SocialProvider provider) {
        this.provider = provider;
    }


    // Member 연관관계 설정 (패키지 레벨)
    void setMember(Member member) {
        this.member = member;
    }
}