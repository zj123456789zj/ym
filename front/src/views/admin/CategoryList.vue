<template>
  <div class="category-page">
    <div class="page-header">
      <h2>分类管理</h2>
      <el-button type="primary" @click="showAdd = true">新增分类</el-button>
    </div>

    <el-table :data="categories" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="分类名称" min-width="200" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button size="small" type="danger" @click="confirmDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAdd" title="新增分类" width="400px">
      <el-form>
        <el-form-item label="分类名称" required>
          <el-input v-model="newCategoryName" placeholder="输入分类名称" @keyup.enter="saveCategory" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="saveCategory" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 含题目的分类删除确认弹窗 -->
    <el-dialog v-model="showDeleteWarning" title="确认删除" width="400px">
      <p style="font-size: 15px; line-height: 1.6;">
        该分类 <strong>{{ deletingCategory?.name }}</strong> 下还有
        <strong style="color: #e74c3c;">{{ deleteQuestionCount }}</strong> 道题目，
        确定删除吗？删除后题目也会一并删除。
      </p>
      <template #footer>
        <el-button @click="showDeleteWarning = false">取消</el-button>
        <el-button type="danger" @click="forceDelete" :loading="deleting">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategories, addCategory, deleteCategory } from '../../api/questionBank'

const loading = ref(false)
const categories = ref([])
const showAdd = ref(false)
const saving = ref(false)
const newCategoryName = ref('')

const showDeleteWarning = ref(false)
const deleting = ref(false)
const deletingCategory = ref(null)
const deleteQuestionCount = ref(0)

onMounted(() => fetchData())

async function fetchData() {
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

async function saveCategory() {
  const name = newCategoryName.value.trim()
  if (!name) { ElMessage.warning('请输入分类名称'); return }
  saving.value = true
  try {
    await addCategory({ name })
    ElMessage.success('已添加')
    showAdd.value = false
    newCategoryName.value = ''
    fetchData()
  } catch {
    ElMessage.error('添加失败')
  } finally {
    saving.value = false
  }
}

async function confirmDelete(category) {
  // 第一步：调删除接口（不传 force），看是否有题目
  try {
    const res = await deleteCategory(category.id)
    if (res.data && res.data.warning) {
      // 有题目，弹二次确认窗
      deletingCategory.value = category
      deleteQuestionCount.value = res.data.questionCount
      showDeleteWarning.value = true
    } else {
      // 没有题目，直接删了
      ElMessage.success(`已删除「${category.name}」`)
      fetchData()
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

async function forceDelete() {
  deleting.value = true
  try {
    const res = await deleteCategory(deletingCategory.value.id, true)
    showDeleteWarning.value = false
    ElMessage.success(`已删除「${deletingCategory.value.name}」及 ${res.data.deletedQuestions} 道题目`)
    fetchData()
  } catch {
    ElMessage.error('删除失败')
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.category-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; }
</style>
