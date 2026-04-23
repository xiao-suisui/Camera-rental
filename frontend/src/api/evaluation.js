import request from '@/utils/request'

export const evaluationApi = {
  // 创建评价
  create: (data) => request.post('/evaluation/create', data),

  // 查看设备的评价列表
  getByDevice: (deviceId) => request.get(`/evaluation/device/${deviceId}`),

  // 查看我收到的评价
  getMine: () => request.get('/evaluation/user/me'),

  // 管理员：查看指定用户收到的评价
  getByUserAsAdmin: (userId) => request.get(`/evaluation/user/${userId}`),

  // 查看订单的评价
  getByOrder: (orderId) => request.get(`/evaluation/order/${orderId}`),

  // 删除评价
  delete: (evaluationId) => request.delete(`/evaluation/${evaluationId}`),

  // 检查是否可以评价
  canEvaluate: (orderId) => request.get('/evaluation/can-evaluate', {
    params: { orderId }
  })
}

