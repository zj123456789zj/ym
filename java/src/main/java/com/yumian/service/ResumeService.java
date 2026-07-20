package com.yumian.service;

import com.yumian.entity.Resume;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ResumeService {
    Resume getResume(Long userId);

    void saveResume(Long userId, String rawContent);

    Map<String, Object> optimizeResume(Long userId);

    void uploadResume(Long userId, MultipartFile file);

    byte[] getPdfBytes(Long userId);
}
