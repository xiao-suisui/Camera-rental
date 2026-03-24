import request from '@/utils/request'

export const register = (data) => {
  return request.post('/user/register', data)
}

export const login = (data) => {
  return request.post('/user/login', data)
}

export const getUserInfo = (userId) => {
  return request.get(`/user/${userId}`)
}

export const updateProfile = (userId, data) => {
  return request.put(`/user/${userId}/profile`, data)
}

export const changePassword = (userId, data) => {
  return request.post(`/user/${userId}/change-password`, data)
}

export const uploadAvatar = (formData) => {
  return request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const logoutApi = () => {
  return request.post('/user/logout')
}

// ========== 管理员接口 ==========
export const adminListUsers = (params) => {
  return request.get('/user/admin/list', { params })
}

export const adminDisableUser = (userId) => {
  return request.put(`/user/admin/${userId}/disable`)
}

export const adminEnableUser = (userId) => {
  return request.put(`/user/admin/${userId}/enable`)
}

