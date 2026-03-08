package com.narek.jobportal.service.impl;

import com.narek.jobportal.service.ResumeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class ResumeServiceImpl implements ResumeService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    private final Path uploadDir;

    public ResumeServiceImpl(@Value("${app.resume.upload-dir:uploads/resumes}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String storeResume(MultipartFile file) {
        validateFile(file);
        try {
            Files.createDirectories(uploadDir);
            String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename());
            String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
            String storedName = UUID.randomUUID() + extension;
            Path target = uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store resume", ex);
        }
    }

    @Override
    public String parseResumeSummary(MultipartFile file) {
        String base = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename();
        return "Parsed resume metadata from " + base + " (basic parsing placeholder: name/email/skills extraction hook).";
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF or DOC/DOCX resumes are supported");
        }
    }
}
