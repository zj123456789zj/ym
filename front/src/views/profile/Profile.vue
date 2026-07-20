<template>
  <div class="profile">
    <div class="page-header">
      <div>
        <h1>个人中心</h1>
      </div>
      <el-button type="primary" @click="handleSave" :loading="saving">保存修改</el-button>
    </div>

    <div class="profile-card">
      <el-form :model="form" label-width="100px" size="large">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="设置昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="选填" />
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '../../api/user'

const saving = ref(false)
const form = reactive({
  username: '',
  nickname: '',
  phone: '',
  email: ''
})

onMounted(async () => {
  try {
    const res = await getProfile()
    if (res.data) {
      Object.assign(form, res.data)
    }
  } catch (e) {}
})

async function handleSave() {
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname,
      phone: form.phone,
      email: form.email
    })
    ElMessage.success('保存成功')
  } catch (e) {} finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { font-size: 26px; font-weight: 700; color: var(--text-primary); }
.page-header .sub { font-size: 14px; color: var(--text-secondary); margin-top: 4px; }
.profile-card {
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--card-border); border-radius: 16px;
  padding: 32px; max-width: 600px;
}
</style>
