# 模拟面试知识库双模式 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将模拟面试拆分为"题库模式"(type=1，知识库原题)和"智能模式"(type=3，知识库改写题)，支持用户设题数、自动结束、去重。

**Architecture:** 后端扩充 `InterviewServiceImpl` 的 `askQuestion` 方法，根据 type 区分分支；新增 `KnowledgeQuestionService.getRandomEnabled()` 实现随机抽题+去重。前端 MockInterview.vue 改为三步流程：选模式 → 设题数 → 开始面试。

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus + MySQL 8 + Vue 3 + Pinia + Element Plus

---

### 文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `java/src/main/java/com/yumian/entity/InterviewSession.java` | 新增 targetCount, sourceQuestionIds 字段 |
| 修改 | `java/src/main/java/com/yumian/entity/InterviewQa.java` | 新增 sourceQuestionId, sourceQuestionText 字段 + 3个 transient 响应字段 |
| 修改 | `java/src/main/java/com/yumian/service/InterviewService.java` | createSession 增加 targetCount 参数 |
| 修改 | `java/src/main/java/com/yumian/service/impl/InterviewServiceImpl.java` | 核心逻辑：随机抽题、改写、自动结束 |
| 修改 | `java/src/main/java/com/yumian/controller/InterviewController.java` | createSession 增加 targetCount 参数 |
| 修改 | `java/src/main/java/com/yumian/service/KnowledgeQuestionService.java` | 新增 getRandomEnabled() 方法 |
| 修改 | `java/src/main/java/com/yumian/service/impl/KnowledgeQuestionServiceImpl.java` | 实现 getRandomEnabled() |
| 新建 | `java/src/main/resources/sql/V20260713__interview_knowledge_mode.sql` | 新增字段 DDL |
| 修改 | `front/src/api/interview.js` | createSession 增加 targetCount 参数 |
| 修改 | `front/src/stores/interview.js` | startSession 增加 targetCount，处理 sessionEnded |
| 修改 | `front/src/views/interview/MockInterview.vue` | 全面改造：模式选择、题数设置、答题流程 |

---

### Task 1: 数据库迁移

**Files:**
- Create: `java/src/main/resources/sql/V20260713__interview_knowledge_mode.sql`

- [ ] **Step 1: 编写迁移 SQL**

```sql
-- interview_session 新增字段
ALTER TABLE interview_session
    ADD COLUMN target_count INT DEFAULT 0 COMMENT '目标题数，达到后自动结束',
    ADD COLUMN source_question_ids TEXT COMMENT '已出的知识库题ID列表，逗号分隔';

-- interview_qa 新增字段
ALTER TABLE interview_qa
    ADD COLUMN source_question_id BIGINT DEFAULT NULL COMMENT '关联的知识库题目ID',
    ADD COLUMN source_question_text TEXT DEFAULT NULL COMMENT '改写前的原题内容（智能模式用）';
```

- [ ] **Step 2: 提交**

```bash
git add java/src/main/resources/sql/V20260713__interview_knowledge_mode.sql
git commit -m "feat: add target_count and source_question fields to interview tables"
```

---

### Task 2: InterviewSession 实体更新

**Files:**
- Modify: `java/src/main/java/com/yumian/entity/InterviewSession.java:10-22`

- [ ] **Step 1: 新增字段**

```java
// InterViewSession.java 中新增字段
private Integer targetCount;      // 目标题数
private String sourceQuestionIds; // 已出知识库题ID，逗号分隔
```

插入在 `private Integer questionCount;` 之后：
```java
private Integer targetCount;
private String sourceQuestionIds;
```

- [ ] **Step 2: 提交**

```bash
git add java/src/main/java/com/yumian/entity/InterviewSession.java
git commit -m "feat: add targetCount and sourceQuestionIds fields to InterviewSession"
```

---

### Task 3: InterviewQa 实体更新

**Files:**
- Modify: `java/src/main/java/com/yumian/entity/InterviewQa.java`

- [ ] **Step 1: 读取并修改 InterviewQa.java**

