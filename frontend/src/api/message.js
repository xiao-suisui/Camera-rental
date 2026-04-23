import request from '@/utils/request'

export const messageApi = {
  // 获取消息列表
  getList: () => request.get('/message/list'),

  // 获取未读消息数量
  getUnreadCount: () => request.get('/message/unread-count'),

  // 标记消息已读
  markRead: (messageId) => request.post(`/message/read/${messageId}`),

  // 标记所有消息已读
  markAllRead: () => request.post('/message/read-all'),

  // 删除消息
  delete: (messageId) => request.delete(`/message/${messageId}`)
}

