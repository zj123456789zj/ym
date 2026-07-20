# 知识库 RAG 模拟面试系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 ym 模拟面试平台中实现基于知识库的 RAG 出题，支持 admin 管理题库、向量检索、直接出题/改编出题。

**Architecture:** 后端使用 Ollama embedding 模型将题目转为向量，存入 MySQL JSON 列；出题时通过余弦相似度检索 TOP-3 最相关题目，发送给 AI 生成问题。前端新增 admin 题库管理页面，支持表格管理和文件导入。

**Tech Stack:** Java 17 + Spring Boot 3.2 + MyBatis-Plus + Ollama (nomic-embed-text) + Vue 3 + Pinia + Element Plus + Apache POI

---

### Task 1: 数据库 SQL 迁移

**Files:**
- Create: `java/src/main/resources/sql/V20260710__knowledge_base.sql`

- [ ] **Step 1: 编写 SQL 迁移脚本**

```sql
-- 题库主表
CREATE TABLE IF NOT EXISTS knowledge_question (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    question         TEXT         NOT NULL COMMENT '题目内容',
    reference_answer TEXT         COMMENT '参考答案',
    category         VARCHAR(100) NOT NULL COMMENT '分类：Java/数据库/系统设计/项目/...',
    difficulty       TINYINT      DEFAULT 1 COMMENT '难度 1-5',
    tags             VARCHAR(500) COMMENT '标签，逗号分隔',
    embedding        JSON         COMMENT '语义向量，数字数组',
    status           TINYINT      DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_by       BIGINT       COMMENT '创建人（admin 用户 ID）',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库题库';

-- 导入记录表
CREATE TABLE IF NOT EXISTS knowledge_import_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name      VARCHAR(255) COMMENT '文件名',
    total_count    INT          DEFAULT 0 COMMENT '总条数',
    success_count  INT          DEFAULT 0 COMMENT '成功数',
    fail_count     INT          DEFAULT 0 COMMENT '失败数',
    created_by     BIGINT       COMMENT '操作人',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库导入记录';

-- user 表加 role 字段
ALTER TABLE user ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin/user' AFTER email;
```

- [ ] **Step 2: 执行 SQL**

运行 SQL 脚本创建表和修改 user 表。

- [ ] **Step 3: 提交**

```bash
git add java/src/main/resources/sql/V20260710__knowledge_base.sql
git commit -m "feat: add knowledge_question, knowledge_import_log tables and user.role column"
```

---

### Task 2: 后端实体类

**Files:**
- Create: `java/src/main/java/com/yumian/entity/KnowledgeQuestion.java`
- Create: `java/src/main/java/com/yumian/entity/KnowledgeImportLog.java`
- Modify: `java/src/main/java/com/yumian/entity/User.java`

- [ ] **Step 1: 创建 KnowledgeQuestion 实体**

```java
package com.yumian.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_question")
public class KnowledgeQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String question;
    private String referenceAnswer;
    private String category;
    private Integer difficulty;
    private String tags;
    private String embedding;   // JSON 字符串 "[0.1,0.2,...]"
    private Integer status;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 KnowledgeImportLog 实体**

```java
package com.yumian.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_import_log")
public class KnowledgeImportLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 修改 User.java，添加 role 字段**

```java
// 在第 16 行 phone 字段之后添加：
private String role;
```

- [ ] **Step 4: 提交**

```bash
git add java/src/main/java/com/yumian/entity/KnowledgeQuestion.java \
       java/src/main/java/com/yumian/entity/KnowledgeImportLog.java \
       java/src/main/java/com/yumian/entity/User.java
git commit -m "feat: add knowledge question entities and user role field"
```

---

### Task 3: 后端 Mapper + MyBatis-Plus 配置更新

**Files:**
- Create: `java/src/main/java/com/yumian/mapper/KnowledgeQuestionMapper.java`
- Create: `java/src/main/java/com/yumian/mapper/KnowledgeImportLogMapper.java`

- [ ] **Step 1: 创建 KnowledgeQuestionMapper**

