import request from '@/utils/request'

export const favoriteApi = {
  // 收藏设备
  add: (userId, deviceId) => request.post('/favorite/add', null, {
    params: { userId, deviceId }
  }),

  // 取消收藏
  remove: (userId, deviceId) => request.delete('/favorite/remove', {
    params: { userId, deviceId }
  }),

  // 检查是否已收藏
  check: (userId, deviceId) => request.get('/favorite/check', {
    params: { userId, deviceId }
  }),

  // 获取收藏列表
  getList: (userId) => request.get('/favorite/list', {
    params: { userId }
  }),

  // 获取收藏数量
  getCount: (userId) => request.get('/favorite/count', {
    params: { userId }
  })
}

