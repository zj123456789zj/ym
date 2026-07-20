<template>
  <div class="question-bank">

    <!-- ====== 分类卡片视图 ====== -->
    <template v-if="viewMode === 'categories'">
      <div class="page-header">
        <h2>题库管理</h2>
        <div class="header-actions">
          <el-button type="primary" @click="showImport = true">导入题目</el-button>
          <el-button @click="handleExport">导出题目</el-button>
        </div>
      </div>

      <div class="category-grid" v-loading="loading">
        <div v-for="cat in categories" :key="cat.id" class="cat-card" @click="enterCategory(cat)">
          <div class="cat-name">{{ cat.name }}</div>
          <div class="cat-count">{{ cat.questionCount }} 题</div>
        </div>
      </div>
    </template>

    <!-- ====== 题目列表视图 ====== -->
    <template v-else>
      <div class="page-header">
        <div class="header-left">
          <el-button text @click="backToCategories">← 返回分类列表</el-button>
          <h2>{{ currentCategoryName }}</h2>
        </div>
        <div class="header-actions">
          <el-button type="primary" @click="showImport = true">导入题目</el-button>
          <el-button @click="handleExport">导出题目</el-button>
          <el-button type="success" @click="openAdd">新增题目</el-button>
        </div>
      </div>

      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索题目..." clearable @clear="fetchData"
                  @keyup.enter="fetchData" style="width: 280px" />
        <el-button @click="fetchData">搜索</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="question" label="题目" min-width="300" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(scope.row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </template>

    <!-- ====== 编辑弹窗 ====== -->
    <el-dialog v-model="showEdit" :title="editingId ? '编辑题目' : '新增题目'" width="700px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="题目" required>
          <el-input v-model="editForm.question" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="editForm.referenceAnswer" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="editForm.category" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- ====== 导入弹窗 ====== -->
    <el-dialog v-model="showImport" title="导入题目" width="520px">
      <div class="import-hint">
        <p>支持 .json 或 .xlsx / .xls 文件。</p>
        <p>JSON 格式：<code>[{"question":"...","referenceAnswer":"...","category":"Java"}]</code></p>
        <p>Excel 格式：表头为 question / reference_answer / category</p>
      </div>
      <el-upload
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept=".json,.xlsx,.xls"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
      </el-upload>
      <div v-if="importResult" class="import-result">
        <p>总条数：{{ importResult.totalCount }}</p>
        <p>成功：{{ importResult.successCount }}</p>
        <p>失败：{{ importResult.failCount }}</p>
      </div>
      <template #footer>
        <el-button @click="showImport = false; importResult = null">关闭</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing" :disabled="!selectedFile">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  getQuestionList, addQuestion, updateQuestion, deleteQuestion,
  importQuestions, exportQuestions, getCategories
} from '../../api/questionBank'

const viewMode = ref('categories')
const currentCategoryName = ref('')

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filterCategory = ref('')
const keyword = ref('')
const categories = ref([])

const showEdit = ref(false)
const editingId = ref(null)
const saving = ref(false)
const editForm = ref({ question: '', referenceAnswer: '', category: '' })

const showImport = ref(false)
const selectedFile = ref(null)
const importing = ref(false)
const importResult = ref(null)

onMounted(() => {
  loadCategories()
})

async function loadCategories() {
  loading.value = true
  try {
    const res = await getCategories()
    categories.value = res.data
  } catch {
    ElMessage.error('加载分类失败')
  } finally {
    loading.value = false
  }
}

function enterCategory(cat) {
  currentCategoryName.value = cat.name
  filterCategory.value = cat.name
  viewMode.value = 'questions'
  currentPage.value = 1
  keyword.value = ''
  fetchData()
}

function backToCategories() {
  viewMode.value = 'categories'
  filterCategory.value = ''
  keyword.value = ''
  loadCategories()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getQuestionList({
      page: currentPage.value,
      size: pageSize.value,
      category: filterCategory.value || undefined,
      keyword: keyword.value || undefined
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch {
    ElMessage.error('加载题目失败')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editingId.value = null
  editForm.value = { question: '', referenceAnswer: '', category: currentCategoryName.value }
  showEdit.value = true
}

function openEdit(row) {
  editingId.value = row.id
  editForm.value = {
    question: row.question,
    referenceAnswer: row.referenceAnswer || '',
    category: row.category
  }
  showEdit.value = true
}

async function saveQuestion() {
  if (!editForm.value.question || !editForm.value.category) {
    ElMessage.warning('请填写题目和分类')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateQuestion(editingId.value, editForm.value)
      ElMessage.success('已更新')
    } else {
      await addQuestion(editForm.value)
      ElMessage.success('已添加')
    }
    showEdit.value = false
    fetchData()
    if (viewMode.value === 'categories') loadCategories()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteQuestion(id)
    ElMessage.success('已删除')
    fetchData()
  } catch {
    ElMessage.error('删除失败')
  }
}

async function toggleStatus(row) {
  try {
    await updateQuestion(row.id, { id: row.id, status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    fetchData()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleExport() {
  try {
    const res = await exportQuestions()
    const blob = new Blob([res], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'knowledge_questions.json'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

function handleFileChange(file) {
  selectedFile.value = file.raw
}

async function handleImport() {
  if (!selectedFile.value) return
  importing.value = true
  importResult.value = null
  try {
    const res = await importQuestions(selectedFile.value)
    importResult.value = res.data
    ElMessage.success(`导入完成：成功 ${res.data.successCount} 条，失败 ${res.data.failCount} 条`)
    if (viewMode.value === 'categories') {
      loadCategories()
    } else {
      fetchData()
      loadCategories()
    }
  } catch {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.question-bank { padding: 0; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 { margin: 0; font-size: 20px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-left h2 { margin: 0; }
.header-actions { display: flex; gap: 10px; }

.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }

/* 分类卡片网格 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.cat-card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 28px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
}
.cat-card:hover {
  border-color: var(--flow-blue, #4a90d9);
  box-shadow: 0 4px 16px rgba(74, 144, 217, 0.12);
  transform: translateY(-2px);
}
.cat-card .cat-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1a1a1a;
}
.cat-card .cat-count {
  font-size: 14px;
  color: #999;
}

.import-hint {
  background: #f5f7fa;
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 13px;
}
.import-hint code {
  background: #e8ecf1;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}
.import-result {
  margin-top: 16px;
  padding: 12px;
  background: #f0f9eb;
  border-radius: 6px;
}
.import-result p { margin: 4px 0; }
</style>
