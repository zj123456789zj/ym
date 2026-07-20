import request from './request'

export const getReview = (sessionId) => request.get(`/review/${sessionId}`)
export const generateReview = (sessionId) => request.post(`/review/generate/${sessionId}`)
