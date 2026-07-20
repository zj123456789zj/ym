import { defineStore } from 'pinia'
import { createSession, askQuestion, submitAnswer, askFollowUp, endSession } from '../api/interview'

export const useInterviewStore = defineStore('interview', {
  state: () => ({
    sessionId: null,
    sessionType: null,
    targetCount: 0,
    currentQa: null,
    qaList: [],
    isEnded: false,
    sessionEnded: false,
    loading: false,
    submitting: false
  }),
  actions: {
    async startSession(type, targetCount) {
      const res = await createSession(type, targetCount)
      this.sessionId = res.data.id
      this.sessionType = type
      this.targetCount = targetCount || res.data.targetCount || 0
      this.qaList = []
      this.isEnded = false
      this.sessionEnded = false
      return res.data
    },
    async ask() {
      this.loading = true
      try {
        const res = await askQuestion(this.sessionId)
        this.currentQa = res.data
        return res.data
      } finally {
        this.loading = false
      }
    },
    async answer(qaId, answer) {
      this.submitting = true
      try {
        const res = await submitAnswer({ qaId, answer })
        const updated = res.data
        this.currentQa = updated
        if (updated.sessionEnded) {
          this.isEnded = true
          this.sessionEnded = true
        }
        const idx = this.qaList.findIndex(q => q.id === qaId)
        if (idx >= 0) this.qaList[idx] = updated
        return updated
      } finally {
        this.submitting = false
      }
    },
    async followUp(parentQaId) {
      this.loading = true
      try {
        const res = await askFollowUp(parentQaId)
        this.currentQa = res.data
        return res.data
      } finally {
        this.loading = false
      }
    },
    async end() {
      this.loading = true
      try {
        await endSession(this.sessionId)
      } finally {
        this.isEnded = true
        this.loading = false
      }
    },
    reset() {
      this.sessionId = null
      this.sessionType = null
      this.targetCount = 0
      this.currentQa = null
      this.qaList = []
      this.isEnded = false
      this.sessionEnded = false
    }
  }
})
