<template>
  <div class="resume-edit">
    <div class="page-header">
      <h1>我的简历</h1>
      <el-button v-if="hasPdf" type="primary" @click="triggerUpload">
        重新上传
      </el-button>
    </div>

    <div v-if="!hasPdf && !uploading" class="upload-area" @click="triggerUpload" @dragover.prevent @drop.prevent="handleDrop">
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <p class="upload-text">点击或拖拽 PDF 文件到此处上传</p>
      <p class="upload-hint">仅支持 PDF 格式，最大 10MB</p>
    </div>
    <input ref="fileInput" type="file" accept=".pdf,application/pdf" style="display:none" @change="handleFileSelect" />

    <div v-if="uploading" class="upload-status">
      <el-progress :percentage="100" :stroke-width="6" indeterminate />
      <p class="uploading-text">正在上传简历...</p>
    </div>

    <div v-if="hasPdf" class="pdf-preview">
      <div class="pdf-scroll-wrap">
        <vue-pdf-embed :source="pdfSource" class="pdf-viewer" :scale="2" />
      </div>
    </div>

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
}
.pdf-scroll-wrap {
  max-height: 75vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.pdf-viewer { width: 100%; }
.pdf-viewer :deep(canvas) {
  max-width: 100%;
  height: auto !important;
}
</style>
