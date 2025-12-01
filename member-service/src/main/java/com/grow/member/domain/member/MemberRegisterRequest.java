package com.grow.member.domain.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record MemberRegisterRequest(
        @Email String email,
        @Size(min=4, max=20) String password,
        @Size(min=4, max=20) String nickname
) {
}