```java
package com.yumian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yumian.entity.KnowledgeQuestion;

public interface KnowledgeQuestionMapper extends BaseMapper<KnowledgeQuestion> {
}
```

- [ ] **Step 2: 创建 KnowledgeImportLogMapper**

```java
package com.yumian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yumian.entity.KnowledgeImportLog;

public interface KnowledgeImportLogMapper extends BaseMapper<KnowledgeImportLog> {
}
```

- [ ] **Step 3: 提交**

```bash
git add java/src/main/java/com/yumian/mapper/KnowledgeQuestionMapper.java \
       java/src/main/java/com/yumian/mapper/KnowledgeImportLogMapper.java
git commit -m "feat: add mappers for knowledge question tables"
```

---

### Task 4: EmbeddingService — 接口 + Ollama 实现

**Files:**
- Create: `java/src/main/java/com/yumian/service/EmbeddingService.java`
- Create: `java/src/main/java/com/yumian/service/impl/OllamaEmbeddingServiceImpl.java`

- [ ] **Step 1: 创建 EmbeddingService 接口**

```java
package com.yumian.service;

public interface EmbeddingService {
    float[] embed(String text);
}
```

- [ ] **Step 2: 创建 OllamaEmbeddingServiceImpl 实现**

```java
package com.yumian.service.impl;

import com.yumian.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OllamaEmbeddingServiceImpl implements EmbeddingService {

    private final WebClient webClient;

    public OllamaEmbeddingServiceImpl(@Value("${ollama.url}") String ollamaUrl) {
        // 将 /api/chat 替换为 /api/embeddings 使用的 base URL
        String baseUrl = ollamaUrl.replace("/api/chat", "");
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public float[] embed(String text) {
        try {
            Map<String, Object> request = Map.of(
                    "model", "nomic-embed-text",
                    "prompt", text
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/api/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Embedding 返回为空");
            }

            @SuppressWarnings("unchecked")
            List<Double> embeddingList = (List<Double>) response.get("embedding");
            if (embeddingList == null || embeddingList.isEmpty()) {
                throw new RuntimeException("Embedding 内容为空");
            }

            float[] result = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                result[i] = embeddingList.get(i).floatValue();
            }
            return result;
        } catch (Exception e) {
            log.error("Embedding 调用失败: {}", e.getMessage());
            throw new RuntimeException("Embedding 调用失败: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 3: 通知用户拉取 embedding 模型**

用户需在终端执行：
```bash
ollama pull nomic-embed-text
```

- [ ] **Step 4: 提交**

```bash
git add java/src/main/java/com/yumian/service/EmbeddingService.java \
       java/src/main/java/com/yumian/service/impl/OllamaEmbeddingServiceImpl.java
git commit -m "feat: add EmbeddingService with Ollama nomic-embed-text"
```

---

### Task 5: 工具类 — 余弦相似度 + Embedding 转换

**Files:**
- Create: `java/src/main/java/com/yumian/utils/VectorUtils.java`

- [ ] **Step 1: 创建 VectorUtils**

```java
package com.yumian.utils;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import java.util.List;

public class VectorUtils {

    /** float[] → JSON 字符串（存入数据库） */
    public static String toJson(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /** JSON 字符串 → float[]（从数据库读取） */
    public static float[] fromJson(String json) {
        JSONArray arr = JSONUtil.parseArray(json);
        float[] result = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            result[i] = arr.getDouble(i).floatValue();
        }
        return result;
    }

    /** 余弦相似度 */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不匹配: " + a.length + " vs " + b.length);
        }
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dotProduct / denom;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add java/src/main/java/com/yumian/utils/VectorUtils.java
git commit -m "feat: add VectorUtils for embedding serialization and cosine similarity"
```

---

### Task 6: KnowledgeQuestionService — 题库 CRUD + 导入导出

**Files:**
- Create: `java/src/main/java/com/yumian/service/KnowledgeQuestionService.java`
- Create: `java/src/main/java/com/yumian/service/impl/KnowledgeQuestionServiceImpl.java`

- [ ] **Step 1: 添加 Apache POI 依赖到 pom.xml（支持 Excel 导入）**

```xml
<!-- 在 hutool-all 依赖后面添加 -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

