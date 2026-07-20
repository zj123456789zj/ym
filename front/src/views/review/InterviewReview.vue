<template>
  <div class="review-page">
    <div class="page-header">
      <div>
        <h1>面试复盘</h1>
      </div>
      <el-button type="primary" @click="handleGenerate" :loading="loading" v-if="!review">
        生成复盘
      </el-button>
    </div>

    <div v-if="loading && !review" class="loading-state">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="!review && !loading" class="empty-state">
      <el-empty description="暂无复盘数据，点击生成" />
    </div>

    <div v-else-if="review" class="review-content">
      <div class="review-section">
        <h3>📝 面试总结</h3>
        <p>{{ review.summary }}</p>
      </div>
      <div class="review-grid">
        <div class="review-section">
          <h3>✅ 表现好的方面</h3>
          <ul><li v-for="(s, i) in parseArray(review.strengths)" :key="i">{{ s }}</li></ul>
        </div>
        <div class="review-section">
          <h3>⚠️ 不足之处</h3>
          <ul><li v-for="(w, i) in parseArray(review.weaknesses)" :key="i">{{ w }}</li></ul>
        </div>
      </div>
      <div class="review-grid">
        <div class="review-section">
          <h3>💡 改进建议</h3>
          <ul><li v-for="(s, i) in parseArray(review.suggestions)" :key="i">{{ s }}</li></ul>
        </div>
        <div class="review-section">
          <h3>📚 知识盲区</h3>
          <ul><li v-for="(g, i) in parseArray(review.knowledgeGaps)" :key="i">{{ g }}</li></ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getReview, generateReview } from '../../api/review'

const route = useRoute()
const sessionId = route.params.id
const review = ref(null)
const loading = ref(false)

onMounted(async () => {
  try {
    const res = await getReview(sessionId)
    review.value = res.data
  } catch (e) {}
})

function parseArray(str) {
  if (!str) return []
  try { return JSON.parse(str) } catch { return [str] }
}

async function handleGenerate() {
  loading.value = true
  try {
    const res = await generateReview(sessionId)
    review.value = res.data
  } catch (e) {} finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); }
.page-header .sub { font-size: 14px; color: var(--text-secondary); margin-top: 4px; }
.empty-state, .loading-state { margin-top: 60px; }
.review-section {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 24px; margin-bottom: 20px;
}
.review-section h3 { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.review-section p { font-size: 14px; line-height: 1.8; color: var(--text-secondary); }
.review-section ul { padding-left: 20px; }
.review-section li { font-size: 14px; line-height: 1.8; color: var(--text-secondary); }
.review-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
</style>
