import request from '@/utils/request'

export const disputeApi = {
  create: (data) => request.post('/dispute/create', data),
  getByOrderId: (orderId) => request.get(`/dispute/order/${orderId}`),
  adminList: (params) => request.get('/dispute/admin/list', { params }),
  adminGetDetail: (disputeId) => request.get(`/dispute/admin/${disputeId}`),
  adminUpdate: (disputeId, data) => request.put(`/dispute/admin/${disputeId}`, data),
  adminPendingCount: () => request.get('/dispute/admin/stats/pending')
}
