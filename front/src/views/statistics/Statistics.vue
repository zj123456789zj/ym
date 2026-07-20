<template>
  <div class="statistics">
    <div class="page-header">
      <div>
        <h1>数据统计</h1>
      </div>
    </div>

    <div class="stat-grid">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="label">{{ s.label }}</div>
        <div class="number">
          {{ s.value }}
          <span v-if="s.rangeLabel" class="range-label">{{ s.rangeLabel }}</span>
        </div>
      </div>
    </div>

    <div class="chart-section">
      <div class="chart-header">
        <h3>学习趋势</h3>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          size="small"
          :editable="false"
          @change="onDateRangeChange"
        />
      </div>
      <div class="chart-wrapper">
        <v-chart :option="chartOption" style="height: 380px" autoresize />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview, getTrend, getOverviewRange } from '../../api/statistics'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const LINE_COLORS = ['#0EA5E9', '#F59E0B', '#10B981', '#8B5CF6']
const LINE_NAMES = ['模拟面试', '复盘次数', '答题数', '错题数']
const LINE_KEYS = ['totalInterviews', 'totalReviews', 'totalQuestions', 'totalWrongQuestions']

const now = new Date()
const defaultStart = new Date(now)
defaultStart.setDate(defaultStart.getDate() - 29)
const defaultEnd = new Date(now)
const dateRange = ref([formatDate(defaultStart), formatDate(defaultEnd)])

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const stats = ref([
  { label: '模拟面试', value: 0, key: 'totalInterviews', rangeLabel: '' },
  { label: '复盘次数', value: 0, key: 'totalReviews', rangeLabel: '' },
  { label: '答题数', value: 0, key: 'totalQuestions', rangeLabel: '' },
  { label: '错题数', value: 0, key: 'totalWrongQuestions', rangeLabel: '' }
])

const trendData = ref(null)

const chartOption = computed(() => {
  if (!trendData.value) return {}

  const series = LINE_KEYS.map((key, i) => ({
    name: LINE_NAMES[i],
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { width: 2.5 },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: LINE_COLORS[i] + '40' },
          { offset: 1, color: LINE_COLORS[i] + '05' }
        ]
      }
    },
    itemStyle: { color: LINE_COLORS[i] },
    data: trendData.value[key]
  }))

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderWidth: 0,
      borderRadius: 8,
      boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
      formatter: (params) => {
        let html = `<div style="font-weight:600;margin-bottom:6px">${params[0].axisValue}</div>`
        params.forEach(p => {
          html += `<div style="display:flex;align-items:center;gap:8px;margin:3px 0">
            <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color}"></span>
            <span>${p.seriesName}：<strong>${p.value}</strong></span>
          </div>`
        })
        return html
      }
    },
    legend: {
      top: 0,
      selectedMode: 'multiple',
      textStyle: { fontSize: 13 }
    },
    grid: {
      left: 40, right: 20, top: 50, bottom: 30
    },
    xAxis: {
      type: 'category',
      data: trendData.value.dates,
      boundaryGap: false,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { fontSize: 11, color: '#94A3B8' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
      axisLabel: { fontSize: 11, color: '#94A3B8' }
    },
    series
  }
})

onMounted(async () => {
  try {
    const res = await getOverview()
    if (res.data) {
      stats.value.forEach(s => {
        s.value = res.data[s.key] || 0
      })
    }
    await loadTrend()
  } catch (e) {}
})

async function onDateRangeChange() {
  if (dateRange.value) {
    await loadTrend()
  }
}

async function loadTrend() {
  if (!dateRange.value) return
  const [start, end] = dateRange.value
  try {
    const trendRes = await getTrend(start, end)
    if (trendRes.data) {
      trendData.value = trendRes.data
    }
    const rangeRes = await getOverviewRange(start, end)
    if (rangeRes.data) {
      const d1 = new Date(start), d2 = new Date(end)
      const days = Math.floor((d2 - d1) / 86400000) + 1
      stats.value.forEach(s => {
        s.value = rangeRes.data[s.key] || 0
        s.rangeLabel = `· 近${days}天`
      })
    }
  } catch (e) {}
}
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); }
.page-header .sub { font-size: 14px; color: var(--text-secondary); margin-top: 4px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 32px; }
.stat-card {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 22px 24px; position: relative; overflow: hidden;
}
.stat-card::before {
  content: ''; position: absolute; top: 0; left: 0;
  width: 4px; height: 100%;
}
.stat-card:nth-child(1)::before { background: var(--flow-blue); }
.stat-card:nth-child(2)::before { background: var(--coral-gold); }
.stat-card:nth-child(3)::before { background: var(--success); }
.stat-card:nth-child(4)::before { background: #8B5CF6; }
.stat-card .label { font-size: 13px; color: var(--text-secondary); font-weight: 500; margin-bottom: 8px; }
.stat-card .number { font-size: 32px; font-weight: 700; color: var(--text-primary); }
.range-label {
  font-size: 12px; font-weight: 400;
  color: var(--text-muted); margin-left: 4px;
}
.chart-section { margin-top: 32px; }
.chart-header {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 16px;
}
.chart-header h3 { font-size: 17px; font-weight: 600; margin: 0; }
.chart-header :deep(.el-date-editor) { width: 50%; flex-shrink: 0; }
.chart-wrapper {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 24px 16px 8px;
}
</style>
