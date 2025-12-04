package com.grow.study.adapter.webapi;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study")
public class StudyApi {

    @GetMapping
    public String hello() {
        return "Hello Study Service!";
    }
}
