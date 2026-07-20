import request from './request'

export const getQuestionList = (params) => request.get('/admin/questions', { params })
export const getQuestionById = (id) => request.get(`/admin/questions/${id}`)
export const addQuestion = (data) => request.post('/admin/questions', data)
export const updateQuestion = (id, data) => request.put(`/admin/questions/${id}`, data)
export const deleteQuestion = (id) => request.delete(`/admin/questions/${id}`)
export const importQuestions = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/questions/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const exportQuestions = () => request.get('/admin/questions/export', { responseType: 'blob' })
export const getImportLogs = () => request.get('/admin/questions/import-logs')
export const getCategories = () => request.get('/admin/questions/categories')
export const addCategory = (data) => request.post('/admin/questions/categories', data)
export const deleteCategory = (id, force = false) => request.delete(`/admin/questions/categories/${id}?force=${force}`)
