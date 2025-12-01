package com.grow.member.application.member.provided;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleUserResponse {
    private String id;
    private String email;
    private String name;
    private String picture;
}