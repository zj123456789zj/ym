package com.yumian.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yumian.entity.KnowledgeCategory;
import com.yumian.entity.KnowledgeImportLog;
import com.yumian.entity.KnowledgeQuestion;
import com.yumian.mapper.KnowledgeCategoryMapper;
import com.yumian.mapper.KnowledgeImportLogMapper;
import com.yumian.mapper.KnowledgeQuestionMapper;
import com.yumian.service.EmbeddingService;
import com.yumian.service.KnowledgeQuestionService;
import com.yumian.utils.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeQuestionServiceImpl implements KnowledgeQuestionService {

    private final KnowledgeQuestionMapper questionMapper;
    private final KnowledgeImportLogMapper importLogMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final EmbeddingService embeddingService;

    @Override
    public IPage<KnowledgeQuestion> page(int pageNum, int pageSize, String category, String keyword) {
        LambdaQueryWrapper<KnowledgeQuestion> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(KnowledgeQuestion::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(KnowledgeQuestion::getQuestion, keyword);
        }
        wrapper.orderByDesc(KnowledgeQuestion::getCreatedAt);
        return questionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public KnowledgeQuestion getById(Long id) {
        return questionMapper.selectById(id);
    }

    @Override
    @Transactional
    public KnowledgeQuestion add(KnowledgeQuestion question, Long adminId) {
        question.setCreatedBy(adminId);
        question.setStatus(1);
        String embedText = question.getQuestion() + " " +
                (question.getReferenceAnswer() != null ? question.getReferenceAnswer() : "");
        try {
            float[] vector = embeddingService.embed(embedText);
            question.setEmbedding(VectorUtils.toJson(vector));
        } catch (Exception e) {
            log.warn("Embedding 生成失败，跳过: {}", e.getMessage());
        }
        questionMapper.insert(question);
        return question;
    }

    @Override
    @Transactional
    public KnowledgeQuestion update(KnowledgeQuestion question) {
        KnowledgeQuestion existing = questionMapper.selectById(question.getId());
        if (existing == null) {
            throw new RuntimeException("题目不存在");
        }
        boolean contentChanged = !existing.getQuestion().equals(question.getQuestion())
                || !java.util.Objects.equals(existing.getReferenceAnswer(), question.getReferenceAnswer());
        if (contentChanged) {
            String embedText = question.getQuestion() + " " +
                    (question.getReferenceAnswer() != null ? question.getReferenceAnswer() : "");
            try {
                float[] vector = embeddingService.embed(embedText);
                question.setEmbedding(VectorUtils.toJson(vector));
            } catch (Exception e) {
                log.warn("Embedding 重新生成失败，保留原向量: {}", e.getMessage());
                question.setEmbedding(existing.getEmbedding());
            }
        } else {
            question.setEmbedding(existing.getEmbedding());
        }
        questionMapper.updateById(question);
        return questionMapper.selectById(question.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        questionMapper.deleteById(id);
    }

    @Override
    @Transactional
    public KnowledgeImportLog importFile(MultipartFile file, Long adminId) {
        String fileName = file.getOriginalFilename();
        List<KnowledgeQuestion> list = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {
            String name = fileName != null ? fileName.toLowerCase() : "";
            if (name.endsWith(".json")) {
                String content = new String(is.readAllBytes(), "UTF-8");
                JSONArray arr = JSONUtil.parseArray(content);
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    KnowledgeQuestion q = new KnowledgeQuestion();
                    q.setQuestion(obj.getStr("question"));
                    q.setReferenceAnswer(obj.getStr("referenceAnswer"));
                    q.setCategory(obj.getStr("category"));
                    q.setDifficulty(obj.getInt("difficulty", 1));
                    q.setTags(obj.getStr("tags"));
                    q.setCreatedBy(adminId);
                    q.setStatus(1);
                    list.add(q);
                }
            } else if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                ExcelReader reader = ExcelUtil.getReader(is);
                List<List<Object>> rows = reader.read();
                boolean firstRow = true;
                for (List<Object> row : rows) {
                    if (firstRow) { firstRow = false; continue; }
                    if (row.isEmpty() || row.get(0) == null) continue;
                    KnowledgeQuestion q = new KnowledgeQuestion();
                    q.setQuestion(row.get(0) != null ? row.get(0).toString() : null);
                    q.setReferenceAnswer(row.size() > 1 && row.get(1) != null ? row.get(1).toString() : null);
                    q.setCategory(row.size() > 2 && row.get(2) != null ? row.get(2).toString() : null);
                    q.setDifficulty(row.size() > 3 && row.get(3) != null ? Integer.parseInt(row.get(3).toString()) : 1);
                    q.setTags(row.size() > 4 && row.get(4) != null ? row.get(4).toString() : null);
                    q.setCreatedBy(adminId);
                    q.setStatus(1);
                    list.add(q);
                }
            } else {
                throw new RuntimeException("不支持的文件格式，请上传 .json / .xlsx / .xls");
            }
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败: " + e.getMessage());
        }

        // 自动添加新分类
        list.stream().map(KnowledgeQuestion::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .forEach(this::ensureCategoryInDb);

        int success = 0, fail = 0;
        for (KnowledgeQuestion q : list) {
            try {
                if (q.getQuestion() == null || q.getQuestion().isBlank()) {
                    fail++;
                    continue;
                }
                String embedText = q.getQuestion() + " " +
                        (q.getReferenceAnswer() != null ? q.getReferenceAnswer() : "");
                float[] vector = embeddingService.embed(embedText);
                q.setEmbedding(VectorUtils.toJson(vector));
                questionMapper.insert(q);
                success++;
            } catch (Exception e) {
                log.warn("导入失败: {}", e.getMessage());
                fail++;
            }
        }

        KnowledgeImportLog logEntry = new KnowledgeImportLog();
        logEntry.setFileName(fileName);
        logEntry.setTotalCount(list.size());
        logEntry.setSuccessCount(success);
        logEntry.setFailCount(fail);
        logEntry.setCreatedBy(adminId);
        importLogMapper.insert(logEntry);

        return logEntry;
    }

    @Override
    public String exportJson() {
        List<KnowledgeQuestion> list = questionMapper.selectList(
                new LambdaQueryWrapper<KnowledgeQuestion>().orderByDesc(KnowledgeQuestion::getCreatedAt));
        JSONArray arr = new JSONArray();
        for (KnowledgeQuestion q : list) {
            JSONObject obj = new JSONObject();
            obj.set("id", q.getId());
            obj.set("question", q.getQuestion());
            obj.set("referenceAnswer", q.getReferenceAnswer());
            obj.set("category", q.getCategory());
            obj.set("difficulty", q.getDifficulty());
            obj.set("tags", q.getTags());
            arr.add(obj);
        }
        return arr.toJSONString(0);
    }

    /** 确保分类存在于 knowledge_category 表中 */
    private void ensureCategoryInDb(String categoryName) {
        Long exists = categoryMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeCategory>().eq(KnowledgeCategory::getName, categoryName));
        if (exists == 0) {
            KnowledgeCategory cat = new KnowledgeCategory();
            cat.setName(categoryName);
            categoryMapper.insert(cat);
        }
    }

    @Override
    public List<KnowledgeQuestion> getAllEnabled() {
        return questionMapper.selectList(
                new LambdaQueryWrapper<KnowledgeQuestion>()
                        .eq(KnowledgeQuestion::getStatus, 1)
                        .isNotNull(KnowledgeQuestion::getEmbedding));
    }

    @Override
    public KnowledgeQuestion getRandomEnabled(List<Long> excludeIds) {
        LambdaQueryWrapper<KnowledgeQuestion> wrapper = new LambdaQueryWrapper<KnowledgeQuestion>()
                .eq(KnowledgeQuestion::getStatus, 1)
                .isNotNull(KnowledgeQuestion::getEmbedding);
        if (excludeIds != null && !excludeIds.isEmpty()) {
            wrapper.notIn(KnowledgeQuestion::getId, excludeIds);
        }
        List<KnowledgeQuestion> list = questionMapper.selectList(wrapper);
        if (list.isEmpty()) return null;
        return list.get(new java.util.Random().nextInt(list.size()));
    }

    @Override
    public List<KnowledgeImportLog> getImportLogs(Long adminId) {
        return importLogMapper.selectList(
                new LambdaQueryWrapper<KnowledgeImportLog>()
                        .eq(adminId != null, KnowledgeImportLog::getCreatedBy, adminId)
                        .orderByDesc(KnowledgeImportLog::getCreatedAt));
    }
}
