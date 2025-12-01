package com.grow.member.domain.member;

//의존 방향을 생각하면 PasswordEncoder는 domain layer에 위치하는 것이 맞다.
public interface PasswordEncoder {
    String encode(String password);
    boolean matches(String password, String passwordHash);
}