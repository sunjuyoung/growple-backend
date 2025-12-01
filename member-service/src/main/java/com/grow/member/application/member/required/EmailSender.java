package com.grow.member.application.member.required;

import com.grow.member.domain.Email;

/**
 * 이메일 발송 기능을 제공한다.
 */
public interface EmailSender {

    void send(Email email, String subject, String content);
}
