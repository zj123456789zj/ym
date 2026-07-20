import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, getProfile } from '../api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.userInfo?.role === 'admin'
  },
  actions: {
    async login(credentials) {
      const res = await loginApi(credentials)
      this.token = res.data.token
      this.userInfo = res.data.user
      localStorage.setItem('token', this.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },
    async register(data) {
      await registerApi(data)
    },
    async fetchProfile() {
      const res = await getProfile()
      this.userInfo = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
