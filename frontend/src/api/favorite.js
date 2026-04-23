import request from '@/utils/request'

export const favoriteApi = {
  // 收藏设备
  add: (deviceId) => request.post('/favorite/add', null, {
    params: { deviceId }
  }),

  // 取消收藏
  remove: (deviceId) => request.delete('/favorite/remove', {
    params: { deviceId }
  }),

  // 检查是否已收藏
  check: (deviceId) => request.get('/favorite/check', {
    params: { deviceId }
  }),

  // 获取收藏列表
  getList: () => request.get('/favorite/list'),

  // 获取收藏数量
  getCount: () => request.get('/favorite/count')
}

