package com.narek.jobportal.service;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {
    String storeResume(MultipartFile file);

    String parseResumeSummary(MultipartFile file);
}
