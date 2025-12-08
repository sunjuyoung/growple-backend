package com.grow.study.application.required;

import org.springframework.web.multipart.MultipartFile;

public interface S3FileUpload {

     String uploadImage(MultipartFile file, String path);
}
