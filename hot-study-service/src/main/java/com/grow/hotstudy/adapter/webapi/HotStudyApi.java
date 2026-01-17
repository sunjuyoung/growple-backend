package com.grow.hotstudy.adapter.webapi;

import com.grow.hotstudy.application.provided.HotStudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hot-studies")
@RequiredArgsConstructor
public class HotStudyApi {

    private final HotStudyService hotStudyService;

}
