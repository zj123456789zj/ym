# 简历 PDF 上传与预览实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将「我的简历」从文本输入改为 PDF 上传 + 嵌入式只读预览

**Architecture:** 前端上传 PDF -> 后端保存文件至磁盘并记录路径 -> 前端通过带认证的 API 获取 PDF 字节流 -> vue-pdf-embed 渲染展示。不保留文本编辑能力。

**Tech Stack:** Spring Boot 3.2 (multipart upload), Vue 3 + vue-pdf-embed (pdf.js), MySQL (resume 表新增 pdf_path 字段)

---

## 文件结构

### 修改的文件

| 文件 | 改动 |
|------|------|
| `java/src/main/java/com/yumian/entity/Resume.java` | 新增 `pdfPath` 字段 |
| `java/src/main/java/com/yumian/service/ResumeService.java` | 新增 `uploadResume`、`getPdfBytes` 方法 |
| `java/src/main/java/com/yumian/service/impl/ResumeServiceImpl.java` | 实现 PDF 文件存储与读取 |
| `java/src/main/java/com/yumian/controller/ResumeController.java` | 新增 `/upload` 和 `/pdf` 端点 |
| `java/src/main/resources/application.yml` | 添加 multipart 上传限制 |
| `java/init.sql` | 新增 `pdf_path` 列 |
| `front/package.json` | 添加 `vue-pdf-embed` 依赖 |
| `front/src/api/resume.js` | 新增 `uploadResume` 和 `getResumePdf` |
| `front/src/views/resume/ResumeEdit.vue` | 完整重写：上传区域 + PDF 预览 |

---

### Task 1: 后端 — 数据库与实体

**Files:**
- Modify: `java/src/main/java/com/yumian/entity/Resume.java`
- Modify: `java/init.sql`

- [ ] **Step 1: Resume 实体新增 pdfPath 字段**

```java
// Resume.java — 新增字段
private String pdfPath;
```

- [ ] **Step 2: init.sql 新增 pdf_path 列**

```sql
ALTER TABLE `resume` ADD COLUMN `pdf_path` VARCHAR(255) DEFAULT NULL COMMENT 'PDF 文件存储路径' AFTER `parsed_content`;
```

---

### Task 2: 后端 — 配置

**Files:**
- Modify: `java/src/main/resources/application.yml`

- [ ] **Step 1: 添加 multipart 上传限制**

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

添加到 `spring:` 层级下（与 `datasource:` 同级）。

---

### Task 3: 后端 — Service 层

**Files:**
- Modify: `java/src/main/java/com/yumian/service/ResumeService.java`
- Modify: `java/src/main/java/com/yumian/service/impl/ResumeServiceImpl.java`

- [ ] **Step 1: Service 接口新增方法**

```java
// ResumeService.java
void uploadResume(Long userId, MultipartFile file);

byte[] getPdfBytes(Long userId);
```

需要添加 import：`org.springframework.web.multipart.MultipartFile`

- [ ] **Step 2: Service 实现类实现方法**

```java
// ResumeServiceImpl.java

private final String RESUME_DIR = "userdata" + File.separator + "resumes";

@Override
@Transactional
public void uploadResume(Long userId, MultipartFile file) {
    // 1. 校验文件类型
    String contentType = file.getContentType();
    String filename = file.getOriginalFilename();
    if (contentType == null || !contentType.equals("application/pdf")) {
        // 部分浏览器 content-type 可能不准确，通过后缀二次校验
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("仅支持 PDF 文件");
        }
    }

    // 2. 确保目录存在
    File dir = new File(RESUME_DIR + File.separator + userId);
    if (!dir.exists()) {
        dir.mkdirs();
    }

    // 3. 保存文件（覆盖旧文件）
    File target = new File(dir, "resume.pdf");
    try {
        file.transferTo(target);
    } catch (IOException e) {
        throw new BusinessException("文件上传失败");
    }

    // 4. 更新数据库
    Resume resume = getResume(userId);
    resume.setPdfPath(userId + "/resume.pdf");
    resumeMapper.updateById(resume);
}

@Override
public byte[] getPdfBytes(Long userId) {
    Resume resume = getResume(userId);
    if (resume.getPdfPath() == null) {
        throw new BusinessException("尚未上传简历");
    }
    try {
        Path filePath = Paths.get(RESUME_DIR, resume.getPdfPath());
        return Files.readAllBytes(filePath);
    } catch (IOException e) {
        throw new BusinessException("简历文件读取失败");
    }
}
```

