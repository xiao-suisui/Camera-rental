import request from '@/utils/request'

export const publishDevice = (data) => {
  return request.post('/device/publish', data)
}

export const batchPublishDevice = (data) => {
  return request.post('/device/batch-publish', data)
}

export const searchDevices = (params) => {
  return request.get('/device/search', { params })
}

export const getDeviceDetail = (deviceId) => {
  return request.get(`/device/${deviceId}`)
}

export const auditDevice = (data) => {
  return request.post('/device/audit', data)
}

export const updateDevice = (deviceId, data) => {
  return request.put(`/device/${deviceId}`, data)
}

export const deleteDevice = (deviceId) => {
  return request.delete(`/device/${deviceId}`)
}

export const offlineDevice = (deviceId) => {
  return request.post(`/device/${deviceId}/offline`)
}

export const onlineDevice = (deviceId) => {
  return request.post(`/device/${deviceId}/online`)
}

export const getMyDevices = (userId) => {
  return request.get('/device/my-devices', { params: { userId } })
}

// 统一API对象
export const deviceApi = {
  publish: publishDevice,
  batchPublish: batchPublishDevice,
  search: searchDevices,
  getById: getDeviceDetail,
  audit: auditDevice,
  update: updateDevice,
  delete: deleteDevice,
  offline: offlineDevice,
  online: onlineDevice,
  getMyDevices: getMyDevices,
  // 管理员接口
  adminList: (params) => request.get('/device/admin/list', { params })
}