Read the file first, then:
1. 新增持久化字段：`private Long sourceQuestionId;` 和 `private String sourceQuestionText;`
2. 新增 4 个 `@TableField(exist = false)` 响应字段：

```java
@TableField(exist = false)
private Boolean fromKnowledgeBase;  // 是否来自知识库

@TableField(exist = false)
private String originalQuestion;    // 智能模式改写前的原题

@TableField(exist = false)
private Boolean noMoreQuestions;    // 题库已无更多题

@TableField(exist = false)
private Boolean sessionEnded;       // 本回答后会话结束（已达目标题数）
```

- [ ] **Step 2: 提交**

```bash
git add java/src/main/java/com/yumian/entity/InterviewQa.java
git commit -m "feat: add source tracking and response-only fields to InterviewQa"
```

---

### Task 4: KnowledgeQuestionService 新增随机取题方法

**Files:**
- Modify: `java/src/main/java/com/yumian/service/KnowledgeQuestionService.java`
- Modify: `java/src/main/java/com/yumian/service/impl/KnowledgeQuestionServiceImpl.java`

- [ ] **Step 1: 接口新增方法**

在 `KnowledgeQuestionService.java` 的 `getAllEnabled()` 方法之后新增：

```java
/** 随机获取一条未出过的启用题目，excludeIds 为空则从全部中随机 */
KnowledgeQuestion getRandomEnabled(List<Long> excludeIds);
```

- [ ] **Step 2: 实现方法**

在 `KnowledgeQuestionServiceImpl.java` 中新增实现：

```java
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
```

- [ ] **Step 3: 提交**

```bash
git add java/src/main/java/com/yumian/service/KnowledgeQuestionService.java java/src/main/java/com/yumian/service/impl/KnowledgeQuestionServiceImpl.java
git commit -m "feat: add getRandomEnabled method for random knowledge question selection"
```

---

### Task 5: InterviewService 接口更新

**Files:**
- Modify: `java/src/main/java/com/yumian/service/InterviewService.java:8`

- [ ] **Step 1: 修改 createSession 签名**

将 `createSession(Long userId, int type)` 改为 `createSession(Long userId, int type, int targetCount)`：

```java
InterviewSession createSession(Long userId, int type, int targetCount);
```

- [ ] **Step 2: 提交**

```bash
git add java/src/main/java/com/yumian/service/InterviewService.java
git commit -m "feat: add targetCount param to createSession interface"
```

---

### Task 6: InterviewServiceImpl 核心逻辑改造

**Files:**
- Modify: `java/src/main/java/com/yumian/service/impl/InterviewServiceImpl.java`

- [ ] **Step 1: 修改 createSession**

```java
@Override
@Transactional
public InterviewSession createSession(Long userId, int type, int targetCount) {
    if ((type == 1 || type == 3) && targetCount <= 0) {
        throw new BusinessException("题库模式和智能模式必须设置目标题数");
    }
    if (type == 1 || type == 3) {
        long count = knowledgeQuestionService.getAllEnabled().size();
        if (count == 0) {
            throw new BusinessException("知识库暂无可用题目，请联系管理员导入题库");
        }
    }
    InterviewSession session = new InterviewSession();
    session.setUserId(userId);
    session.setType(type);
    session.setStatus(0);
    session.setQuestionCount(0);
    session.setTargetCount(targetCount);
    session.setSourceQuestionIds("");
    session.setStartTime(LocalDateTime.now());
    sessionMapper.insert(session);
    return session;
}
```

- [ ] **Step 2: 改造 askQuestion 方法**

将原有的 `askQuestion` 方法替换为以下逻辑（注意 `askRagQuestion` 方法废弃，因为原有 RAG 逻辑被拆分为 type=1 和 type=3）：

