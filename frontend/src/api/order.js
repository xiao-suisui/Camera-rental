import request from '@/utils/request'

// 统一导出order API
export const orderApi = {
  // 创建订单
  create: (data) => request.post('/order/create', data),

  // 支付订单
  pay: (orderId) => request.post(`/order/${orderId}/pay`),

  // 确认发货
  ship: (orderId, data) => request.post(`/order/${orderId}/ship`, data),

  // 确认收货
  receive: (orderId) => request.post(`/order/${orderId}/receive`),

  // 申请归还
  applyReturn: (orderId, data) => request.post(`/order/${orderId}/return`, data),

  // 确认归还
  confirmReturn: (orderId, data) => request.post(`/order/${orderId}/confirm`, data),

  // 取消订单
  cancel: (orderId) => request.post(`/order/${orderId}/cancel`),

  // 订单详情
  getDetail: (orderId) => request.get(`/order/${orderId}`),

  // 我的订单列表（分页，用户身份由后端从请求头判定）
  getList: (params) => request.get('/order/list', { params }),

  // 我出租的订单（分页，用户身份由后端从请求头判定）
  getLessorList: (params) => request.get('/order/lessor/list', { params }),

  // 我租用的订单（分页，用户身份由后端从请求头判定）
  getRenterList: (params) => request.get('/order/renter/list', { params }),

  // 管理员接口
  adminList: (params) => request.get('/order/admin/list', { params })
}

// 兼容旧的导出方式
export const createOrder = orderApi.create
export const payOrder = orderApi.pay
export const getOrderList = orderApi.getList


