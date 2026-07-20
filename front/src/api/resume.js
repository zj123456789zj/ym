import request from './request'

export const getResume = () => request.get('/resume')

export const uploadResume = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/resume/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getResumePdf = () => {
  const token = localStorage.getItem('token')
  return fetch('/api/resume/pdf', {
    headers: { Authorization: `Bearer ${token}` }
  })
}

export const optimizeResume = () => request.post('/resume/optimize')
