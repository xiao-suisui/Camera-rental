<template>
  <div class="message-center">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>消息中心</span>
          <div class="header-actions">
            <el-button size="small" @click="handleMarkAllRead" :disabled="unreadCount === 0">
              全部已读
            </el-button>
          </div>
        </div>
      </template>

      <!-- 消息类型标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部消息" name="all">
          <template #label>
            <span>全部消息</span>
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="tab-badge" />
          </template>
        </el-tab-pane>
        <el-tab-pane label="设备审核" name="DEVICE_AUDIT" />
        <el-tab-pane label="订单消息" name="ORDER_STATUS" />
        <el-tab-pane label="优惠券" name="COUPON" />
        <el-tab-pane label="系统消息" name="SYSTEM" />
      </el-tabs>

      <!-- 消息列表 -->
      <div v-loading="loading" style="min-height: 400px">
        <el-empty v-if="filteredMessages.length === 0" description="暂无消息" />

        <div v-else class="message-list">
          <div
            v-for="message in filteredMessages"
            :key="message.id"
            class="message-item"
            :class="{ unread: message.readStatus === 'UNREAD' }"
            @click="handleMarkRead(message)"
          >
            <div class="message-header">
              <div class="message-type">
                <el-tag :type="getTypeTag(message.type)" size="small">
                  {{ getTypeText(message.type) }}
                </el-tag>
                <span v-if="message.readStatus === 'UNREAD'" class="unread-dot"></span>
              </div>
              <span class="message-time">{{ formatTime(message.createdAt) }}</span>
            </div>

            <div class="message-content">
              {{ message.content }}
            </div>

            <div class="message-footer">
              <el-button
                type="danger"
                size="small"
                text
                @click.stop="handleDelete(message.id)"
              >
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { messageApi } from '@/api/message'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { formatDate } from '@/utils/date'

const userStore = useUserStore()
const loading = ref(false)
const activeTab = ref('all')
const messages = ref([])
const unreadCount = ref(0)

const filteredMessages = computed(() => {
  if (activeTab.value === 'all') return messages.value
  return messages.value.filter(m => m.type === activeTab.value)
})

const getTypeText = (type) => {
  const typeMap = {
    'DEVICE_AUDIT': '设备审核',
    'ORDER_STATUS': '订单消息',
    'COUPON': '优惠券',
    'SYSTEM': '系统消息'
  }
  return typeMap[type] || type
}

const getTypeTag = (type) => {
  const tagMap = {
    'DEVICE_AUDIT': 'warning',
    'ORDER_STATUS': 'primary',
    'COUPON': 'success',
    'SYSTEM': 'info'
  }
  return tagMap[type] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  return formatDate(time, 'YYYY-MM-DD HH:mm:ss')
}

onMounted(async () => {
  await loadMessages()
  await loadUnreadCount()
})

const loadMessages = async () => {
  loading.value = true
  try {
    const { data } = await messageApi.getList(userStore.userInfo.userId)
    messages.value = data || []
  } catch (error) {
    ElMessage.error('加载消息列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadUnreadCount = async () => {
  try {
    const { data } = await messageApi.getUnreadCount(userStore.userInfo.userId)
    unreadCount.value = data || 0
  } catch (error) {
    console.error('获取未读数量失败', error)
  }
}

const handleTabChange = () => {
  // 切换标签页
}

const handleMarkRead = async (message) => {
  if (message.readStatus === 'READ') return

  try {
    await messageApi.markRead(message.id)
    message.readStatus = 'READ'
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (error) {
    console.error('标记已读失败', error)
  }
}

const handleMarkAllRead = async () => {
  try {
    await messageApi.markAllRead(userStore.userInfo.userId)
    ElMessage.success('已全部标记为已读')
    await loadMessages()
    unreadCount.value = 0
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (messageId) => {
  try {
    await ElMessageBox.confirm('确认删除此消息？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await messageApi.delete(messageId)
    ElMessage.success('删除成功')
    await loadMessages()
    await loadUnreadCount()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.tab-badge {
  margin-left: 8px;
}

.message-list {
  margin-top: 20px;
}

.message-item {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.3s;
}

.message-item:hover {
  background-color: #f5f7fa;
}

.message-item.unread {
  background-color: #ecf5ff;
}

.message-item.unread:hover {
  background-color: #d9ecff;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.message-type {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4f46e5;
  display: inline-block;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-content {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
}

.message-footer {
  display: flex;
  justify-content: flex-end;
}
</style>

