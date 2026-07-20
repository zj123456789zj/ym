<template>
  <div class="overview">
    <div class="page-header">
      <div>
        <h1>总览</h1>
      </div>
    </div>

    <div class="stat-grid">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="label">{{ s.label }}</div>
        <div class="number">{{ s.value }}</div>
      </div>
    </div>

    <div class="section-header">
      <h2>快捷入口</h2>
    </div>
    <div class="action-grid">
      <div class="action-card" @click="$router.push('/interview/mock')">
        <div class="icon-big blue">🎯</div>
        <div class="action-title">开始模拟面试</div>
        <div class="action-desc">AI 一对一面试，含追问</div>
      </div>
      <div class="action-card" @click="$router.push('/interview/special')">
        <div class="icon-big gold">⚡</div>
        <div class="action-title">专项训练</div>
        <div class="action-desc">根据简历内容精准出题</div>
      </div>
      <div class="action-card" @click="$router.push('/resume/optimize')">
        <div class="icon-big green">✨</div>
        <div class="action-title">简历优化</div>
        <div class="action-desc">AI 评分 + 修改建议</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { getOverview } from '../../api/statistics'

const userStore = useUserStore()
const stats = ref([
  { label: '模拟面试', value: 0 },
  { label: '总题数', value: 0 },
  { label: '复盘数', value: 0 },
  { label: '错题数', value: 0 }
])

onMounted(async () => {
  try {
    const res = await getOverview()
    if (res.data) {
      stats.value[0].value = res.data.totalInterviews || 0
      stats.value[1].value = res.data.totalQuestions || 0
      stats.value[2].value = res.data.totalReviews || 0
      stats.value[3].value = res.data.totalWrongQuestions || 0
    }
  } catch (e) {}
})
</script>

<style scoped>
.overview { }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; }
.page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); }
.page-header .sub { font-size: 14px; color: var(--text-secondary); margin-top: 4px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 32px; }
.stat-card {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 22px 24px; transition: all 0.3s ease;
  position: relative; overflow: hidden;
}
.stat-card::before {
  content: ''; position: absolute; top: 0; left: 0;
  width: 4px; height: 100%;
}
.stat-card:nth-child(1)::before { background: var(--flow-blue); }
.stat-card:nth-child(2)::before { background: var(--coral-gold); }
.stat-card:nth-child(3)::before { background: var(--success); }
.stat-card:nth-child(4)::before { background: #8B5CF6; }
.stat-card:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(11,29,58,0.10); }
.stat-card .label { font-size: 13px; color: var(--text-secondary); font-weight: 500; margin-bottom: 8px; }
.stat-card .number { font-size: 32px; font-weight: 700; color: var(--text-primary); }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-header h2 { font-size: 17px; font-weight: 600; color: var(--text-primary); }
.action-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.action-card {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 24px; text-align: center; cursor: pointer;
  transition: all 0.3s ease;
}
.action-card:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(11,29,58,0.10); border-color: var(--flow-blue); }
.icon-big {
  width: 52px; height: 52px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; margin: 0 auto 14px;
}
.icon-big.blue { background: rgba(14,165,233,0.12); }
.icon-big.gold { background: rgba(245,158,11,0.12); }
.icon-big.green { background: rgba(16,185,129,0.12); }
.action-title { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.action-desc { font-size: 13px; color: var(--text-secondary); }
</style>