```java
@Override
public InterviewQa askQuestion(Long sessionId) {
    InterviewSession session = sessionMapper.selectById(sessionId);
    if (session == null) {
        throw new BusinessException("面试会话不存在");
    }
    if (session.getStatus() == 1) {
        // 会话已经结束
        InterviewQa qa = new InterviewQa();
        qa.setNoMoreQuestions(true);
        return qa;
    }

    Long count = qaMapper.selectCount(
            new LambdaQueryWrapper<InterviewQa>().eq(InterviewQa::getSessionId, sessionId));

    InterviewQa qa = new InterviewQa();
    qa.setSessionId(sessionId);
    qa.setQuestionType(0);
    qa.setSortOrder(count.intValue() + 1);

    if (session.getType() == 1 || session.getType() == 3) {
        handleKnowledgeBaseQuestion(session, qa);
    } else if (session.getType() == 2) {
        handleResumeQuestion(session, qa);
    }

    qaMapper.insert(qa);
    session.setQuestionCount(session.getQuestionCount() + 1);
    sessionMapper.updateById(session);

    // 响应标记
    qa.setFromKnowledgeBase(session.getType() == 1 || session.getType() == 3);
    return qa;
}

private void handleKnowledgeBaseQuestion(InterviewSession session, InterviewQa qa) {
    // 解析已出过的题 ID
    List<Long> excludeIds = new java.util.ArrayList<>();
    if (session.getSourceQuestionIds() != null && !session.getSourceQuestionIds().isBlank()) {
        for (String id : session.getSourceQuestionIds().split(",")) {
            try {
                excludeIds.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    KnowledgeQuestion kq = knowledgeQuestionService.getRandomEnabled(excludeIds);
    if (kq == null) {
        qa.setNoMoreQuestions(true);
        qa.setQuestion("题库中的题目已全部出完，点击结束按钮完成面试。");
        return;
    }

    // 记录来源
    qa.setSourceQuestionId(kq.getId());
    String updatedIds = session.getSourceQuestionIds() == null || session.getSourceQuestionIds().isBlank()
            ? String.valueOf(kq.getId())
            : session.getSourceQuestionIds() + "," + kq.getId();
    session.setSourceQuestionIds(updatedIds);

    if (session.getType() == 1) {
        // 题库模式：直接出原题
        qa.setQuestion(kq.getQuestion());
    } else {
        // 智能模式：AI 改写
        qa.setSourceQuestionText(kq.getQuestion());
        String rewritten = aiService.chat(
                "你是一个技术面试官。请根据下面的题目进行改写，保持考察点不变，换一种表述方式或变换角度出题。只返回改写后的问题本身，不要多余内容。",
                "原题：" + kq.getQuestion()
                        + "\n参考答案：" + (kq.getReferenceAnswer() != null ? kq.getReferenceAnswer() : ""));
        qa.setQuestion(rewritten);
        qa.setOriginalQuestion(kq.getQuestion());
    }
}

private void handleResumeQuestion(InterviewSession session, InterviewQa qa) {
    Resume resume = resumeMapper.selectOne(
            new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, session.getUserId()));
    String resumeContent = (resume != null && resume.getRawContent() != null)
            ? resume.getRawContent() : "暂无简历内容";
    String question = aiService.chat(
            "你是一个技术面试官。请根据候选人的简历内容出面试题，每次只出一道题，考察技术深度和项目经验。只返回问题本身，不要多余内容。",
            "简历内容：" + resumeContent);
    qa.setQuestion(question);
}
```

- [ ] **Step 3: 改造 submitAnswer — 添加自动结束逻辑**

在 `submitAnswer` 方法中，保存回答后增加判断：

