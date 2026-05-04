import request from '@/utils/request'

export const chatApi = {
  // 创建或获取会话
  getOrCreateConversation: (data) => request.post('/chat/conversations', data),

  // 获取会话列表
  getConversations: () => request.get('/chat/conversations'),

  // 获取历史消息
  getMessages: (conversationId, page = 1, size = 20) =>
    request.get(`/chat/conversations/${conversationId}/messages`, { params: { page, size } }),

  // 获取总未读数
  getUnreadTotal: () => request.get('/chat/unread-total'),

  // 标记会话已读
  markAsRead: (conversationId) => request.post(`/chat/conversations/${conversationId}/read`),

  // 删除会话
  deleteConversation: (conversationId) => request.delete(`/chat/conversations/${conversationId}`)
}
