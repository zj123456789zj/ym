<template>
  <div class="history">
    <div class="page-header">
      <div>
        <h1>面试历史</h1>
      </div>
    </div>

    <div v-if="list.length === 0" class="empty-state">
      <el-empty description="还没有面试记录" />
    </div>

    <div v-else class="history-list">
      <div v-for="item in list" :key="item.id" class="history-card" @click="$router.push('/interview/review/' + item.id)">
        <div class="card-top">
          <el-tag :type="item.type === 1 ? 'primary' : 'success'" size="small">
            {{ item.type === 1 ? '模拟面试' : '专项训练' }}
          </el-tag>
          <span class="card-time">{{ new Date(item.createdAt).toLocaleDateString() }}</span>
        </div>
        <div class="card-mid">
          <span>共 {{ item.questionCount || 0 }} 题</span>
          <span v-if="item.overallScore" class="card-score">{{ item.overallScore }} 分</span>
          <el-tag v-else size="small" type="info">未评分</el-tag>
        </div>
        <div class="card-footer">点击查看复盘 →</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHistory } from '../../api/interview'

const list = ref([])

onMounted(async () => {
  try {
    const res = await getHistory()
    list.value = res.data || []
  } catch (e) {}
})
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); }
.page-header .sub { font-size: 14px; color: var(--text-secondary); margin-top: 4px; }
.empty-state { margin-top: 60px; }
.history-list { display: grid; gap: 16px; }
.history-card {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 20px 24px; cursor: pointer; transition: all 0.3s ease;
}
.history-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(11,29,58,0.08); }
.card-top { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.card-time { font-size: 12px; color: var(--text-muted); }
.card-mid { display: flex; align-items: center; gap: 16px; font-size: 14px; color: var(--text-secondary); }
.card-score { font-size: 20px; font-weight: 700; color: var(--flow-blue); }
.card-footer { margin-top: 12px; font-size: 12px; color: var(--flow-blue); }
</style>
