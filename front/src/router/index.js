import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/overview',
    children: [
      { path: 'overview', name: 'Overview', component: () => import('../views/overview/Overview.vue') },
      { path: 'resume', name: 'Resume', component: () => import('../views/resume/ResumeEdit.vue') },
      { path: 'resume/optimize', name: 'ResumeOptimize', component: () => import('../views/resume/ResumeOptimize.vue') },
      { path: 'interview/mock', name: 'MockInterview', component: () => import('../views/interview/MockInterview.vue') },
      { path: 'interview/special', name: 'SpecialTraining', component: () => import('../views/interview/SpecialTraining.vue') },
      { path: 'interview/history', name: 'InterviewHistory', component: () => import('../views/interview/InterviewHistory.vue') },
      { path: 'interview/review/:id', name: 'InterviewReview', component: () => import('../views/review/InterviewReview.vue') },
      { path: 'questions', name: 'WrongQuestions', component: () => import('../views/questions/WrongQuestions.vue') },
      { path: 'statistics', name: 'Statistics', component: () => import('../views/statistics/Statistics.vue') },
      { path: 'profile', name: 'Profile', component: () => import('../views/profile/Profile.vue') },
      { path: '/admin/questions', name: 'QuestionBank', component: () => import('../views/admin/QuestionList.vue') },
      { path: '/admin/categories', name: 'CategoryList', component: () => import('../views/admin/CategoryList.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path.startsWith('/admin')) {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr)
        if (userInfo.role === 'admin') {
          next()
        } else {
          next('/overview')
        }
      } catch {
        next('/overview')
      }
    } else {
      next('/overview')
    }
  } else {
    next()
  }
})

export default router
