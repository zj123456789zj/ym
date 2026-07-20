# 学习趋势折线图设计

## 背景

数据统计页面的"学习趋势"区域目前为 `el-empty` 占位。用户希望用折线图将 4 个核心指标（模拟面试、复盘次数、答题数、错题数）的趋势可视化，并且支持自定义日期范围联动统计卡片。

## 变更范围

### 前端：Statistics.vue

将 `chart-placeholder` 替换为：

1. **日期范围选择器** — `el-date-picker` 的 daterange 类型，默认最近 30 天
2. **ECharts 折线图** — 使用已安装的 `vue-echarts` + `echarts`

**图表配置：**

| 属性 | 值 |
|------|----|
| type | line（4 条折线） |
| smooth | true |
| areaStyle | 半透明面积填充 |
| legend | 顶部，点击切换系列，联动统计卡片高亮 |
| tooltip | 悬浮显示该日全部 4 个指标值 |
| xAxis | 时间轴，日期标签 |
| colors | interviews:`#0EA5E9`, reviews:`#F59E0B`, questions:`#10B981`, wrong:`#8B5CF6` |

**联动逻辑：**

- 日期范围变化 → 重新请求 trend API
- 更新折线图
- 同时更新 4 个统计卡片为所选范围内的聚合值，值旁加标签如"· 近30天"

### 后端：新增趋势接口

**`GET /api/statistics/trend`**

参数：
- `startDate` (String, yyyy-MM-dd)
- `endDate` (String, yyyy-MM-dd)
- `userId` (从 RequestAttribute 取)

返回格式：

```json
{
  "dates": ["2026-07-01", "2026-07-02", ...],
  "totalInterviews": [1, 0, 2, ...],
  "totalReviews": [0, 1, 0, ...],
  "totalQuestions": [5, 8, 12, ...],
  "totalWrongQuestions": [2, 1, 3, ...]
}
```

**数据来源（按 created_at 按天分组计数）：**

| 字段 | 表 |
|------|----|
| totalInterviews | interview_session |
| totalReviews | interview_review |
| totalQuestions | interview_qa |
| totalWrongQuestions | wrong_question |

**补零逻辑：** 返回结果中覆盖 startDate~endDate 所有日期，无数据的日期补 0。

**涉及变更文件：**

| 文件 | 变更 |
|------|------|
| `StatisticsService.java` | 新增 `getTrend()` 方法 |
| `StatisticsServiceImpl.java` | 注入 `InterviewQaMapper`，实现按天分组查询+补零 |
| `StatisticsController.java` | 新增 `/trend` 端点 |
| `Statistics.vue` | 替换图表占位，添加日期选择器和 chart 组件 |
| `statistics.js` | 新增 `getTrend(startDate, endDate)` API 方法 |

### 不变部分

- 原 `/overview` 接口不变（仍返回累计总数，首次加载时使用）
- 侧边栏、路由不变
- 无新增 npm 依赖（echarts + vue-echarts 已安装）