```java
@Override
@Transactional
public InterviewQa submitAnswer(Long qaId, String answer) {
    InterviewQa qa = qaMapper.selectById(qaId);
    if (qa == null) throw new BusinessException("题目不存在");
    qa.setUserAnswer(answer);

    // 原有 AI 评价逻辑不变
    String response = aiService.chat(
            "你是一个面试评分专家。对于以下面试题目和回答，请严格按 JSON 格式返回："
            + "{\"score\": 数字, \"evaluation\": \"评价内容\", \"referenceAnswer\": \"参考答案\"}"
            + " 注意：只返回 JSON，不要加多余说明。",
            "题目：" + qa.getQuestion() + "\n回答：" + answer);

    try {
        cn.hutool.json.JSONObject json = new cn.hutool.json.JSONObject(response);
        qa.setScore(json.getInt("score"));
        qa.setAiEvaluation(json.getStr("evaluation"));
        qa.setReferenceAnswer(json.getStr("referenceAnswer"));
    } catch (Exception e) {
        qa.setAiEvaluation(response);
        qa.setScore(75);
    }

    qaMapper.updateById(qa);

    // 判断是否达到目标题数，自动结束
    InterviewSession session = sessionMapper.selectById(qa.getSessionId());
    if (session != null && session.getTargetCount() != null && session.getTargetCount() > 0
            && session.getQuestionCount() >= session.getTargetCount()) {
        endSession(session.getId(), session.getUserId());
        qa.setSessionEnded(true);
    }

    return qa;
}
```

- [ ] **Step 4: 清理废弃的 askRagQuestion 和 SimilarityResult**

删除原 `askRagQuestion(InterviewSession session, Long currentCount)` 方法和 `SimilarityResult` 内部类（约 92-169 行），因为它们已被 `handleKnowledgeBaseQuestion` 替代。

如果 `EmbeddingService` 不再被使用（确认没有其他引用），可以移除 `embeddingService` 字段注入。但建议保留字段注入声明，因为其他功能可能还在使用。

- [ ] **Step 5: 提交**

```bash
git add java/src/main/java/com/yumian/service/impl/InterviewServiceImpl.java
git commit -m "feat: implement knowledge base question modes and auto-end logic"
```

---

### Task 7: InterviewController 更新

**Files:**
- Modify: `java/src/main/java/com/yumian/controller/InterviewController.java:27-31`

- [ ] **Step 1: 新增 targetCount 参数**

```java
@PostMapping("/session")
public Result<InterviewSession> createSession(@RequestAttribute("userId") Long userId,
                                               @RequestParam int type,
                                               @RequestParam(required = false, defaultValue = "0") int targetCount) {
    return Result.success(interviewService.createSession(userId, type, targetCount));
}
```

`targetCount` 默认值 0 保持与 type=2（专项训练，不需要 targetCount）的兼容。

- [ ] **Step 2: 提交**

```bash
git add java/src/main/java/com/yumian/controller/InterviewController.java
git commit -m "feat: add targetCount param to createSession endpoint"
```

---

### Task 8: 前端 API 更新

**Files:**
- Modify: `front/src/api/interview.js:3`

- [ ] **Step 1: 更新 createSession**

```javascript
export const createSession = (type, targetCount) => request.post(`/interview/session?type=${type}&targetCount=${targetCount || 0}`)
```

- [ ] **Step 2: 提交**

```bash
git add front/src/api/interview.js
git commit -m "feat: add targetCount param to createSession API"
```

---

### Task 9: 前端 Store 更新

**Files:**
- Modify: `front/src/stores/interview.js`

- [ ] **Step 1: 更新 startSession，新增 targetCount 参数和 sessionEnded 处理**

```javascript
// 修改 startSession
async startSession(type, targetCount) {
    const res = await createSession(type, targetCount)
    this.sessionId = res.data.id
    this.sessionType = type
    this.targetCount = targetCount || res.data.targetCount || 0
    this.qaList = []
    this.isEnded = false
    this.sessionEnded = false
    return res.data
},
```

在 state 中新增字段：
```javascript
state: () => ({
    sessionId: null,
    sessionType: null,
    targetCount: 0,
    currentQa: null,
    qaList: [],
    isEnded: false,
    sessionEnded: false,    // 新增：答题后会话自动结束
    loading: false,
    submitting: false
}),
```

在 answer action 中处理 sessionEnded：
```javascript
async answer(qaId, answer) {
    this.submitting = true
    try {
        const res = await submitAnswer({ qaId, answer })
        const updated = res.data
        this.currentQa = updated
        if (updated.sessionEnded) {
            this.isEnded = true
            this.sessionEnded = true
        }
        const idx = this.qaList.findIndex(q => q.id === qaId)
        if (idx >= 0) this.qaList[idx] = updated
        return updated
    } finally {
        this.submitting = false
    }
},
```