- [ ] **Step 2: 创建 service 接口**

```java
package com.yumian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yumian.entity.KnowledgeImportLog;
import com.yumian.entity.KnowledgeQuestion;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeQuestionService {
    /** 分页查询 */
    IPage<KnowledgeQuestion> page(int pageNum, int pageSize, String category, String keyword);

    /** 根据 ID 查询 */
    KnowledgeQuestion getById(Long id);

    /** 新增（自动生成向量） */
    KnowledgeQuestion add(KnowledgeQuestion question, Long adminId);

    /** 编辑（重新生成向量） */
    KnowledgeQuestion update(KnowledgeQuestion question);

    /** 删除 */
    void delete(Long id);

    /** 批量导入（支持 .xlsx / .json） */
    KnowledgeImportLog importFile(MultipartFile file, Long adminId);

    /** 导出为 JSON */
    String exportJson();

    /** 获取所有启用的题目（含向量，用于 RAG 检索） */
    List<KnowledgeQuestion> getAllEnabled();

    /** 导入记录列表 */
    List<KnowledgeImportLog> getImportLogs(Long adminId);
}
```

- [ ] **Step 3: 创建 service 实现**

```java
package com.yumian.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yumian.entity.KnowledgeImportLog;
import com.yumian.entity.KnowledgeQuestion;
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
        // 生成向量
        String embedText = question.getQuestion() + " " + (question.getReferenceAnswer() != null ? question.getReferenceAnswer() : "");
        float[] vector = embeddingService.embed(embedText);
        question.setEmbedding(VectorUtils.toJson(vector));
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
        // 如果题目内容变了，重新生成向量
        boolean contentChanged = !existing.getQuestion().equals(question.getQuestion())
                || !java.util.Objects.equals(existing.getReferenceAnswer(), question.getReferenceAnswer());
        if (contentChanged) {
            String embedText = question.getQuestion() + " " + (question.getReferenceAnswer() != null ? question.getReferenceAnswer() : "");
            float[] vector = embeddingService.embed(embedText);
            question.setEmbedding(VectorUtils.toJson(vector));
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
                // 解析 JSON
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
                // 解析 Excel
                ExcelReader reader = ExcelUtil.getReader(is);
                List<List<Object>> rows = reader.read();
                boolean firstRow = true;
                for (List<Object> row : rows) {
                    if (firstRow) { firstRow = false; continue; } // 跳过表头
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

        int success = 0, fail = 0;
        for (KnowledgeQuestion q : list) {
            try {
                if (q.getQuestion() == null || q.getQuestion().isBlank()) {
                    fail++;
                    continue;
                }
                String embedText = q.getQuestion() + " " + (q.getReferenceAnswer() != null ? q.getReferenceAnswer() : "");
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

    @Override
    public List<KnowledgeQuestion> getAllEnabled() {
        return questionMapper.selectList(
                new LambdaQueryWrapper<KnowledgeQuestion>()
                        .eq(KnowledgeQuestion::getStatus, 1)
                        .isNotNull(KnowledgeQuestion::getEmbedding));
    }

    @Override
    public List<KnowledgeImportLog> getImportLogs(Long adminId) {
        return importLogMapper.selectList(
                new LambdaQueryWrapper<KnowledgeImportLog>()
                        .eq(adminId != null, KnowledgeImportLog::getCreatedBy, adminId)
                        .orderByDesc(KnowledgeImportLog::getCreatedAt));
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add java/src/main/java/com/yumian/service/KnowledgeQuestionService.java \
       java/src/main/java/com/yumian/service/impl/KnowledgeQuestionServiceImpl.java
git commit -m "feat: add KnowledgeQuestionService with CRUD, import/export, and embedding"
```

---

### Task 7: KnowledgeController — Admin 管理 API

