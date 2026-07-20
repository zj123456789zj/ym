import request from './request'

export const createSession = (type, targetCount) => request.post(`/interview/session?type=${type}&targetCount=${targetCount || 0}`)
export const askQuestion = (sessionId) => request.post(`/interview/question?sessionId=${sessionId}`)
export const submitAnswer = (data) => request.post('/interview/answer', data)
export const askFollowUp = (qaId) => request.post(`/interview/follow-up?qaId=${qaId}`)
export const endSession = (sessionId) => request.post(`/interview/end?sessionId=${sessionId}`, null, { timeout: 120000 })
export const getHistory = () => request.get('/interview/history')
export const getSession = (id) => request.get(`/interview/session/${id}`)
export const getQaList = (sessionId) => request.get(`/interview/qa/${sessionId}`)