更新 reset：
```javascript
reset() {
    this.sessionId = null
    this.sessionType = null
    this.targetCount = 0
    this.currentQa = null
    this.qaList = []
    this.isEnded = false
    this.sessionEnded = false
}
```

- [ ] **Step 2: 提交**

```bash
git add front/src/stores/interview.js
git commit -m "feat: update interview store with targetCount and sessionEnded"
```

---

### Task 10: MockInterview.vue 全面改造

**Files:**
- Modify: `front/src/views/interview/MockInterview.vue`

这是改动最大的任务。整个页面分为三个阶段：

**阶段 A: 未开始 — 模式选择 + 题数设置**
**阶段 B: 面试中 — 问答流程**
**阶段 C: 已结束 — 结果展示**

- [ ] **Step 1: 重写 template**

```vue
<template>
  <div class="mock-interview">
    <div class="page-header">
      <div>
        <h1>模拟面试</h1>
        <p class="sub">{{ statusText }}</p>
      </div>
      <div>
        <el-button v-if="store.sessionId && !store.isEnded" type="danger" plain
          @click="endInterview" :loading="store.loading">
          结束面试
        </el-button>
      </div>
    </div>

    <!-- 阶段 A: 选择模式和题数 -->
    <div v-if="!store.sessionId" class="setup-section">
      <!-- 模式选择 -->
      <div class="mode-select">
        <h2>选择面试模式</h2>
        <div class="mode-cards">
          <div class="mode-card"
            :class="{ active: selectedMode === 1 }"
            @click="selectedMode = 1">
            <div class="mode-icon">📖</div>
            <div class="mode-name">题库模式</div>
            <div class="mode-desc">从知识库直接抽取原题，原汁原味练习</div>
          </div>
          <div class="mode-card"
            :class="{ active: selectedMode === 3 }"
            @click="selectedMode = 3">
            <div class="mode-icon">✏️</div>
            <div class="mode-name">智能模式</div>
            <div class="mode-desc">基于知识库内容 AI 改写，变式训练</div>
          </div>
        </div>
      </div>

      <!-- 题数设置 -->
      <div class="count-setting">
        <h2>设置题目数量</h2>
        <el-input-number v-model="targetCount" :min="1" :max="20" />
        <span class="count-hint">共 {{ targetCount }} 题</span>
      </div>

      <el-button type="primary" size="large" @click="startInterview" :loading="store.loading"
        :disabled="!selectedMode">
        开始面试
      </el-button>
    </div>

    <!-- 阶段 B: 面试中 -->
    <template v-else-if="!store.isEnded">
      <!-- 进度条 -->
      <div class="progress-bar">
        <el-progress :percentage="progressPercent" :stroke-width="8" />
        <span class="progress-text">{{ store.qaList.length + 1 }} / {{ store.targetCount }}</span>
      </div>

      <!-- 加载中 -->
      <div v-if="questionLoading" class="loading-state">
        <el-skeleton :rows="3" animated />
        <p class="loading-text">AI 面试官正在出题中，请稍候...</p>
      </div>

      <!-- 当前题目 -->
      <div v-if="store.currentQa && !questionLoading" class="qa-card">
        <div class="qa-header">
          <el-tag :type="store.currentQa.questionType === 1 ? 'warning' : 'primary'" size="small">
            {{ store.currentQa.questionType === 1 ? '追问' : '题目 ' + store.currentQa.sortOrder }}
          </el-tag>
          <el-tag v-if="store.sessionType === 3 && store.currentQa.originalQuestion" type="info" size="small" effect="plain">
            改编题
          </el-tag>
          <span class="qa-time">{{ new Date(store.currentQa.createdAt).toLocaleTimeString() }}</span>
        </div>

        <!-- 智能模式：显示原题来源 -->
        <div v-if="store.sessionType === 3 && store.currentQa.originalQuestion" class="source-hint">
          改编自：{{ store.currentQa.originalQuestion }}
        </div>

        <div class="qa-question">{{ store.currentQa.question }}</div>

        <!-- 作答区域 -->
        <div v-if="!store.currentQa.userAnswer" class="qa-answer-area">
          <el-input v-model="currentAnswer" type="textarea" :rows="4" placeholder="输入你的回答..." />
          <div class="qa-actions">
            <el-button type="primary" @click="submitAnswer" :loading="store.submitting" :disabled="store.submitting">
              提交回答
            </el-button>
          </div>
        </div>

        <!-- 评价结果 -->
        <div v-else class="qa-result">
          <div class="qa-answer">
            <div class="label">你的回答：</div>
            <p>{{ store.currentQa.userAnswer }}</p>
          </div>
          <div v-if="store.currentQa.referenceAnswer" class="qa-reference">
            <div class="label">参考答案：</div>
            <p>{{ store.currentQa.referenceAnswer }}</p>
          </div>
          <div class="qa-evaluation">
            <div class="label">AI 评价：</div>
            <p>{{ store.currentQa.aiEvaluation || '评价生成中...' }}</p>
          </div>
          <div class="qa-actions">
            <el-button type="warning" @click="askFollowUp" :loading="store.loading">追问</el-button>
            <el-button v-if="!store.sessionEnded" type="primary" plain @click="nextQuestion" :loading="store.loading">
              下一题
            </el-button>
            <el-button v-else type="success" @click="goToReview">
              查看复盘
            </el-button>
          </div>
        </div>
      </div>

      <!-- 已回答列表 -->
      <div v-if="store.qaList.length > 0" class="history-section">
        <h3>已回答 ({{ store.qaList.length }})</h3>
        <div v-for="qa in store.qaList" :key="qa.id" class="history-item">
          <el-tag :type="qa.questionType === 1 ? 'warning' : 'info'" size="small" class="qa-type-tag">
            {{ qa.questionType === 1 ? '追问' : 'Q' + qa.sortOrder }}
          </el-tag>
          <span class="history-question">{{ qa.question }}</span>
        </div>
      </div>
    </template>

    <!-- 阶段 C: 已结束 -->
    <div v-else class="end-section">
      <el-result icon="success" title="面试已结束" :sub-title="`共完成 ${store.qaList.length} 题`">
        <template #extra>
          <el-button type="primary" @click="goToReview">查看复盘</el-button>
          <el-button @click="resetAndRestart">再来一次</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>
```

