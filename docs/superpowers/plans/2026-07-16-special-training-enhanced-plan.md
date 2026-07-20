# 专项训练增强实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将专项训练升级为与模拟面试一致的完整交互，AI 基于简历 PDF 内容出题

**Architecture:** 后端新增 PDFBox 提取 PDF 文字 + 增强 AI 出题 prompt（基于简历技术栈、不重复出题）；前端重写为三段式（设置→答题→完成），复用现有模拟面试的 store 和 API

**Tech Stack:** Spring Boot 3.2 + Apache PDFBox 3.0.3 (PDF 提取), Vue 3 + Element Plus (前端)

---

## 修改的文件

| 文件 | 改动 |
|------|------|
| `java/pom.xml` | 添加 Apache PDFBox 3.0.3 依赖 |
| `java/.../service/impl/InterviewServiceImpl.java` | 新增 `extractTextFromPdf()` + 增强 `handleResumeQuestion()` |
| `front/src/views/interview/SpecialTraining.vue` | 完整重写，参考 MockInterview.vue 结构 |

---

### Task 1: 后端 — pom.xml 添加 PDFBox 依赖

**Files:**
- Modify: `java/pom.xml`

- [ ] **Step 1: 添加 PDFBox dependency**

在 `pom.xml` 的 `<dependencies>` 中（建议放在 `poi-ooxml` 后面）：

```xml
        <!-- Apache PDFBox (简历 PDF 文字提取) -->
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>3.0.3</version>
        </dependency>
```

---

### Task 2: 后端 — InterviewServiceImpl 增强

**Files:**
- Modify: `java/src/main/java/com/yumian/service/impl/InterviewServiceImpl.java`

- [ ] **Step 1: 添加 PDFBox import**

```java
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
```

- [ ] **Step 2: 新增 `extractTextFromPdf()` 私有方法**

添加到 `InterviewServiceImpl` 类中（放在 `handleResumeQuestion` 方法之前或之后）：

```java
private String extractTextFromPdf(String pdfPath) {
    java.io.File file = new java.io.File(
            System.getProperty("user.dir") + java.io.File.separator + "userdata" + java.io.File.separator + pdfPath);
    if (!file.exists()) {
        throw new BusinessException("简历文件不存在");
    }
    try (PDDocument document = PDDocument.load(file)) {
        PDFTextStripper stripper = new PDFTextStripper();
        return stripper.getText(document);
    } catch (java.io.IOException e) {
        throw new BusinessException("简历 PDF 解析失败");
    }
}
```

注意：`pdfPath` 存储格式为 `1/resume.pdf`，`RESUME_DIR = "userdata/resumes"`，所以完整路径为 `{user.dir}/userdata/resumes/1/resume.pdf`。

- [ ] **Step 3: 重写 `handleResumeQuestion()` 方法**

```java
private void handleResumeQuestion(InterviewSession session, InterviewQa qa) {
    // 1. 获取简历内容
    Resume resume = resumeMapper.selectOne(
            new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, session.getUserId()));
    if (resume == null) {
        qa.setQuestion("请先在「我的简历」中上传简历");
        return;
    }

    String resumeContent;
    if (resume.getPdfPath() != null) {
        // 从 PDF 提取文字
        resumeContent = extractTextFromPdf(resume.getPdfPath());
    } else if (resume.getRawContent() != null && !resume.getRawContent().isBlank()) {
        // 兼容旧数据（文本简历）
        resumeContent = resume.getRawContent();
    } else {
        qa.setQuestion("请先在「我的简历」中上传简历");
        return;
    }

    // 2. 收集已出过的题目（避免重复）
    List<InterviewQa> existingQas = qaMapper.selectList(
            new LambdaQueryWrapper<InterviewQa>()
                    .eq(InterviewQa::getSessionId, session.getId())
                    .eq(InterviewQa::getQuestionType, 0));  // 只收集普通题，不包括追问
    StringBuilder previousQuestions = new StringBuilder();
    for (InterviewQa existing : existingQas) {
        previousQuestions.append("- ").append(existing.getQuestion()).append("\n");
    }

    // 3. 构建 prompt 并调用 AI
    String prompt = "你是一位资深技术面试官。以下是一份候选人的简历，请根据简历中出现的" +
            "技术栈、项目经验、专业技能进行提问，每次只出一道题。\n\n" +
            "要求：\n" +
            "- 问题必须严格基于简历中出现的内容\n" +
            "- 考察技术深度，而非表面概念\n" +
            "- 不要出简历中没有提到的技术\n" +
            "- 不要重复已出过的题目\n\n" +
            "简历内容：\n" + resumeContent + "\n\n";
    if (previousQuestions.length() > 0) {
        prompt += "已出过的题目：\n" + previousQuestions.toString() + "\n";
    }
    prompt += "请输出下一道面试题：";

    String question = aiService.chat(prompt, "");
    qa.setQuestion(question);
}
```

