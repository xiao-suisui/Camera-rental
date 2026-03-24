import request from '@/utils/request'

export const messageApi = {
  // 获取消息列表
  getList: (userId) => request.get('/message/list', { params: { userId } }),

  // 获取未读消息数量
  getUnreadCount: (userId) => request.get('/message/unread-count', { params: { userId } }),

  // 标记消息已读
  markRead: (messageId) => request.post(`/message/read/${messageId}`),

  // 标记所有消息已读
  markAllRead: (userId) => request.post('/message/read-all', null, { params: { userId } }),

  // 删除消息
  delete: (messageId) => request.delete(`/message/${messageId}`)
}

