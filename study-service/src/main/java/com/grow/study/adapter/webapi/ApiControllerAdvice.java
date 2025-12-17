package com.grow.study.adapter.webapi;

import com.grow.study.application.NonRetryableException;
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


    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail  illegalArgumentException(IllegalArgumentException exception){
        return getProblemDetail(HttpStatus.CONFLICT,exception);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail illegalStateException(IllegalStateException exception){
        return getProblemDetail(HttpStatus.CONFLICT,exception);
    }

    @ExceptionHandler(NonRetryableException.class)
    public ProblemDetail emailExceptionHandler(NonRetryableException exception){
        return getProblemDetail(HttpStatus.CONFLICT,exception);
    }


    private static ProblemDetail getProblemDetail(HttpStatus status,Exception exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problemDetail.setProperty("exception", exception.getClass().getSimpleName());
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }
}