**Files:**
- Create: `java/src/main/java/com/yumian/controller/KnowledgeController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.yumian.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yumian.common.Result;
import com.yumian.entity.KnowledgeImportLog;
import com.yumian.entity.KnowledgeQuestion;
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

    /** 校验当前用户是否为 admin */
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

    /** 获取所有分类（供前端筛选下拉） */
    @GetMapping("/categories")
    public Result<List<String>> categories(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(knowledgeQuestionService.getAllEnabled()
                .stream().map(KnowledgeQuestion::getCategory).distinct().sorted().toList());
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add java/src/main/java/com/yumian/controller/KnowledgeController.java
git commit -m "feat: add KnowledgeController for admin question management"
```

---

### Task 8: JWT 过滤器 — 注入用户 role

**Files:**
- Modify: `java/src/main/java/com/yumian/config/SecurityConfig.java`

修改 JWT 过滤器，从数据库加载用户角色并设为 request attribute。

- [ ] **Step 1: 修改 SecurityConfig.java**

注入 UserMapper，在 JWT 过滤器中查询用户 role：

```java
// 在类顶部的依赖中添加：
private final com.yumian.mapper.UserMapper userMapper;

// 在 doFilterInternal 中，userId 解析成功后添加：
if (userId != null) {
    request.setAttribute("userId", userId);
    // 加载用户 role
    com.yumian.entity.User user = userMapper.selectById(userId);
    if (user != null) {
        request.setAttribute("role", user.getRole() != null ? user.getRole() : "user");
    }
}
```

完整修改后的 `doFilterInternal` 关键部分：

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    String path = request.getRequestURI();
    boolean isPublic = path.equals("/api/user/login") || path.equals("/api/user/register")
            || path.startsWith("/api/captcha/");

    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
        try {
            String token = authHeader.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                request.setAttribute("userId", userId);
                // 加载用户角色
                User user = userMapper.selectById(userId);
                request.setAttribute("role", user != null && user.getRole() != null ? user.getRole() : "user");
            } else if (!isPublic) {
                writeUnauthorized(response);
                return;
            }
        } catch (Exception e) {
            log.warn("无效的 JWT token: {}", e.getMessage());
            if (!isPublic) {
                writeUnauthorized(response);
                return;
            }
        }
    } else if (!isPublic) {
        writeUnauthorized(response);
        return;
    }
    filterChain.doFilter(request, response);
}
```

注意：需在 SecurityConfig.java 顶部添加 import：
```java
import com.yumian.entity.User;
import com.yumian.mapper.UserMapper;
```

- [ ] **Step 2: 提交**

```bash
git add java/src/main/java/com/yumian/config/SecurityConfig.java
git commit -m "feat: inject user role into request attribute in JWT filter"
```

---

### Task 9: 修改 InterviewServiceImpl — RAG 出题逻辑

**Files:**
- Modify: `java/src/main/java/com/yumian/service/impl/InterviewServiceImpl.java`

- [ ] **Step 1: 注入 KnowledgeQuestionService 和 EmbeddingService**

```java
private final KnowledgeQuestionService knowledgeQuestionService;
private final EmbeddingService embeddingService;
```

- [ ] **Step 2: 在 askQuestion() 中添加 RAG 出题分支**

在 `session.getType() == 2` 的简历分支后面添加 type=3 的知识库分支：

```java
// 在 askQuestion() 方法中，替换原有逻辑为：

Long count = qaMapper.selectCount(
        new LambdaQueryWrapper<InterviewQa>().eq(InterviewQa::getSessionId, sessionId));

