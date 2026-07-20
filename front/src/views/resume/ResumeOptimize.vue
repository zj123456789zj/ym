<template>
  <div class="resume-optimize">
    <div class="page-header">
      <div>
        <h1>简历优化</h1>
      </div>
      <el-button type="primary" @click="handleOptimize" :loading="optimizing">
        {{ optimizing ? 'AI 分析中...' : '开始优化' }}
      </el-button>
    </div>

    <el-alert v-if="!hasResume" :title="'请先在「我的简历」中填写简历内容'" type="warning" show-icon :closable="false" />

    <div v-if="result" class="result-section">
      <div class="score-card">
        <div class="score-label">AI 评分</div>
        <div class="score-value">{{ result.score || 85 }}</div>
        <div class="score-desc">百分制</div>
      </div>
      <div class="suggestions-card">
        <h3>优化建议</h3>
        <p class="ai-content">{{ result.suggestions }}</p>
      </div>
    </div>

    <el-empty v-else-if="!optimizing && hasResume" :description="'点击「开始优化」按钮，AI 将分析你的简历'" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { optimizeResume } from '../../api/resume'
import { getResume } from '../../api/resume'

const optimizing = ref(false)
const hasResume = ref(false)
const result = ref(null)

onMounted(async () => {
  try {
    const res = await getResume()
    hasResume.value = !!(res.data && res.data.rawContent)
  } catch (e) {}
})

async function handleOptimize() {
  optimizing.value = true
  try {
    const res = await optimizeResume()
    result.value = res.data
  } catch (e) {} finally {
    optimizing.value = false
  }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); }
.page-header .sub { font-size: 14px; color: var(--text-secondary); margin-top: 4px; }
.result-section { display: grid; grid-template-columns: 200px 1fr; gap: 24px; margin-top: 24px; }
.score-card {
  background: linear-gradient(135deg, var(--flow-blue), #38BDF8);
  border-radius: 16px; padding: 32px; text-align: center; color: #fff;
}
.score-label { font-size: 14px; opacity: 0.9; }
.score-value { font-size: 56px; font-weight: 700; margin: 8px 0; }
.score-desc { font-size: 13px; opacity: 0.8; }
.suggestions-card {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 24px;
}
.suggestions-card h3 { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.ai-content { font-size: 14px; line-height: 1.8; color: var(--text-secondary); white-space: pre-wrap; }
</style>
