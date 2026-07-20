import request from './request'

export const getWrongQuestions = (category) => request.get('/questions', { params: { category } })
export const addWrongQuestion = (data) => request.post('/questions', data)
export const reviewQuestion = (id) => request.post(`/questions/${id}/review`)