Import 需要添加：
```java
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
```

---

### Task 4: 后端 — Controller 层

**Files:**
- Modify: `java/src/main/java/com/yumian/controller/ResumeController.java`

- [ ] **Step 1: 新增上传和 PDF 获取端点**

```java
// ResumeController.java — 新增 import
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

// 新增方法：

@PostMapping("/upload")
public Result<Void> uploadResume(@RequestAttribute("userId") Long userId,
                                  @RequestParam("file") MultipartFile file) {
    resumeService.uploadResume(userId, file);
    return Result.success();
}

@GetMapping("/pdf")
public ResponseEntity<byte[]> getPdf(@RequestAttribute("userId") Long userId) {
    byte[] pdfBytes = resumeService.getPdfBytes(userId);
    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes);
}
```

- [ ] **Step 2: 修改 GET /api/resume 响应，添加 hasPdf 字段**

```java
// 修改 getResume 方法，返回 Map 补充 hasPdf 字段
@GetMapping
public Result<Map<String, Object>> getResume(@RequestAttribute("userId") Long userId) {
    Resume resume = resumeService.getResume(userId);
    Map<String, Object> result = new HashMap<>();
    result.put("id", resume.getId());
    result.put("userId", resume.getUserId());
    result.put("rawContent", resume.getRawContent());
    result.put("parsedContent", resume.getParsedContent());
    result.put("pdfPath", resume.getPdfPath());
    result.put("hasPdf", resume.getPdfPath() != null);
    result.put("createdAt", resume.getCreatedAt());
    result.put("updatedAt", resume.getUpdatedAt());
    return Result.success(result);
}
```

需要添加 import `java.util.Map` 和 `java.util.HashMap`（如果已有则不需要重复）。

---

### Task 5: 前端 — API 层

**Files:**
- Modify: `front/src/api/resume.js`

- [ ] **Step 1: 新增 uploadResume 和 getResumePdf**

```javascript
import request from './request'

export const getResume = () => request.get('/resume')

export const uploadResume = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/resume/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getResumePdf = () => {
  const token = localStorage.getItem('token')
  return fetch('/api/resume/pdf', {
    headers: { Authorization: `Bearer ${token}` }
  })
}

// 保留 optimizeResume 导出
export const optimizeResume = () => request.post('/resume/optimize')
```

---

### Task 6: 前端 — 添加依赖

**Files:**
- Modify: `front/package.json`

- [ ] **Step 1: 添加 vue-pdf-embed**

```
// package.json dependencies 中新增一行
"vue-pdf-embed": "^1.3.1",
```

- [ ] **Step 2: 安装依赖**

运行: `cd front && npm install`

---

### Task 7: 前端 — 页面重写

**Files:**
- Modify: `front/src/views/resume/ResumeEdit.vue`

- [ ] **Step 1: 完整重写 ResumeEdit.vue**

