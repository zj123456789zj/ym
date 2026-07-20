<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="fish-icon">鱼</div>
        <h1>鱼面</h1>
        <p class="slogan">鱼跃深海 · 面面俱到</p>
      </div>
      <el-tabs v-model="activeTab" class="tabs" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" class="submit-btn" @click="handleLogin" :loading="loading">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码（至少6位）" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" show-password />
            </el-form-item>
            <el-form-item prop="captcha">
              <div class="captcha-row">
                <el-input v-model="registerForm.captcha" placeholder="验证码" class="captcha-input" />
                <div class="captcha-box" @click="refreshCaptcha">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" width="100" height="38" />
              </div>
                <span class="captcha-refresh" @click="refreshCaptcha">换一张</span>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" class="submit-btn" @click="handleRegister" :loading="loading">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import { getCaptcha } from '../../api/user'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirmPassword: '', captcha: '' })
const captchaImage = ref('')

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function refreshCaptcha() {
  registerForm.captcha = ''
  try {
    const res = await getCaptcha()
    captchaImage.value = res.data.captchaImage
  } catch (e) {}
}

async function handleLogin() {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(loginForm)
      ElMessage.success('登录成功')
      router.push('/overview')
    } catch (e) {
    } finally {
      loading.value = false
    }
  })
}

async function handleRegister() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.register({
        username: registerForm.username,
        password: registerForm.password,
        captchaCode: registerForm.captcha
      })
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
    } catch (e) {
      // 注册失败（用户名重复/验证码过期/验证码错误）自动刷新验证码
      refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

// 切换到注册 tab 时加载验证码
watch(activeTab, (val) => {
  if (val === 'register') {
    nextTick(() => refreshCaptcha())
  }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--page-bg-gradient);
}
.login-card {
  width: 400px; padding: 40px;
  background: var(--card-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--card-border);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(11,29,58,0.08);
}
.brand { text-align: center; margin-bottom: 32px; }
.brand .fish-icon {
  width: 56px; height: 56px; margin: 0 auto 12px;
  background: linear-gradient(135deg, var(--flow-blue), #38BDF8);
  border-radius: 16px; display: flex; align-items: center; justify-content: center;
  font-size: 28px; color: #fff; font-weight: 700;
  box-shadow: 0 8px 24px rgba(14,165,233,0.3);
}
.brand h1 { font-size: 28px; font-weight: 700; color: var(--deep-blue); }
.brand .slogan { font-size: 13px; color: var(--text-muted); margin-top: 4px; letter-spacing: 2px; }
.submit-btn { width: 100%; }
.tabs { width: 100%; }
.captcha-row { display: flex; align-items: center; gap: 8px; width: 100%; }
.captcha-input { width: 100px; }
.captcha-box {
  border-radius: 6px; overflow: hidden; cursor: pointer;
  border: 1px solid var(--card-border); flex-shrink: 0;
  line-height: 0;
}
.captcha-box img { display: block; }
.captcha-refresh {
  font-size: 12px; color: var(--flow-blue); cursor: pointer;
  white-space: nowrap; user-select: none;
}
.captcha-refresh:hover { text-decoration: underline; }
</style>