String question;
if (session.getType() == 3) {
    // RAG 知识库出题
    question = askRagQuestion(session, count);
} else if (session.getType() == 2) {
    // 原简历面试逻辑
    Resume resume = resumeMapper.selectOne(
            new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, session.getUserId()));
    String resumeContent = (resume != null && resume.getRawContent() != null)
            ? resume.getRawContent() : "暂无简历内容";
    question = aiService.chat(
            "你是一个技术面试官。请根据候选人的简历内容出面试题，每次只出一道题，考察技术深度和项目经验。只返回问题本身，不要多余内容。",
            "简历内容：" + resumeContent);
} else {
    // 原随机出题逻辑
    question = aiService.chat(
            "你是一个技术面试官。请出一道技术面试题(Java/数据库/项目/系统设计方向)，每次只出一道。只返回问题本身，不要多余内容。",
            "这是第" + (count + 1) + "道题");
}
```

- [ ] **Step 3: 添加 askRagQuestion 私有方法**

```java
/** RAG 知识库出题 */
private String askRagQuestion(InterviewSession session, Long currentCount) {
    // 获取该会话已问过的题目 ID
    List<InterviewQa> askedQaList = qaMapper.selectList(
            new LambdaQueryWrapper<InterviewQa>().eq(InterviewQa::getSessionId, session.getId()));
    List<Long> askedIds = askedQaList.stream().map(InterviewQa::getId).toList();

    // 获取所有启用的题目
    List<KnowledgeQuestion> allQuestions = knowledgeQuestionService.getAllEnabled();
    if (allQuestions.isEmpty()) {
        // 降级到随机出题
        return aiService.chat(
                "你是一个技术面试官。请出一道技术面试题(Java/数据库/项目/系统设计方向)，每次只出一道。只返回问题本身，不要多余内容。",
                "这是第" + (currentCount + 1) + "道题");
    }

    // 生成查询向量
    String queryText = "面试类型: 技术面试";
    float[] queryVector;
    try {
        queryVector = embeddingService.embed(queryText);
    } catch (Exception e) {
        // Embedding 失败，从知识库随机选一道
        List<KnowledgeQuestion> available = allQuestions.stream()
                .filter(q -> !askedIds.contains(q.getId()))
                .toList();
        if (available.isEmpty()) available = allQuestions;
        KnowledgeQuestion picked = available.get(new java.util.Random().nextInt(available.size()));
        return picked.getQuestion();
    }

    // 计算相似度，取 TOP-3（跳过已问过的）
    List<SimilarityResult> scored = new java.util.ArrayList<>();
    for (KnowledgeQuestion kq : allQuestions) {
        if (kq.getEmbedding() == null || kq.getEmbedding().isBlank()) continue;
        try {
            float[] emb = com.yumian.utils.VectorUtils.fromJson(kq.getEmbedding());
            double score = com.yumian.utils.VectorUtils.cosineSimilarity(queryVector, emb);
            scored.add(new SimilarityResult(kq, score));
        } catch (Exception ignored) {}
    }
    scored.sort((a, b) -> Double.compare(b.score, a.score));

    // 取 TOP-3（跳过已问的，如果全部已问则用全部 TOP-3）
    List<KnowledgeQuestion> top3 = new java.util.ArrayList<>();
    for (SimilarityResult sr : scored) {
        if (!askedIds.contains(sr.question.getId())) {
            top3.add(sr.question);
            if (top3.size() >= 3) break;
        }
    }
    if (top3.isEmpty() && !scored.isEmpty()) {
        // 全部已问过，随便取 TOP-3
        top3 = scored.subList(0, Math.min(3, scored.size()))
                .stream().map(sr -> sr.question).toList();
    }

    if (top3.isEmpty()) {
        // 没有可用的题目（知识库全空或全部 embedding 失败）
        return aiService.chat(
                "你是一个技术面试官。请出一道技术面试题(Java/数据库/项目/系统设计方向)，每次只出一道。只返回问题本身，不要多余内容。",
                "这是第" + (currentCount + 1) + "道题");
    }

    // 构建参考题文本
    StringBuilder refBuilder = new StringBuilder("以下是知识库中与当前面试相关的参考题：\n");
    for (int i = 0; i < top3.size(); i++) {
        KnowledgeQuestion kq = top3.get(i);
        refBuilder.append("参考题").append(i + 1).append(": ")
                .append(kq.getQuestion());
        if (kq.getReferenceAnswer() != null && !kq.getReferenceAnswer().isBlank()) {
            refBuilder.append(" (答案: ").append(kq.getReferenceAnswer()).append(")");
        }
        refBuilder.append("\n");
    }
    refBuilder.append("\n请出一道面试题。你可以：\n");
    refBuilder.append("1. 直接使用其中一道原题\n");
    refBuilder.append("2. 改编其中一道题（换场景、换参数、换问法）\n");
    refBuilder.append("只返回问题本身，不要多余内容。");

    return aiService.chat("你是一个技术面试官。", refBuilder.toString());
}

