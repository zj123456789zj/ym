# 专项训练增强设计

## 概述

将「专项训练」从简陋的文本交互升级为与「模拟面试」一致的完整体验：支持题数选择、逐题作答、AI 评价打分、追问、进度跟踪。核心是让 AI 基于简历 PDF 内容（技术栈、项目经验、专业技能）出题，从面试官角度考察候选人。

## 当前问题

- 专项训练页面没有题数选择、评价展示、进度追踪
- 简历已改为 PDF 上传，但 `rawContent` 为空，AI 出题时拿不到简历内容
- AI 出题 prompt 过于通用，未充分利用简历中的具体技术栈和项目信息

## 交互流程

```
设置区（选题数）→ 开始 → AI 分析简历 → 逐题作答 → AI 评价 → 支持追问 → 下一题（循环至目标题数）→ 完成
```

页面三阶段：
- **A - 设置**：标题 + 题数选择器 + 开始按钮
- **B - 答题**：进度条 + 题目卡片 + 输入框 + 评价展示 + 追问/下一题
- **C - 完成**：结果展示 + 查看复盘/再来一次

## PDF 文字提取

**位置**：`InterviewServiceImpl.handleResumeQuestion()`

**逻辑**：
1. 获取 resume 记录
2. 如果 `pdfPath` 不为空 → `extractTextFromPdf()` 用 PDFBox 提取文字
3. 如果 `pdfPath` 为空但有 `rawContent` → 直接使用（兼容旧数据）
4. 将文字传入 AI 出题

**依赖**：`pom.xml` 添加 Apache PDFBox

## AI 出题策略

session.type == 2 时，调用 AI 的出题 prompt：

```
你是一位资深技术面试官。以下是一份候选人的简历，请根据简历中出现的
技术栈、项目经验、专业技能进行提问，每次只出一道题。

要求：
- 问题必须严格基于简历中出现的内容
- 考察技术深度，而非表面概念
- 不要出简历中没有提到的技术
- 不要重复已出过的题目

简历内容：{resumeText}
已出过的题目：{previousQuestions}

请输出下一道面试题：
```

答题评价和追问复用现有 `submitAnswer()` / `askFollowUp()` 逻辑，不需要改动。

## 前端改动

重写 `SpecialTraining.vue`，参考 `MockInterview.vue` 结构但简化（去掉模式选择卡片）：
- 设置区：仅保留题数选择 + 开始按钮
- 答题区：进度条 + 题目 + 输入 + 评价 + 追问
- 完成区：结果 + 复盘入口

## 后端改动

| 文件 | 改动 |
|------|------|
| `java/pom.xml` | 添加 Apache PDFBox 依赖 |
| `InterviewServiceImpl.java` | 增强 `handleResumeQuestion()`：PDF 提取 + 改进 prompt + 防重复 |
| 其他 | 无需新建 API，复用现有接口 |

## 涉及文件清单

- Modify: `front/src/views/interview/SpecialTraining.vue`
- Modify: `java/pom.xml`
- Modify: `java/src/main/java/com/yumian/service/impl/InterviewServiceImpl.java`
