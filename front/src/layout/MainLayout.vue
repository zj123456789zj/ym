<template>
  <div class="layout">
    <Sidebar />
    <div class="main-area">
      <div class="top-bar">
        <div></div>
        <el-dropdown trigger="click" @command="handleCommand">
          <div class="user-avatar">{{ avatarText }}</div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <main class="main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Sidebar from './Sidebar.vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const avatarText = computed(() => {
  if (userStore.userInfo?.nickname) {
    return userStore.userInfo.nickname.charAt(0).toUpperCase()
  }
  return 'U'
})

if (!userStore.userInfo) {
  userStore.fetchProfile()
}

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  display: flex; min-height: 100vh; position: relative; z-index: 1;
}
.main-area {
  flex: 1; display: flex; flex-direction: column;
  max-width: 1200px;
}
.top-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 40px 0;
}
.user-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, var(--flow-blue), #38BDF8);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 600; font-size: 14px;
  cursor: pointer; transition: opacity 0.2s;
}
.user-avatar:hover { opacity: 0.8; }
.main { padding: 20px 40px 40px; flex: 1; }
</style>
