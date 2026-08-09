<template>
  <div class="chat-list">
    <h2>我的聊天</h2>
    <div v-loading="loading">
      <div v-if="conversations.length === 0" class="empty-state">
        <el-empty description="暂无聊天记录">
          <el-button type="primary" @click="$router.push('/devices')">去浏览设备</el-button>
        </el-empty>
      </div>
      <div v-else class="conversation-items">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-item"
          @click="$router.push(`/chat/${conv.id}`)"
        >
          <div class="avatar-circle">
            {{ conv.otherNickname?.charAt(0) || '?' }}
          </div>
          <div class="conv-info">
            <div class="conv-header">
              <span class="nickname">{{ conv.otherNickname }}</span>
              <span class="time">{{ formatTime(conv.lastMessageAt) }}</span>
            </div>
            <div class="conv-footer">
              <span class="last-msg">{{ conv.lastMessage || '暂无消息' }}</span>
              <div class="conv-actions">
                <el-badge v-if="conv.unreadCount > 0" :value="conv.unreadCount" :max="99" />
                <el-button
                  class="delete-btn"
                  text
                  size="small"
                  @click.stop="handleDelete(conv)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, onActivated } from 'vue'
import { chatApi } from '@/api/chat'
import eventBus from '@/utils/eventBus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const loading = ref(false)
const conversations = ref([])

const formatTime = (time) => {
  if (!time) return ''
  const now = dayjs()
  const t = dayjs(time)
  if (t.isSame(now, 'day')) return t.format('HH:mm')
  if (t.isSame(now.subtract(1, 'day'), 'day')) return '昨天'
  if (t.isSame(now, 'year')) return t.format('MM-DD')
  return t.format('YYYY-MM-DD')
}

const loadConversations = async () => {
  loading.value = true
  try {
    const res = await chatApi.getConversations()
    conversations.value = res.data || []
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

const handleDelete = async (conv) => {
  try {
    await ElMessageBox.confirm(
      `确定删除与「${conv.otherNickname}」的聊天吗？`,
      '删除会话',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await chatApi.deleteConversation(conv.id)
    conversations.value = conversations.value.filter(c => c.id !== conv.id)
    ElMessage.success('已删除')
  } catch (e) {
    // 取消或失败
  }
}

onMounted(() => {
  loadConversations()
  eventBus.on('chat:message', loadConversations)
})

onUnmounted(() => {
  eventBus.off('chat:message', loadConversations)
})

// 从聊天室返回时自动刷新
onActivated(() => {
  loadConversations()
})
</script>

<style scoped>
.chat-list {
  max-width: 800px;
  margin: 30px auto;
  padding: 0 20px;
}

.chat-list h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 20px;
}

.empty-state {
  padding: 60px 0;
}

.conversation-items {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f5f5f5;
}

.conversation-item:last-child {
  border-bottom: none;
}

.conversation-item:hover {
  background-color: #f8f9fa;
}

.avatar-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
  margin-right: 14px;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.nickname {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.time {
  font-size: 12px;
  color: #999;
}

.conv-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.last-msg {
  font-size: 14px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 80%;
}

.conv-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
  color: #999;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #f56c6c;
}

@media (max-width: 768px) {
  .delete-btn {
    opacity: 0.5;
  }
}
</style>
