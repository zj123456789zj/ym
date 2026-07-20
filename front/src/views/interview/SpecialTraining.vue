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
