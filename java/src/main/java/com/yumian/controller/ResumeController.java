package com.yumian.controller;

import com.yumian.common.Result;
import com.yumian.dto.ResumeSaveRequest;
import com.yumian.entity.Resume;
import com.yumian.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeService resumeService;

    @GetMapping
    public Result<Map<String, Object>> getResume(@RequestAttribute("userId") Long userId) {
        Resume resume = resumeService.getResume(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", resume.getId());
        result.put("userId", resume.getUserId());
        result.put("rawContent", resume.getRawContent());
        result.put("parsedContent", resume.getParsedContent());
        result.put("pdfPath", resume.getPdfPath());
        result.put("hasPdf", resume.getPdfPath() != null);
        result.put("createdAt", resume.getCreatedAt());
        result.put("updatedAt", resume.getUpdatedAt());
        return Result.success(result);
    }

    @PostMapping
    public Result<Void> saveResume(@RequestAttribute("userId") Long userId,
                                   @RequestBody ResumeSaveRequest request) {
        resumeService.saveResume(userId, request.getRawContent());
        return Result.success();
    }

    @PostMapping("/upload")
    public Result<Void> uploadResume(@RequestAttribute("userId") Long userId,
                                     @RequestParam("file") MultipartFile file) {
        resumeService.uploadResume(userId, file);
        return Result.success();
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> getPdf(@RequestAttribute("userId") Long userId) {
        byte[] pdfBytes = resumeService.getPdfBytes(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/optimize")
    public Result<Map<String, Object>> optimize(@RequestAttribute("userId") Long userId) {
        return Result.success(resumeService.optimizeResume(userId));
    }
}
