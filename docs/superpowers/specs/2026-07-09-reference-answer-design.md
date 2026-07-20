# 模拟面试参考答案功能设计

## 背景

用户在模拟面试中提交回答后，目前只显示 AI 评价和评分（硬编码 75 分），但缺少正确答案或参考回答，无法对照学习。需要在提交回答后同时展示参考答案。

## 改动范围

### 1. 数据库

`interview_qa` 表新增一列：

```sql
ALTER TABLE interview_qa ADD COLUMN reference_answer TEXT COMMENT '参考答案（由 AI 生成）' AFTER ai_evaluation;
```

### 2. 实体类

`InterviewQa.java` 新增字段：

```java
private String referenceAnswer;
```

### 3. 后端逻辑 — `InterviewServiceImpl.submitAnswer()`

**错误处理：** 如果 AI 返回的 JSON 解析失败，降级为旧行为 — 整段响应作为评价内容，score 仍设 75，referenceAnswer 为空。

**修改 AI 提示词**，让一次调用同时返回评分、评价、参考答案，格式为 JSON：

```
你是一个面试评分专家。对于以下面试题目和回答，请严格按 JSON 格式返回：
{"score": 数字, "evaluation": "评价内容", "referenceAnswer": "参考答案"}

题目：...
回答：...
```

返回后，用项目已有的 Hutool JSON 工具解析，分别设置：

```java
qa.setScore(score);            // 修复硬编码 75 的问题
qa.setAiEvaluation(evaluation);
qa.setReferenceAnswer(referenceAnswer);
```

### 4. 前端 — `MockInterview.vue`

在提交回答后的结果卡片中，"你的回答"和"AI 评价"之间插入参考答案区块：

```html
<div v-if="store.currentQa.referenceAnswer" class="qa-reference">
  <div class="label">参考答案：</div>
  <p>{{ store.currentQa.referenceAnswer }}</p>
</div>
```

样式与"你的回答"卡片一致，边框色用绿色/teal 区分。

## 影响文件

| 文件 | 改动类型 |
|------|---------|
| `entity/InterviewQa.java` | 新增 `referenceAnswer` 字段 |
| `service/impl/InterviewServiceImpl.java` | 修改 `submitAnswer()` 提示词 + 解析 JSON |
| `views/interview/MockInterview.vue` | 新增参考答案展示区块 + 样式 |
| 数据库 | 手动执行 ALTER TABLE |

## 不受影响

- `ReviewServiceImpl`、`ResumeServiceImpl` — 不涉及提交答案
- 其他前端页面 — 不涉及面试答题
- AiService 接口 — 无需修改
- 路由、Store、API 请求层 — 无需修改

## 验证方式

1. 手动执行 `ALTER TABLE` 添加列
2. 启动后端，进入模拟面试
3. 回答一道题，确认显示：你的回答 → 参考答案 → AI 评价
4. 检查数据库 `reference_answer` 字段有内容
5. 切换回 DeepSeek 模式验证同样正常工作
