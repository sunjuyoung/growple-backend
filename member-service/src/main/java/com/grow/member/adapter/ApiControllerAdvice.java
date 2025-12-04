package com.grow.member.adapter;

import com.grow.member.application.InvalidTokenException;
import com.grow.member.domain.member.DuplicationEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class ApiControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception exception){
        return  getProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,exception);
    }

    @ExceptionHandler(DuplicationEmailException.class)
    public ProblemDetail emailExceptionHandler(DuplicationEmailException exception){
        return getProblemDetail(HttpStatus.CONFLICT,exception);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail invalidTokenExceptionHandler(InvalidTokenException exception){
        return getProblemDetail(HttpStatus.UNAUTHORIZED,exception);
    }

    private static ProblemDetail getProblemDetail(HttpStatus status,Exception exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setProperty("exception", exception.getClass().getSimpleName());
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }
}
