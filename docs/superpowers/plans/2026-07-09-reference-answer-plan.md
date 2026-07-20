# 参考答案功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在模拟面试提交回答后，同时展示 AI 给出的参考答案

**Architecture:** 修改 `submitAnswer()` 的 AI 提示词，让一次调用返回 JSON（score + evaluation + referenceAnswer），用 Hutool 解析后存入数据库，前端新增展示区块

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, Vue 3, Element Plus, Hutool

---

### Task 1: 数据库迁移

- [ ] **Step 1: 执行 ALTER TABLE**

在 MySQL 中执行：

```sql
ALTER TABLE interview_qa ADD COLUMN reference_answer TEXT COMMENT '参考答案（由 AI 生成）' AFTER ai_evaluation;
```

验证：`DESC interview_qa;` 确认多出 `reference_answer` 列

---

### Task 2: 实体类新增字段

**Files:** Modify: `entity/InterviewQa.java:16`（在 `private Integer score;` 之后）

- [ ] **Step 1: 添加 referenceAnswer 字段**

```java
private String referenceAnswer;
```

放在 `private Integer score;` 之后，`private Integer sortOrder;` 之前。

---

### Task 3: 修改 submitAnswer 后端逻辑

**Files:** Modify: `service/impl/InterviewServiceImpl.java:80-96`

- [ ] **Step 1: 添加 Hutool JSON 导入**

在文件顶部现有 import 之后添加：
```java
import cn.hutool.json.JSONObject;
```

- [ ] **Step 2: 重写 submitAnswer 方法**

将 `submitAnswer()` 方法内容替换为：

```java
    @Override
    @Transactional
    public InterviewQa submitAnswer(Long qaId, String answer) {
        InterviewQa qa = qaMapper.selectById(qaId);
        if (qa == null) {
            throw new BusinessException("题目不存在");
        }
        qa.setUserAnswer(answer);

        String response = aiService.chat(
                "你是一个面试评分专家。对于以下面试题目和回答，请严格按 JSON 格式返回："
                + "{\"score\": 数字, \"evaluation\": \"评价内容\", \"referenceAnswer\": \"参考答案\"}"
                + " 注意：只返回 JSON，不要加多余说明。",
                "题目：" + qa.getQuestion() + "\n回答：" + answer);

        try {
            JSONObject json = new JSONObject(response);
            qa.setScore(json.getInt("score"));
            qa.setAiEvaluation(json.getStr("evaluation"));
            qa.setReferenceAnswer(json.getStr("referenceAnswer"));
        } catch (Exception e) {
            // JSON 解析失败，降级为旧行为
            qa.setAiEvaluation(response);
            qa.setScore(75);
        }

        qaMapper.updateById(qa);
        return qa;
    }
```

---

### Task 4: 前端展示参考答案

**Files:** Modify: `views/interview/MockInterview.vue:45-58`

- [ ] **Step 1: 在"你的回答"和"AI 评价"之间插入参考答案区块**

在 `<div class="qa-answer">` 块结束之后、`<div class="qa-evaluation">` 块之前插入：

```html
          <div v-if="store.currentQa.referenceAnswer" class="qa-reference">
            <div class="label">参考答案：</div>
            <p>{{ store.currentQa.referenceAnswer }}</p>
          </div>
```

- [ ] **Step 2: 添加参考答案的 CSS 样式**

在 `<style scoped>` 中 `.qa-answer, .qa-evaluation` 规则之后添加：

```css
.qa-reference {
  margin-bottom: 16px; padding: 12px;
  background: rgba(0,0,0,0.02); border-radius: 8px;
  border-left: 3px solid #10b981;
}
```

---

### Task 5: 验证

- [ ] **Step 1: 重启后端服务**
- [ ] **Step 2: 进入模拟面试，回答一道题**
- [ ] **Step 3: 确认页面依次显示：你的回答 → 参考答案 → AI 评价**
- [ ] **Step 4: 检查数据库中该条 QA 记录的 `reference_answer` 字段有内容**
- [ ] **Step 5: 修改 application.yml 切换 ai.provider=deepseek，重启验证同样正常工作**