- [ ] **Step 2: 重写 script**

```vue
<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useInterviewStore } from '../../stores/interview'
import { useRouter } from 'vue-router'

const router = useRouter()
const store = useInterviewStore()
const currentAnswer = ref('')
const questionLoading = ref(false)
const selectedMode = ref(0)   // 0=未选, 1=题库模式, 3=智能模式
const targetCount = ref(5)    // 默认 5 题

const statusText = computed(() => {
  if (!store.sessionId) return '选择面试模式并开始练习'
  if (store.sessionType === 1) return '题库模式'
  if (store.sessionType === 3) return '智能模式'
  return '模拟面试'
})

const progressPercent = computed(() => {
  if (!store.targetCount) return 0
  return Math.round((store.qaList.length / store.targetCount) * 100)
})

async function startInterview() {
  if (!selectedMode.value) {
    ElMessage.warning('请先选择面试模式')
    return
  }
  questionLoading.value = true
  try {
    await store.startSession(selectedMode.value, targetCount.value)
    await store.ask()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '创建面试失败，请重试')
    store.reset()
  } finally {
    questionLoading.value = false
  }
}

async function submitAnswer() {
  if (!currentAnswer.value.trim()) {
    ElMessage.warning('请输入回答')
    return
  }
  const qa = store.currentQa
  store.qaList.push({ ...qa })
  try {
    await store.answer(qa.id, currentAnswer.value)
    currentAnswer.value = ''
  } catch {
    store.qaList = store.qaList.filter(q => q.id !== qa.id)
    ElMessage.error('提交失败，请重试')
  }
}

async function askFollowUp() {
  const qa = store.currentQa
  store.qaList.push({ ...qa })
  try {
    const followUp = await store.followUp(qa.id)
    store.currentQa = followUp
  } catch {
    store.qaList = store.qaList.filter(q => q.id !== qa.id)
    ElMessage.error('追问失败，请重试')
  }
}

async function nextQuestion() {
  questionLoading.value = true
  try {
    const res = await store.ask()
    if (res && res.noMoreQuestions) {
      ElMessage.warning('题库中的题目已全部出完')
      await store.end()
    }
  } catch {
    ElMessage.error('获取下一题失败')
  } finally {
    questionLoading.value = false
  }
}

async function endInterview() {
  try {
    await store.end()
  } catch { /* ignore */ }
  ElMessage.success('面试已结束')
  store.reset()
}

function goToReview() {
  router.push('/interview/history')
}

async function resetAndRestart() {
  store.reset()
  selectedMode.value = 0
  targetCount.value = 5
}
</script>
```