/** 相似度内部记录类 */
@lombok.AllArgsConstructor
private static class SimilarityResult {
    final KnowledgeQuestion question;
    final double score;
}
```

- [ ] **Step 4: 提交**

```bash
git add java/src/main/java/com/yumian/service/impl/InterviewServiceImpl.java
git commit -m "feat: add RAG-based question selection for type=3 interview"
```

---

### Task 10: 前端 API 层

**Files:**
- Create: `front/src/api/questionBank.js`

- [ ] **Step 1: 创建 questionBank.js**

```javascript
import request from './request'

export const getQuestionList = (params) => request.get('/admin/questions', { params })
export const getQuestionById = (id) => request.get(`/admin/questions/${id}`)
export const addQuestion = (data) => request.post('/admin/questions', data)
export const updateQuestion = (id, data) => request.put(`/admin/questions/${id}`, data)
export const deleteQuestion = (id) => request.delete(`/admin/questions/${id}`)
export const importQuestions = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/questions/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const exportQuestions = () => request.get('/admin/questions/export', { responseType: 'blob' })
export const getImportLogs = () => request.get('/admin/questions/import-logs')
export const getCategories = () => request.get('/admin/questions/categories')
```

- [ ] **Step 2: 提交**

```bash
git add front/src/api/questionBank.js
git commit -m "feat: add question bank API layer"
```

---

### Task 11: 前端 Store — 用户 role

**Files:**
- Modify: `front/src/stores/user.js`

- [ ] **Step 1: 添加 role getter**

```javascript
// 在 getters 中添加：
isAdmin: (state) => state.userInfo?.role === 'admin'
```

完整 getters 部分：
```javascript
getters: {
  isLoggedIn: (state) => !!state.token,
  isAdmin: (state) => state.userInfo?.role === 'admin'
},
```

- [ ] **Step 2: 提交**

```bash
git add front/src/stores/user.js
git commit -m "feat: add isAdmin getter to user store"
```

---

### Task 12: 前端路由 — admin 路由守卫

**Files:**
- Modify: `front/src/router/index.js`

- [ ] **Step 1: 添加 admin 路由**

```javascript
// 在 children 数组中添加：
{ path: '/admin/questions', name: 'QuestionBank', component: () => import('../views/admin/QuestionList.vue') },
```

- [ ] **Step 2: 在 beforeEach 中添加 role 守卫**

```javascript
// 在现有 beforeEach 中，token 检查之后添加 admin 路由检查：
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path.startsWith('/admin')) {
    // admin 路由需要检查用户角色
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.role === 'admin') {
          next()
        } else {
          next('/overview')
        }
      } catch {
        next('/overview')
      }
    } else {
      next('/overview')
    }
  } else {
    next()
  }
})
```

注意：userInfo 已经在登录时存入 Pinia store，也需要存入 localStorage 以便路由守卫读取。需在 `stores/user.js` 的 `login` action 中添加：
```javascript
localStorage.setItem('userInfo', JSON.stringify(res.data.user))
```

并在 `logout` action 中添加：
```javascript
localStorage.removeItem('userInfo')
```

- [ ] **Step 3: 提交**

```bash
git add front/src/router/index.js front/src/stores/user.js
git commit -m "feat: add admin route guard and persist userInfo to localStorage"
```

---

### Task 13: 前端 Admin 题库页面 — QuestionList.vue

**Files:**
- Create: `front/src/views/admin/QuestionList.vue`

这个页面是最大的前端改动，包含三个视图：题目列表、新增/编辑弹窗、导入弹窗。

- [ ] **Step 1: 创建 QuestionList.vue**

```vue
<template>
  <div class="question-bank">
    <div class="page-header">
      <h2>题库管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showImport = true">导入题目</el-button>
        <el-button @click="handleExport">导出题目</el-button>
        <el-button type="success" @click="openAdd">新增题目</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="filterCategory" placeholder="全部分类" clearable @change="fetchData">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索题目..." clearable @clear="fetchData"
                @keyup.enter="fetchData" style="width: 280px" />
      <el-button @click="fetchData">搜索</el-button>
    </div>

    <!-- 题目表格 -->
    <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="question" label="题目" min-width="300" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="difficulty" label="难度" width="80">
        <template #default="scope">
          <el-rate v-model="scope.row.difficulty" disabled text-color="#ff9900" score-template="" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="small" @click="toggleStatus(scope.row)">
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(scope.row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showEdit" :title="editingId ? '编辑题目' : '新增题目'" width="700px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="题目" required>
          <el-input v-model="editForm.question" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="editForm.referenceAnswer" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-input v-model="editForm.category" placeholder="如：Java、数据库、系统设计" />
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="editForm.difficulty" :max="5" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editForm.tags" placeholder="逗号分隔，如：线程,并发" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog v-model="showImport" title="导入题目" width="500px">
      <div class="import-hint">
        <p>支持 .json 或 .xlsx / .xls 文件。</p>
        <p>JSON 格式：<code>[{"question":"...","referenceAnswer":"...","category":"Java","difficulty":3,"tags":"线程"}]</code></p>
        <p>Excel 格式：表头为 question / reference_answer / category / difficulty / tags</p>
      </div>
      <el-upload
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept=".json,.xlsx,.xls"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
      </el-upload>
      <div v-if="importResult" class="import-result">
        <p>总条数：{{ importResult.totalCount }}</p>
        <p>成功：{{ importResult.successCount }}</p>
        <p>失败：{{ importResult.failCount }}</p>
      </div>
      <template #footer>
        <el-button @click="showImport = false; importResult = null">关闭</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing" :disabled="!selectedFile">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  getQuestionList, addQuestion, updateQuestion, deleteQuestion,
  importQuestions, exportQuestions, getCategories
} from '../../api/questionBank'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filterCategory = ref('')
const keyword = ref('')
const categories = ref([])