```vue
<template>
  <div class="resume-edit">
    <div class="page-header">
      <h1>我的简历</h1>
      <el-button v-if="hasPdf" type="primary" @click="handleReUpload">
        重新上传
      </el-button>
    </div>

    <!-- 无简历时：上传区域 -->
    <div v-if="!hasPdf && !uploading" class="upload-area" @click="triggerUpload" @dragover.prevent @drop.prevent="handleDrop">
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <p class="upload-text">点击或拖拽 PDF 文件到此处上传</p>
      <p class="upload-hint">仅支持 PDF 格式，最大 10MB</p>
      <input ref="fileInput" type="file" accept=".pdf,application/pdf" style="display:none" @change="handleFileSelect" />
    </div>

    <!-- 正在上传 -->
    <div v-if="uploading" class="upload-status">
      <el-progress :percentage="100" :stroke-width="6" indeterminate />
      <p class="uploading-text">正在上传并解析简历...</p>
    </div>

    <!-- 已有简历：PDF 预览 -->
    <div v-if="hasPdf" class="pdf-preview">
      <vue-pdf-embed :source="pdfSource" class="pdf-viewer" />
    </div>

    <!-- 上传失败 -->
    <el-dialog v-model="showError" title="上传失败" width="400px">
      <span>{{ errorMessage }}</span>
      <template #footer>
        <el-button @click="showError = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import VuePdfEmbed from 'vue-pdf-embed'
import { getResume, uploadResume, getResumePdf } from '../../api/resume'

const hasPdf = ref(false)
const uploading = ref(false)
const pdfSource = ref(null)
const fileInput = ref(null)
const showError = ref(false)
const errorMessage = ref('')

onMounted(async () => {
  try {
    const res = await getResume()
    if (res.data && res.data.hasPdf) {
      hasPdf.value = true
      await loadPdf()
    }
  } catch (e) {}
})

async function loadPdf() {
  try {
    const response = await getResumePdf()
    if (response.ok) {
      const blob = await response.blob()
      pdfSource.value = URL.createObjectURL(blob)
    }
  } catch (e) {}
}

function triggerUpload() {
  fileInput.value.click()
}

function handleDrop(e) {
  const files = e.dataTransfer.files
  if (files.length > 0) {
    uploadFile(files[0])
  }
}

function handleFileSelect(e) {
  const files = e.target.files
  if (files.length > 0) {
    uploadFile(files[0])
  }
  // 重置 input 以支持重复选择同一文件
  e.target.value = ''
}

async function uploadFile(file) {
  if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.warning('请选择 PDF 格式的文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 10MB')
    return
  }

  uploading.value = true
  try {
    await uploadResume(file)
    ElMessage.success('上传成功')
    hasPdf.value = true
    await loadPdf()
  } catch (e) {
    errorMessage.value = '上传失败，请重试'
    showError.value = true
  } finally {
    uploading.value = false
  }
}

function handleReUpload() {
  triggerUpload()
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); }

.upload-area {
  border: 2px dashed var(--card-border);
  border-radius: 16px;
  padding: 80px 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--card-bg);
}
.upload-area:hover {
  border-color: var(--flow-blue);
  background: rgba(14,165,233,0.04);
}
.upload-icon { font-size: 48px; color: var(--flow-blue); margin-bottom: 16px; }
.upload-text { font-size: 16px; color: var(--text-primary); margin: 0 0 8px; }
.upload-hint { font-size: 13px; color: var(--text-secondary); margin: 0; }

.upload-status { text-align: center; padding: 60px 40px; }
.uploading-text { font-size: 14px; color: var(--text-secondary); margin-top: 16px; }

.pdf-preview {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 16px;
  overflow: auto;
  max-height: 80vh;
}
.pdf-viewer {
  width: 100%;
}
.pdf-viewer :deep(canvas) {
  width: 100% !important;
  height: auto !important;
}
</style>
```

---

### Task 8: 验证与提交

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

- [ ] **Step 3: 集成测试（手动）**

1. 启动后端 `cd java && mvn spring-boot:run`
2. 启动前端 `cd front && npm run dev`
3. 登录系统 → 进入「我的简历」
4. 验证：页面显示上传区域，不显示文本输入框
5. 选择一个 PDF 文件上传
6. 验证：上传成功后展示 PDF 内容
7. 验证：PDF 展示为只读，不可编辑
8. 点击「重新上传」按钮
9. 验证：可选择新 PDF 替换
10. 验证页面刷新后 PDF 仍正常展示
