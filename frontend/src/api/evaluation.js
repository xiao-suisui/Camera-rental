import request from '@/utils/request'

export const evaluationApi = {
  // 创建评价
  create: (data) => request.post('/evaluation/create', data),

  // 查看设备的评价列表
  getByDevice: (deviceId) => request.get(`/evaluation/device/${deviceId}`),

  // 查看用户收到的评价
  getByUser: (userId) => request.get(`/evaluation/user/${userId}`),

  // 查看订单的评价
  getByOrder: (orderId) => request.get(`/evaluation/order/${orderId}`),

  // 删除评价
  delete: (evaluationId, userId) => request.delete(`/evaluation/${evaluationId}`, {
    params: { userId }
  }),

  // 检查是否可以评价
  canEvaluate: (orderId, userId) => request.get('/evaluation/can-evaluate', {
    params: { orderId, userId }
  })
}

