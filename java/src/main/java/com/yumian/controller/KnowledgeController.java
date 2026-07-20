package com.yumian.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yumian.common.Result;
import com.yumian.entity.KnowledgeCategory;
import com.yumian.entity.KnowledgeImportLog;
import com.yumian.entity.KnowledgeQuestion;
import com.yumian.mapper.KnowledgeCategoryMapper;
import com.yumian.mapper.KnowledgeQuestionMapper;
import com.yumian.service.KnowledgeQuestionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/questions")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeQuestionService knowledgeQuestionService;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeQuestionMapper questionMapper;

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            throw new RuntimeException("无权限：仅管理员可操作题库");
        }
    }

    @GetMapping
    public Result<IPage<KnowledgeQuestion>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(knowledgeQuestionService.page(page, size, category, keyword));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeQuestion> getById(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(knowledgeQuestionService.getById(id));
    }

    @PostMapping
    public Result<KnowledgeQuestion> add(@RequestBody KnowledgeQuestion question,
                                         HttpServletRequest request) {
        checkAdmin(request);
        Long adminId = (Long) request.getAttribute("userId");
        return Result.success(knowledgeQuestionService.add(question, adminId));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeQuestion> update(@PathVariable Long id,
                                            @RequestBody KnowledgeQuestion question,
                                            HttpServletRequest request) {
        checkAdmin(request);
        question.setId(id);
        return Result.success(knowledgeQuestionService.update(question));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        knowledgeQuestionService.delete(id);
        return Result.success();
    }

    @PostMapping("/import")
    public Result<KnowledgeImportLog> importFile(@RequestParam("file") MultipartFile file,
                                                  HttpServletRequest request) {
        checkAdmin(request);
        Long adminId = (Long) request.getAttribute("userId");
        return Result.success(knowledgeQuestionService.importFile(file, adminId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(HttpServletRequest request) {
        checkAdmin(request);
        String json = knowledgeQuestionService.exportJson();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "knowledge_questions.json");
        return ResponseEntity.ok().headers(headers).body(json.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/import-logs")
    public Result<List<KnowledgeImportLog>> importLogs(HttpServletRequest request) {
        checkAdmin(request);
        Long adminId = (Long) request.getAttribute("userId");
        return Result.success(knowledgeQuestionService.getImportLogs(adminId));
    }

    // ====== 分类管理接口 ======

    @GetMapping("/categories")
    public Result<List<java.util.Map<String, Object>>> categories(HttpServletRequest request) {
        checkAdmin(request);
        List<KnowledgeCategory> list = categoryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCategory>().orderByAsc(KnowledgeCategory::getName));
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (KnowledgeCategory cat : list) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", cat.getId());
            item.put("name", cat.getName());
            item.put("createdAt", cat.getCreatedAt());
            long count = questionMapper.selectCount(
                    new LambdaQueryWrapper<KnowledgeQuestion>().eq(KnowledgeQuestion::getCategory, cat.getName()));
            item.put("questionCount", count);
            result.add(item);
        }
        return Result.success(result);
    }

    @PostMapping("/categories")
    public Result<KnowledgeCategory> addCategory(@RequestBody KnowledgeCategory category,
                                                  HttpServletRequest request) {
        checkAdmin(request);
        categoryMapper.insert(category);
        return Result.success(category);
    }

    @DeleteMapping("/categories/{id}")
    public Result<java.util.Map<String, Object>> deleteCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force,
            HttpServletRequest request) {
        checkAdmin(request);
        KnowledgeCategory cat = categoryMapper.selectById(id);
        if (cat == null) return Result.success();

        // 查询该分类下的题目数量
        long questionCount = questionMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeQuestion>().eq(KnowledgeQuestion::getCategory, cat.getName()));

        if (questionCount > 0 && !force) {
            // 第一次请求：返回警告，告诉用户有多少道题
            java.util.Map<String, Object> warning = new java.util.HashMap<>();
            warning.put("warning", true);
            warning.put("questionCount", questionCount);
            warning.put("categoryId", id);
            warning.put("message", "该分类下还有 " + questionCount + " 道题目，确定删除吗？删除后题目也会一并删除。");
            return Result.success(warning);
        }

        // 二次确认或没有题目：删除分类下的所有题目 + 分类
        if (questionCount > 0) {
            questionMapper.delete(
                    new LambdaQueryWrapper<KnowledgeQuestion>().eq(KnowledgeQuestion::getCategory, cat.getName()));
        }
        categoryMapper.deleteById(id);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("warning", false);
        result.put("deletedQuestions", questionCount);
        return Result.success(result);
    }
}
