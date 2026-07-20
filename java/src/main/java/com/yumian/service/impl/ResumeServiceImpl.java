package com.yumian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yumian.common.exception.BusinessException;
import com.yumian.entity.Resume;
import com.yumian.entity.ResumeOptimization;
import com.yumian.mapper.ResumeMapper;
import com.yumian.mapper.ResumeOptimizationMapper;
import com.yumian.service.ResumeService;
import com.yumian.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final ResumeMapper resumeMapper;
    private final ResumeOptimizationMapper resumeOptimizationMapper;
    private final AiService aiService;

    private final String RESUME_DIR = System.getProperty("user.dir") + File.separator + "userdata" + File.separator + "resumes";

    @Override
    public Resume getResume(Long userId) {
        Resume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, userId));
        if (resume == null) {
            resume = new Resume();
            resume.setUserId(userId);
            resumeMapper.insert(resume);
        }
        return resume;
    }

    @Override
    @Transactional
    public void saveResume(Long userId, String rawContent) {
        Resume resume = getResume(userId);
        resume.setRawContent(rawContent);
        resumeMapper.updateById(resume);
    }

    @Override
    public Map<String, Object> optimizeResume(Long userId) {
        Resume resume = getResume(userId);
        if (resume.getRawContent() == null || resume.getRawContent().isBlank()) {
            throw new BusinessException("请先填写简历内容");
        }

        String systemPrompt = "你是一个专业的简历优化顾问。请分析以下简历：1. 给出逐条修改建议 2. 给出评分(百分制) 3. 输出优化后的完整简历。以JSON格式返回，包含 suggestions 数组、score 数字、optimizedContent 字符串。";
        String response = aiService.chat(systemPrompt, resume.getRawContent());

        ResumeOptimization optimization = new ResumeOptimization();
        optimization.setUserId(userId);
        optimization.setResumeId(resume.getId());
        optimization.setOptimizedContent(response);
        optimization.setSuggestions(response);
        optimization.setScore(85);
        resumeOptimizationMapper.insert(optimization);

        Map<String, Object> result = new HashMap<>();
        result.put("suggestions", response);
        result.put("score", 85);
        result.put("optimizedContent", response);
        return result;
    }

    @Override
    @Transactional
    public void uploadResume(Long userId, MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        if (contentType == null || !contentType.equals("application/pdf")) {
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                throw new BusinessException("仅支持 PDF 文件");
            }
        }

        File dir = new File(RESUME_DIR + File.separator + userId);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("无法创建上传目录");
        }

        File target = new File(dir, "resume.pdf");
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        Resume resume = getResume(userId);
        resume.setPdfPath(userId + "/resume.pdf");
        resumeMapper.updateById(resume);
    }

    @Override
    public byte[] getPdfBytes(Long userId) {
        Resume resume = getResume(userId);
        if (resume.getPdfPath() == null) {
            throw new BusinessException("尚未上传简历");
        }
        try {
            Path filePath = Paths.get(RESUME_DIR, resume.getPdfPath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new BusinessException("简历文件读取失败");
        }
    }
}
