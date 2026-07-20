# 学习趋势折线图实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在数据统计页面的"学习趋势"区域添加 ECharts 折线图，支持自定义日期范围选择并联动统计卡片数据。

**Architecture:** 后端新增 /api/statistics/trend 接口对 4 张表按天分组计数，前端使用 vue-echarts 渲染 4 条折线，日期范围选择器联动图表和统计卡片。

**Tech Stack:** Spring Boot + MyBatis-Plus (后端), Vue 3 + Element Plus + ECharts 5 + vue-echarts 6 (前端)

---

### Task 1: 后端 — StatisticsService 接口新增 getTrend 方法

**Files:**
- Modify: `java/src/main/java/com/yumian/service/StatisticsService.java`

- [ ] **Step 1: 添加 getTrend 方法签名**

在 `StatisticsService.java` 中添加方法声明：

```java
package com.yumian.service;

import java.util.Map;

public interface StatisticsService {
    Map<String, Object> getOverview(Long userId);
    Map<String, Object> getTrend(Long userId, String startDate, String endDate);
}
```

---

### Task 2: 后端 — StatisticsServiceImpl 实现 getTrend 方法

**Files:**
- Modify: `java/src/main/java/com/yumian/service/impl/StatisticsServiceImpl.java`

- [ ] **Step 1: 注入 InterviewQaMapper**

```java
import com.yumian.mapper.InterviewQaMapper;

public class StatisticsServiceImpl implements StatisticsService {
    private final InterviewSessionMapper sessionMapper;
    private final InterviewReviewMapper reviewMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final InterviewQaMapper interviewQaMapper;

    // constructor injection via @RequiredArgsConstructor
    // Lombok will auto-inject if field is final
}
```

改成：

```java
private final InterviewSessionMapper sessionMapper;
private final InterviewReviewMapper reviewMapper;
private final WrongQuestionMapper wrongQuestionMapper;
private final InterviewQaMapper interviewQaMapper;
```

- [ ] **Step 2: 实现 getTrend 方法**

```java
@Override
public Map<String, Object> getTrend(Long userId, String startDate, String endDate) {
    // 1. 生成日期列表
    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);
    List<String> dates = new ArrayList<>();
    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
        dates.add(d.toString());
    }

    // 2. 辅助方法：按天分组计数
    Map<String, Long> interviews = countByDate(sessionMapper, userId, startDate, endDate);
    Map<String, Long> reviews = countByDate(reviewMapper, userId, startDate, endDate);
    Map<String, Long> questions = countByDate(interviewQaMapper, userId, startDate, endDate);
    Map<String, Long> wrongQuestions = countByDate(wrongQuestionMapper, userId, startDate, endDate);

    // 3. 组装结果（补零）
    List<Long> interviewList = fillZeros(dates, interviews);
    List<Long> reviewList = fillZeros(dates, reviews);
    List<Long> questionList = fillZeros(dates, questions);
    List<Long> wrongList = fillZeros(dates, wrongQuestions);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("dates", dates);
    result.put("totalInterviews", interviewList);
    result.put("totalReviews", reviewList);
    result.put("totalQuestions", questionList);
    result.put("totalWrongQuestions", wrongList);
    return result;
}

private Map<String, Long> countByDate(BaseMapper<?> mapper, Long userId, String startDate, String endDate) {
    String tableName = getTableName(mapper);
    // 使用原生 SQL 因为 MyBatis-Plus LambdaQueryWrapper 不支持 GROUP BY
    List<Map<String, Object>> rows = mapper.selectMaps(
        new QueryWrapper<>()
            .select("DATE(created_at) as date, COUNT(*) as cnt")
            .eq("user_id", userId)
            .between("created_at", startDate + " 00:00:00", endDate + " 23:59:59")
            .groupBy("DATE(created_at)")
            .orderByAsc("DATE(created_at)")
    );
    Map<String, Long> map = new LinkedHashMap<>();
    for (Map<String, Object> row : rows) {
        map.put(row.get("date").toString(), ((Number) row.get("cnt")).longValue());
    }
    return map;
}

private List<Long> fillZeros(List<String> dates, Map<String, Long> data) {
    List<Long> result = new ArrayList<>();
    for (String date : dates) {
        result.add(data.getOrDefault(date, 0L));
    }
    return result;
}
```

注意：`BaseMapper<?>` 需要添加 `import com.baomidou.mybatisplus.core.mapper.BaseMapper;`。同时 `QueryWrapper` 需要 `import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;`。

需要在文件顶部添加的 import：

```java
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yumian.mapper.InterviewQaMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
```

- [ ] **Step 3: 添加日期范围的统计汇总方法**

新增 `getTrendOverview` 方法供前端联动统计卡片使用（统计选定范围内的总数而不是每日明细）：

```java
@Override
public Map<String, Object> getOverview(Long userId, String startDate, String endDate) {
    Map<String, Object> stats = getTrend(userId, startDate, endDate);
    Map<String, Object> result = new LinkedHashMap<>();
    
    long sum = 0;
    for (Long v : (List<Long>) stats.get("totalInterviews")) sum += v;
    result.put("totalInterviews", sum);
    
    sum = 0;
    for (Long v : (List<Long>) stats.get("totalReviews")) sum += v;
    result.put("totalReviews", sum);
    
    sum = 0;
    for (Long v : (List<Long>) stats.get("totalQuestions")) sum += v;
    result.put("totalQuestions", sum);
    
    sum = 0;
    for (Long v : (List<Long>) stats.get("totalWrongQuestions")) sum += v;
    result.put("totalWrongQuestions", sum);
    
    return result;
}
```

