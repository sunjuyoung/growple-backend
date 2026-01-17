package com.grow.hotstudy.application;

import com.grow.hotstudy.application.provided.HotStudyService;
import com.grow.hotstudy.application.required.HotStudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotStudyServiceImpl implements HotStudyService {

    private final HotStudyRepository hotStudyRepository;

}
