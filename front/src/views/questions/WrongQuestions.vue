<template>
  <div class="wrong-questions">
    <div class="page-header">
      <div>
        <h1>错题本</h1>
      </div>
      <el-button type="primary" @click="showAdd = true">添加错题</el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="category" placeholder="全部分类" @change="fetchList" clearable>
        <el-option label="全部" value="" />
        <el-option label="Java" value="Java" />
        <el-option label="MySQL" value="MySQL" />
        <el-option label="项目经验" value="项目经验" />
        <el-option label="系统设计" value="系统设计" />
        <el-option label="其他" value="其他" />
      </el-select>
    </div>

    <div v-if="list.length === 0" class="empty-state">
      <el-empty description="还没有错题" />
    </div>

    <div v-else class="question-list">
      <div v-for="item in list" :key="item.id" class="question-card">
        <div class="card-header">
          <el-tag v-if="item.category" size="small">{{ item.category }}</el-tag>
          <span class="card-source" v-if="item.source">{{ item.source }}</span>
          <span class="card-review">复习 {{ item.reviewCount || 0 }} 次</span>
        </div>
        <div class="card-question">{{ item.question }}</div>
        <el-collapse>
          <el-collapse-item title="查看答案与解析">
            <div class="answer-section">
              <div class="answer-item">
                <span class="label">正确答案：</span>
                <p>{{ item.referenceAnswer || item.analysis }}</p>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
        <div class="card-footer">
          <el-button size="small" type="primary" plain @click="handleReview(item.id)">标记复习</el-button>
        </div>
      </div>
    </div>

    <!-- 添加错题对话框 -->
    <el-dialog v-model="showAdd" title="添加错题" width="500px">
      <el-form :model="newQuestion" label-width="80px">
        <el-form-item label="题目">
          <el-input v-model="newQuestion.question" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="newQuestion.category">
            <el-option label="Java" value="Java" />
            <el-option label="MySQL" value="MySQL" />
            <el-option label="项目经验" value="项目经验" />
            <el-option label="系统设计" value="系统设计" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="正确答案">
          <el-input v-model="newQuestion.referenceAnswer" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="handleAdd" :loading="adding">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getWrongQuestions, addWrongQuestion, reviewQuestion } from '../../api/question'

const list = ref([])
const category = ref('')
const showAdd = ref(false)
const adding = ref(false)
const newQuestion = reactive({
  question: '',
  category: 'Java',
  referenceAnswer: '',
  source: '手动添加'
})

onMounted(() => fetchList())

async function fetchList() {
  try {
    const res = await getWrongQuestions(category.value || undefined)
    list.value = res.data || []
  } catch (e) {}
}

async function handleReview(id) {
  try {
    await reviewQuestion(id)
    ElMessage.success('已标记复习')
    fetchList()
  } catch (e) {}
}

async function handleAdd() {
  adding.value = true
  try {
    await addWrongQuestion(newQuestion)
    ElMessage.success('添加成功')
    showAdd.value = false
    fetchList()
  } catch (e) {} finally {
    adding.value = false
  }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); }
.page-header .sub { font-size: 14px; color: var(--text-secondary); margin-top: 4px; }
.filter-bar { margin-bottom: 20px; }
.empty-state { margin-top: 60px; }
.question-list { display: grid; gap: 16px; }
.question-card {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 20px 24px;
}
.card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.card-source { font-size: 12px; color: var(--text-muted); }
.card-review { margin-left: auto; font-size: 12px; color: var(--text-muted); }
.card-question { font-size: 15px; font-weight: 500; line-height: 1.6; margin-bottom: 12px; }
.answer-section { padding: 12px 0; }
.answer-item .label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.answer-item p { font-size: 14px; line-height: 1.6; color: var(--text-primary); margin-top: 4px; }
.card-footer { margin-top: 12px; }
</style>