再到 `StatisticsService.java` 添加对应签名：

```java
Map<String, Object> getOverview(Long userId, String startDate, String endDate);
```

---

### Task 3: 后端 — StatisticsController 新增 /trend 端点

**Files:**
- Modify: `java/src/main/java/com/yumian/controller/StatisticsController.java`

- [ ] **Step 1: 添加 /trend 和 /overview/range 端点**

```java
@GetMapping("/trend")
public Result<Map<String, Object>> getTrend(
        @RequestAttribute("userId") Long userId,
        @RequestParam String startDate,
        @RequestParam String endDate) {
    return Result.success(statisticsService.getTrend(userId, startDate, endDate));
}

@GetMapping("/overview/range")
public Result<Map<String, Object>> getOverviewRange(
        @RequestAttribute("userId") Long userId,
        @RequestParam String startDate,
        @RequestParam String endDate) {
    return Result.success(statisticsService.getOverview(userId, startDate, endDate));
}
```

---

### Task 4: 前端 — 新增 API 方法

**Files:**
- Modify: `front/src/api/statistics.js`

- [ ] **Step 1: 添加 getTrend 和 getOverviewRange**

```javascript
import request from './request'

export const getOverview = () => request.get('/statistics/overview')

export const getTrend = (startDate, endDate) =>
    request.get('/statistics/trend', { params: { startDate, endDate } })

export const getOverviewRange = (startDate, endDate) =>
    request.get('/statistics/overview/range', { params: { startDate, endDate } })
```

---

### Task 5: 前端 — 重写 Statistics.vue 的图表区域

**Files:**
- Modify: `front/src/views/statistics/Statistics.vue`

- [ ] **Step 1: 替换 template 图表占位为日期选择器 + v-chart**

```vue
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
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-value="defaultRange"
          value-format="YYYY-MM-DD"
          @change="onDateRangeChange"
        />
      </div>
      <div class="chart-wrapper">
        <v-chart :option="chartOption" style="height: 380px" autoresize />
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: 更新 script setup — 导入和注册 ECharts**

```vue
<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview, getTrend, getOverviewRange } from '../../api/statistics'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, TooltipComponent, LegendComponent, GridComponent, DataZoomComponent, CanvasRenderer])

const LINE_COLORS = ['#0EA5E9', '#F59E0B', '#10B981', '#8B5CF6']
const LINE_NAMES = ['模拟面试', '复盘次数', '答题数', '错题数']

const defaultRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 29)
  return [start, end]
}

const dateRange = ref(defaultRange())

// 统计卡片（累计）
const stats = ref([
  { label: '模拟面试', value: 0, key: 'totalInterviews', rangeLabel: '' },
  { label: '复盘次数', value: 0, key: 'totalReviews', rangeLabel: '' },
  { label: '答题数', value: 0, key: 'totalQuestions', rangeLabel: '' },
  { label: '错题数', value: 0, key: 'totalWrongQuestions', rangeLabel: '' }
])

// 趋势数据
const trendData = ref(null)

// ECharts option
const chartOption = computed(() => {
  if (!trendData.value) return {}
  
  const series = ['totalInterviews', 'totalReviews', 'totalQuestions', 'totalWrongQuestions'].map((key, i) => ({
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
    // 加载默认趋势
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
  const trendRes = await getTrend(start, end)
  if (trendRes.data) {
    trendData.value = trendRes.data
  }
  // 联动统计卡片 → 显示所选范围内的聚合值
  const rangeRes = await getOverviewRange(start, end)
  if (rangeRes.data) {
    const days = dateDiff(start, end) + 1
    stats.value.forEach(s => {
      s.value = rangeRes.data[s.key] || 0
      s.rangeLabel = `· 近${days}天`
    })
  }
}

function dateDiff(start, end) {
  const d1 = new Date(start), d2 = new Date(end)
  return Math.floor((d2 - d1) / (86400000))
}
</script>
```

- [ ] **Step 3: 更新样式**

在原有 `<style scoped>` 中替换/添加：

```css
.chart-section { margin-top: 32px; }
.chart-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.chart-header h3 { font-size: 17px; font-weight: 600; margin: 0; }
.chart-wrapper {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 24px 16px 8px;
}
.stat-card .number { font-size: 32px; font-weight: 700; color: var(--text-primary); }
.range-label {
  font-size: 12px; font-weight: 400;
  color: var(--text-muted); margin-left: 4px;
}
```

---

## 自检

1. **Spec 覆盖:** 所有 spec 中的需求均有对应 task：趋势接口(Task 1-3)、日期选择器(Task 5)、折线图配置(Task 5)、联动逻辑(Task 5)、补零逻辑(Task 2)。
2. **Placeholder 扫描:** 无 TBD/TODO。
3. **类型一致性:** 所有接口参数名和 spec 一致（startDate/endDate），返回字段名一致（totalInterviews 等）。
