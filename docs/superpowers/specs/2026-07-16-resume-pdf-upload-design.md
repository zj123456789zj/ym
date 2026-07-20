# 简历 PDF 上传与预览功能设计

## 概述

将「我的简历」页面从文本输入改为 PDF 上传 + 嵌入式预览。用户上传简历 PDF 后，前端直接用 pdf.js 渲染展示原件，不可编辑。

## 页面布局

### 无简历时
- 上传区域：支持点击选择和拖拽上传 PDF 文件
- 文件类型限制：仅 `.pdf`，最大 10MB
- 上传后自动跳转到预览状态

### 已有简历时
- PDF 预览区域：使用 `vue-pdf-embed` 渲染 PDF 所有页面，支持翻页/缩放
- 底部操作栏：「重新上传」按钮
- 去除原有的文本输入框和保存按钮

## 前端改动

### ResumeEdit.vue（重写）
- 移除 `el-input type="textarea"` 和保存按钮
- 新增状态：`idle`（无简历展示上传区）、`loaded`（展示 PDF 预览）
- 引入 `vue-pdf-embed` 组件
- 上传通过 `multipart/form-data` 调用后端 API

### api/resume.js
- 新增 `uploadResume(file)` → `POST /api/resume/upload`
- `getResume()` 返回数据新增 `pdfUrl` 字段
- 移除非必要的 `saveResume` 导出（不再需要）

### 新增依赖
- `vue-pdf-embed`（基于 pdf.js）

## 后端改动

### ResumeController.java
- 新增 `POST /api/resume/upload`：接收 MultipartFile，保存 PDF，更新数据库，返回 pdfUrl
- `GET /api/resume`：返回数据包含新增的 `pdfUrl` 字段

### ResumeService.java / ResumeServiceImpl.java
- 新增 `uploadResume(userId, MultipartFile)` 方法
- PDF 存储路径：`uploads/resumes/{userId}/{uuid}.pdf`
- 文件命名使用 UUID 避免冲突
- 原有 `saveResume` 方法可保留（兼容旧数据）或标记废弃

### Resume.java（实体类）
- 新增字段 `pdfPath`（对应数据库 `pdf_path` 列）

### 数据库
- `resume` 表新增 `pdf_path` 字段

### application.yml
- 新增 multipart 上传限制

## 文件结构

```
uploads/
  resumes/
    {userId}/
      {uuid}.pdf
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/resume/upload` | 上传 PDF 简历（multipart/form-data） |
| GET | `/api/resume` | 获取简历信息（含 pdfUrl） |

## 不涉及的改动
- 简历优化页面（ResumeOptimize.vue）暂不改动
- 后端 AI 优化功能暂不改动（后续可从 PDF 提取文本供 AI 使用）
