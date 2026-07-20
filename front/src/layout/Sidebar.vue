<template>
  <aside class="sidebar">
    <div class="sidebar-brand">
      <div class="fish-icon">鱼</div>
      <div class="brand-text">
        <span>鱼面</span>
        <span class="brand-slogan">鱼跃深海 · 面面俱到</span>
      </div>
    </div>
    <nav class="sidebar-nav">
      <router-link v-for="item in navItems" :key="item.path" :to="item.path"
        class="nav-item" active-class="active">
        <span class="nav-icon">{{ item.icon }}</span>
        <span class="label">{{ item.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

const navItems = computed(() => {
  const items = [
    { path: '/overview', icon: '📊', label: '总览' },
    { path: '/resume', icon: '📄', label: '我的简历' },
    { path: '/interview/mock', icon: '🎯', label: '模拟面试' },
    { path: '/interview/special', icon: '⚡', label: '专项训练' },
    { path: '/interview/history', icon: '📝', label: '面试历史' },
    { path: '/statistics', icon: '📈', label: '数据统计' },
    { path: '/profile', icon: '👤', label: '个人中心' },
  ]
  if (userStore.isAdmin) {
    items.push({ path: '/admin/questions', icon: '📚', label: '题库管理' })
    items.push({ path: '/admin/categories', icon: '🏷️', label: '分类管理' })
  }
  return items
})
</script>

<style scoped>
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, rgba(168,192,212,0.75) 0%, rgba(181,203,224,0.7) 50%, rgba(191,210,229,0.65) 100%);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 0 0 24px;
  border-right: 1px solid rgba(0,0,0,0.05);
}
.sidebar-brand {
  display: flex; align-items: center; gap: 10px;
  padding: 24px 20px 24px;
}
.sidebar-brand .fish-icon {
  width: 36px; height: 36px;
  background: linear-gradient(135deg, var(--flow-blue), #38BDF8);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 19px; font-weight: 700; color: #fff;
  box-shadow: 0 4px 16px rgba(14,165,233,0.25);
}
.brand-text { display: flex; flex-direction: column; }
.brand-text span:first-child {
  font-size: 20px; font-weight: 700; letter-spacing: 2px;
  color: var(--deep-blue);
}
.brand-slogan {
  font-size: 11px; color: var(--text-muted); letter-spacing: 1px;
  margin-top: 1px;
}
.sidebar-nav {
  flex: 1; display: flex; flex-direction: column; gap: 2px; padding: 8px 10px 0;
}
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 9px;
  font-size: 13.5px; font-weight: 500;
  color: var(--text-secondary); text-decoration: none;
  transition: all 0.2s ease; position: relative;
}
.nav-item:hover {
  color: var(--text-primary); background: rgba(0,0,0,0.04);
}
.nav-item.active {
  color: var(--deep-blue); font-weight: 600;
  background: linear-gradient(90deg, rgba(14,165,233,0.12) 0%, rgba(14,165,233,0.03) 100%);
}
.nav-item.active::before {
  content: ''; position: absolute; left: 0; top: 50%;
  transform: translateY(-50%);
  width: 2.5px; height: 18px;
  background: var(--flow-blue); border-radius: 0 3px 3px 0;
  box-shadow: 0 0 10px rgba(14,165,233,0.4);
}
.nav-icon {
  width: 28px; height: 28px; border-radius: 7px;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; flex-shrink: 0;
  transition: all 0.2s ease;
  background: rgba(255,255,255,0.5); color: var(--text-secondary);
}
.nav-item:hover .nav-icon {
  background: rgba(255,255,255,0.7); color: var(--text-primary);
}
.nav-item.active .nav-icon {
  background: #fff; color: var(--flow-blue);
  box-shadow: 0 2px 8px rgba(14,165,233,0.12);
}
</style>