注意：需要在 `InterviewServiceImpl` 类中已有的 import 基础上，确认 `java.util.List` 已导入（已有的 `import java.util.List;` 可能没有，需要添加）。

同时需要确认 `InterviewQa` 的 `getQuestionType()` 字段——追问的 `questionType` 为 1，普通题为 0。

---

### Task 3: 前端 — 重写 SpecialTraining.vue

**Files:**
- Rewrite: `front/src/views/interview/SpecialTraining.vue`

- [ ] **Step 1: 完整重写 SpecialTraining.vue**

```vue
<template>
  <div class="special-training">
    <div class="page-header">
      <div>
        <h1>专项训练</h1>
      </div>
      <div>
        <el-button v-if="store.sessionId && !store.isEnded" type="danger" plain
          @click="endTraining" :loading="store.loading">
          结束训练
        </el-button>
      </div>
    </div>

    <!-- 阶段 A: 设置 -->
    <div v-if="!store.sessionId" class="setup-section">
      <div class="count-setting">
        <h2>设置题目数量</h2>
        <el-input-number v-model="targetCount" :min="1" :max="20" />
        <span class="count-hint">共 {{ targetCount }} 题</span>
      </div>
      <div class="resume-hint">
        <p>AI 将基于你的简历内容（技术栈、项目经验）出题</p>
      </div>
      <el-button type="primary" size="large" @click="startTraining" :loading="store.loading">
        开始训练
      </el-button>
    </div>

    <!-- 阶段 B: 答题中 -->
    <template v-else-if="!store.isEnded">
      <div class="progress-bar">
        <el-progress :percentage="progressPercent" :stroke-width="8" />
        <span class="progress-text">{{ store.qaList.length }} / {{ store.targetCount }}</span>
      </div>

      <div v-if="questionLoading" class="loading-state">
        <el-skeleton :rows="3" animated />
        <p class="loading-text">AI 面试官正在分析简历并出题中，请稍候...</p>
      </div>

      <div v-if="store.currentQa && !questionLoading" class="qa-card">
        <div class="qa-header">
          <el-tag type="success" size="small">专项题 {{ store.currentQa.sortOrder }}</el-tag>
          <span class="qa-time">{{ new Date(store.currentQa.createdAt).toLocaleTimeString() }}</span>
        </div>
        <div class="qa-question">{{ store.currentQa.question }}</div>

        <div v-if="!store.currentQa.userAnswer" class="qa-answer-area">
          <el-input v-model="currentAnswer" type="textarea" :rows="4" placeholder="输入你的回答..." />
          <div class="qa-actions">
            <el-button type="primary" @click="submitAnswer" :loading="store.submitting" :disabled="store.submitting">
              提交回答
            </el-button>
          </div>
        </div>

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
            <el-button v-else type="success" @click="goToReview">查看复盘</el-button>
          </div>
        </div>
      </div>

      <div v-if="store.qaList.length > 0" class="history-section">
        <h3>已回答 ({{ store.qaList.length }})</h3>
        <div v-for="qa in store.qaList" :key="qa.id" class="history-item">
          <el-tag type="success" size="small" class="qa-type-tag">Q{{ qa.sortOrder }}</el-tag>
          <span class="history-question">{{ qa.question }}</span>
        </div>
      </div>
    </template>

    <!-- 阶段 C: 已结束 -->
    <div v-else class="end-section">
      <el-result icon="success" title="训练已结束" :sub-title="`共完成 ${store.qaList.length} 题`">
        <template #extra>
          <el-button type="primary" @click="goToReview">查看复盘</el-button>
          <el-button @click="resetAndRestart">再来一次</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useInterviewStore } from '../../stores/interview'
import { useRouter } from 'vue-router'

const router = useRouter()
const store = useInterviewStore()
const currentAnswer = ref('')
const questionLoading = ref(false)
const targetCount = ref(5)

onMounted(() => {
  if (store.sessionId && store.sessionType !== 2) {
    store.reset()
  }
})

const progressPercent = computed(() => {
  if (!store.targetCount) return 0
  return Math.round((store.qaList.length / store.targetCount) * 100)
})

async function startTraining() {
  questionLoading.value = true
  try {
    await store.startSession(2, targetCount.value)
    await store.ask()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '创建训练失败，请重试')
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
      ElMessage.warning('训练已结束')
      await store.end()
    }
  } catch {
    ElMessage.error('获取下一题失败')
  } finally {
    questionLoading.value = false
  }
}

async function endTraining() {
  try {
    await store.end()
  } catch { /* ignore */ }
  ElMessage.success('训练已结束')
  store.reset()
}

function goToReview() {
  router.push('/interview/history')
}

async function resetAndRestart() {
  store.reset()
  targetCount.value = 5
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); }
.setup-section { max-width: 500px; margin: 0 auto; text-align: center; padding-top: 40px; }
.setup-section h2 { font-size: 18px; font-weight: 600; margin-bottom: 16px; color: var(--text-primary); }
.count-setting { margin-bottom: 24px; }
.count-hint { margin-left: 12px; font-size: 14px; color: var(--text-secondary); }
.resume-hint { margin-bottom: 32px; }
.resume-hint p { font-size: 14px; color: var(--text-secondary); background: rgba(14,165,233,0.06); padding: 12px 20px; border-radius: 8px; display: inline-block; }
.progress-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; }
.progress-bar .el-progress { flex: 1; }
.progress-text { font-size: 14px; font-weight: 600; color: var(--text-primary); white-space: nowrap; }
.end-section { margin-top: 60px; }
.qa-card {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 24px; margin-bottom: 24px;
}
.qa-header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.qa-time { font-size: 12px; color: var(--text-muted); }
.qa-question { font-size: 16px; font-weight: 500; line-height: 1.6; margin-bottom: 20px; }
.qa-actions { margin-top: 12px; display: flex; gap: 8px; }
.qa-result .label { font-size: 13px; font-weight: 600; color: var(--text-secondary); margin-bottom: 4px; }
.qa-result p { font-size: 14px; line-height: 1.6; color: var(--text-primary); }
.qa-answer, .qa-evaluation { margin-bottom: 16px; padding: 12px; background: rgba(0,0,0,0.02); border-radius: 8px; }
.qa-reference {
  margin-bottom: 16px; padding: 12px;
  background: rgba(0,0,0,0.02); border-radius: 8px;
  border-left: 3px solid #10b981;
}
.history-section { margin-top: 32px; }
.history-section h3 { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.history-item { display: flex; align-items: center; gap: 8px; padding: 10px 0; border-bottom: 1px solid rgba(0,0,0,0.04); }
.history-question { font-size: 13px; color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.qa-type-tag { flex-shrink: 0; }
.loading-state { margin-top: 60px; text-align: center; }
.loading-state .el-skeleton { max-width: 500px; margin: 0 auto; }
.loading-text { margin-top: 16px; font-size: 14px; color: var(--text-secondary); }
</style>
```

---

### Task 4: 验证构建

- [ ] **Step 1: 后端编译检查**

```bash
cd java && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 前端构建检查**

```bash
cd front && npm run build
```

Expected: 无报错

- [ ] **Step 3: 功能测试（手动）**

1. 启动后端 + 前端
2. 在「我的简历」上传一份 PDF 简历
3. 进入「专项训练」
4. 验证：显示题数选择 + 提示基于简历出题
5. 选择题数（如 3 题），点击开始
6. 验证：AI 生成的题目与简历技术栈相关
7. 回答并提交
8. 验证：显示 AI 评价 + 参考答案
9. 验证：支持追问
10. 验证：进度正确
11. 重复至目标题数，验证自动结束
12. 验证：可查看复盘