// 编辑弹窗
const showEdit = ref(false)
const editingId = ref(null)
const saving = ref(false)
const editForm = ref({
  question: '',
  referenceAnswer: '',
  category: '',
  difficulty: 3,
  tags: ''
})

// 导入弹窗
const showImport = ref(false)
const selectedFile = ref(null)
const importing = ref(false)
const importResult = ref(null)

onMounted(() => {
  fetchData()
  loadCategories()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getQuestionList({
      page: currentPage.value,
      size: pageSize.value,
      category: filterCategory.value || undefined,
      keyword: keyword.value || undefined
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    ElMessage.error('加载题库失败')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data
  } catch {}
}

function openAdd() {
  editingId.value = null
  editForm.value = { question: '', referenceAnswer: '', category: '', difficulty: 3, tags: '' }
  showEdit.value = true
}

function openEdit(row) {
  editingId.value = row.id
  editForm.value = {
    question: row.question,
    referenceAnswer: row.referenceAnswer || '',
    category: row.category,
    difficulty: row.difficulty || 3,
    tags: row.tags || ''
  }
  showEdit.value = true
}

async function saveQuestion() {
  if (!editForm.value.question || !editForm.value.category) {
    ElMessage.warning('请填写题目和分类')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateQuestion(editingId.value, editForm.value)
      ElMessage.success('已更新')
    } else {
      await addQuestion(editForm.value)
      ElMessage.success('已添加')
    }
    showEdit.value = false
    fetchData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteQuestion(id)
    ElMessage.success('已删除')
    fetchData()
  } catch {
    ElMessage.error('删除失败')
  }
}

async function toggleStatus(row) {
  try {
    await updateQuestion(row.id, { ...row, status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    fetchData()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleExport() {
  try {
    const res = await exportQuestions()
    const blob = new Blob([res], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'knowledge_questions.json'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

function handleFileChange(file) {
  selectedFile.value = file.raw
}

async function handleImport() {
  if (!selectedFile.value) return
  importing.value = true
  importResult.value = null
  try {
    const res = await importQuestions(selectedFile.value)
    importResult.value = res.data
    ElMessage.success(`导入完成：成功 ${res.data.successCount} 条，失败 ${res.data.failCount} 条`)
    fetchData()
  } catch {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.question-bank { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; }
.header-actions { display: flex; gap: 10px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }
.import-hint { background: #f5f7fa; padding: 12px 16px; border-radius: 6px; margin-bottom: 16px; font-size: 13px; }
.import-hint code { background: #e8ecf1; padding: 2px 6px; border-radius: 3px; font-size: 12px; }
.import-result { margin-top: 16px; padding: 12px; background: #f0f9eb; border-radius: 6px; }
.import-result p { margin: 4px 0; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add front/src/views/admin/QuestionList.vue
git commit -m "feat: add admin question bank management page with CRUD and import/export"
```

---

### Task 14: 前端侧边栏 — admin 菜单入口

**Files:**
- Modify: `front/src/layout/Sidebar.vue`
- Modify: `front/src/layout/MainLayout.vue`

- [ ] **Step 1: 修改 Sidebar.vue，根据角色显示 admin 菜单**

```vue
<script setup>
import { computed } from 'vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

const navItems = computed(() => {
  const items = [
    { path: '/overview', icon: '📊', label: '总览' },
    { path: '/resume', icon: '📄', label: '我的简历' },
    { path: '/resume/optimize', icon: '✨', label: '简历优化' },
    { path: '/interview/mock', icon: '🎯', label: '模拟面试' },
    { path: '/interview/special', icon: '⚡', label: '专项训练' },
    { path: '/interview/history', icon: '📝', label: '面试历史' },
    { path: '/questions', icon: '❌', label: '错题本' },
    { path: '/statistics', icon: '📈', label: '数据统计' },
    { path: '/profile', icon: '👤', label: '个人中心' },
  ]
  if (userStore.isAdmin) {
    items.push({ path: '/admin/questions', icon: '📚', label: '题库管理' })
  }
  return items
})
</script>
```

- [ ] **Step 2: 确保 MainLayout.vue 在页面刷新后加载 userInfo**

当前 MainLayout.vue 已有 fetchProfile 调用，但路由守卫在页面刷新时可能先于 store 加载执行。需要确保 userInfo 持久化。

在 `front/src/stores/user.js` 的 `login` action 末尾添加：
```javascript
localStorage.setItem('userInfo', JSON.stringify(res.data.user))
```

在 `fetchProfile` action 中，获取到 userInfo 后也要持久化：
```javascript
async fetchProfile() {
  const res = await getProfile()
  this.userInfo = res.data
  localStorage.setItem('userInfo', JSON.stringify(res.data))
},
```

在 `logout` action 中添加：
```javascript
localStorage.removeItem('userInfo')
```

并在 store 初始化时从 localStorage 恢复：
```javascript
state: () => ({
  token: localStorage.getItem('token') || '',
  userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
}),
```

- [ ] **Step 3: 提交**

```bash
git add front/src/layout/Sidebar.vue front/src/stores/user.js
git commit -m "feat: add admin sidebar menu item and persist userInfo"
```

---

### Task 15: 初始化 admin 账号 + 验证

**Files:**
- 无新建文件，直接执行 SQL

- [ ] **Step 1: 手动创建 admin 账号**

```sql
-- 密码 123456 的 BCrypt 编码
INSERT INTO user (username, password, nickname, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'admin');
```

使用项目启动后通过注册接口创建一个用户，然后手动执行：
```sql
UPDATE user SET role = 'admin' WHERE username = 'admin';
```

或者直接用项目中的 PasswordEncoder 生成加密密码。

- [ ] **Step 2: 端到端验证**

1. `ollama pull nomic-embed-text` 拉取 embedding 模型
2. 重启后端 + 前端
3. 用 admin 账号登录，确认侧边栏出现"题库管理"
4. 进入题库管理页，新增 3-5 道题
5. 导入一个 JSON 文件
6. 创建 type=3 的面试会话，确认 AI 从知识库出题
7. 多次出题，确认有时原题有时改编题
8. 用普通用户登录，确认看不到题库管理

- [ ] **Step 3: 最终提交**

```bash
git commit -m "chore: add admin init SQL and verify end-to-end"
```
