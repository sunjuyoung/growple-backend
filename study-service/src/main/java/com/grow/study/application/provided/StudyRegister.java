package com.grow.study.application.provided;

import com.grow.study.domain.study.dto.StudyRegisterRequest;
import org.springframework.web.multipart.MultipartFile;

public interface StudyRegister {

    StudyRegisterResponse register(StudyRegisterRequest request, MultipartFile thumbnail, Long leaderId);

}
