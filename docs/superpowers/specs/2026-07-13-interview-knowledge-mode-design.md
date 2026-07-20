# 模拟面试知识库双模式设计

## 背景

现有模拟面试（type=1）由 AI 随机出题，与用户备考方向脱节。知识库上线后，用户希望在模拟面试中充分利用已有题库，支持两种模式：

1. **题库模式**：从知识库直接抽取原题，原封不动展示给用户
2. **智能模式**：从知识库抽取题目后，AI 改写表述/变换角度再出题

## 变更范围

### 表结构

`interview_session` 新增字段：

```sql
ALTER TABLE interview_session
    ADD COLUMN target_count      INT    DEFAULT 0  COMMENT '目标题数',
    ADD COLUMN source_question_ids TEXT  COMMENT '本会话已出的知识库题ID列表，逗号分隔';
```

- `target_count`：用户设定的目标题数，达到后自动结束
- `source_question_ids`：去重用，替代内存 Map（掉线恢复不丢状态）

### type 语义重新映射

| type | 原名 | 新含义 |
|------|------|--------|
| 1 | 模拟面试（AI 随机出题） | 题库模式：从知识库出原题 |
| 2 | 专项训练（简历面） | 不变 |
| 3 | 知识库面（RAG） | 智能模式：知识库出题，AI 改写后面 |

原有的 type=1 AI 随机出题逻辑废弃。

## API 变更

### `POST /api/interview/session` — 创建会话（修改）

请求体新增：

```json
{
  "type": 1,
  "targetCount": 5
}
```

`targetCount` 范围 1-20。

校验：若 type=1 或 type=3 时知识库可用题数为 0，返回 400 + 错误提示"知识库暂无可用题目，请联系管理员导入题库"。

### `POST /api/interview/question` — 获取下一题（修改）

根据 `session.type` 走不同分支：

**type=1（题库模式）**
1. 解析 `source_question_ids`，从 `knowledge_question` 随机取一条 `status=1` 且不在 ID 列表中的记录
2. 若所有可用题目已出完，返回特殊响应 `{ "noMoreQuestions": true }`
3. 返回题目内容

**type=3（智能模式）**
1. 同上步 1 抽取原题
2. 调用 AI 改写 prompt，将原题 + 参考答案发给 AI，要求：保持考察点不变，换种表述或变换角度出题
3. 返回改写后的题目

新响应字段（适用于两种模式）：

```json
{
  "question": "请解释Java内存模型...",
  "isFromKnowledgeBase": true,
  "sourceQuestionId": 42,
  "noMoreQuestions": false
}
```

智能模式额外返回：

```json
{
  "originalQuestion": "请说明JMM是什么",
  "question": "假设你在排查一个多线程并发问题，JMM中的哪些概念能帮助你分析？"
}
```

### 现有接口不变

- `POST /api/interview/answer` — 提交答案（逻辑不变）
- `POST /api/interview/end` — 主动结束（不变）
- `GET /api/interview/session/{id}` — 会话详情（不变）
- `GET /api/interview/qa/{sessionId}` — QA 列表（不变）

## 自动结束逻辑

每个 `submitAnswer` 成功后判断：`questionCount >= targetCount`，若是则自动调用 `endSession` 生成总结评价，并将 `session.status` 置为 1（已结束）。

返回给前端增加 `sessionEnded: true` 标志。

## 前端变更

### 侧边栏「模拟面试」

点击后不直接进入页面，弹出模式选择弹窗：

```
┌──────────────────────┐
│  选择面试模式         │
│                      │
│  📖 题库模式          │
│  从知识库出原题        │
│                      │
│  ✏️ 智能模式          │
│  知识库出题+AI改写     │
│                      │
│  [取消]              │
└──────────────────────┘
```

选择后跳转到 `/interview/mock?type=1` 或 `/interview/mock?type=3`。

### 模拟面试页 `MockInterview.vue`

1. **初始状态** — 显示题数设置界面：数字输入框（1-20）+「开始面试」按钮
2. **面试中** — 沿用现有问答界面布局，添加：
   - 顶部进度条 `当前题数/targetCount`
   - 智能模式下题目上方灰色小字标注「改编自：xxx」
3. **已结束** — 显示结束页，包含：
   - 本次得分/评价
   - 「查看复盘」按钮 → 跳转复盘页
   - 「返回首页」按钮

## 边界情况

| 场景 | 处理 |
|------|------|
| 知识库为空 | 创建会话时校验，返回错误提示 |
| 所有题目已出完 | `question` 接口返回 `noMoreQuestions: true`，前端自动结束并提示 |
| 用户刷新/断线恢复 | `source_question_ids` 存 DB，重新加载会话后恢复正确进度 |
| targetCount 手动设为 0 | 改为题数设置界面，下限为 1 |
| 智能模式改写失败（AI 异常） | 降级为原题模式，返回原题并在响应中标记 `originalFallback: true` |

## 不变的部分

- type=2 专项训练（简历面）代码完全不变
- 答题评价逻辑（AI 评分 + 评价 + 参考答案）不变
- 追问逻辑不变
- 知识库管理端（增删改查导入导出）不变