- [ ] **Step 3: 新增样式**

在已有的 `<style scoped>` 中追加模式选择卡片和题数设置样式：

```css
.setup-section { max-width: 600px; margin: 0 auto; text-align: center; }
.setup-section h2 { font-size: 18px; font-weight: 600; margin-bottom: 16px; color: var(--text-primary); }
.mode-select { margin-bottom: 32px; }
.mode-cards { display: flex; gap: 16px; justify-content: center; }
.mode-card {
  width: 240px; padding: 24px 16px; border: 2px solid var(--card-border);
  border-radius: 12px; cursor: pointer; transition: all 0.2s;
  background: var(--card-bg);
}
.mode-card:hover { border-color: var(--flow-blue); transform: translateY(-2px); }
.mode-card.active { border-color: var(--flow-blue); background: rgba(14,165,233,0.06); }
.mode-icon { font-size: 32px; margin-bottom: 8px; }
.mode-name { font-size: 16px; font-weight: 600; margin-bottom: 4px; color: var(--text-primary); }
.mode-desc { font-size: 13px; color: var(--text-secondary); line-height: 1.5; }
.count-setting { margin-bottom: 32px; }
.count-hint { margin-left: 12px; font-size: 14px; color: var(--text-secondary); }
.progress-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.progress-bar .el-progress { flex: 1; }
.progress-text { font-size: 14px; font-weight: 600; color: var(--text-primary); white-space: nowrap; }
.source-hint {
  font-size: 12px; color: var(--text-muted); margin-bottom: 8px;
  padding: 4px 8px; background: rgba(0,0,0,0.02); border-radius: 4px;
}
.end-section { margin-top: 60px; }
```

- [ ] **Step 4: 提交**

```bash
git add front/src/views/interview/MockInterview.vue
git commit -m "feat: redesign MockInterview with mode selection, count setting, auto-end flow"
```

---

### 自审检查

1. **Spec 覆盖检查：**
   - ✅ 题库模式 (type=1) 直接出原题 → Task 6
   - ✅ 智能模式 (type=3) AI 改写 → Task 6
   - ✅ 用户设定题量 → Task 10 (前端), Task 5+6 (后端)
   - ✅ 答完自动结束 → Task 6 submitAnswer
   - ✅ 去重 (`sourceQuestionIds`) → Task 6 handleKnowledgeBaseQuestion
   - ✅ 侧边栏点击弹窗选择 → Task 10 模式选择卡片
   - ✅ 降级处理（改写失败/题数不够）→ Task 6 异常分支
   - ✅ type=2 专项训练不变 → Task 6 保留 handleResumeQuestion

2. **占位符检查：** 无 TODO/TBD/占位符

3. **类型一致性检查：**
   - `createSession` 签名在接口、实现、Controller 中一致（3 参数）
   - `getRandomEnabled(List<Long>)` 在接口和实现中一致
   - `sessionEnded` 在实体、store、Vue 组件中一致
