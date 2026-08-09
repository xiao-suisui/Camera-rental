<template>
  <div class="chat-room">
    <div class="chat-header">
      <el-button text @click="$router.push('/chat')">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="chat-title">{{ otherNickname }}</span>
    </div>

    <div v-if="disputeInfo" class="dispute-banner">
      <el-tag :type="DISPUTE_STATUS[disputeInfo.status]?.type || 'info'" size="small">
        纠纷：{{ DISPUTE_STATUS[disputeInfo.status]?.text || disputeInfo.status }}
      </el-tag>
      <span class="dispute-hint">订单 {{ disputeInfo.orderNo }} 存在纠纷工单</span>
    </div>

    <div class="chat-messages" ref="messagesContainer">
      <div v-if="hasMore" class="load-more">
        <el-button text size="small" @click="loadMore">加载更多</el-button>
      </div>
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-row"
        :class="{ 'self': msg.senderId === currentUserId }"
      >
        <div class="avatar-circle small" v-if="msg.senderId !== currentUserId">
          {{ msg.senderNickname?.charAt(0) || '?' }}
        </div>
        <div class="message-bubble" :class="{ 'self-bubble': msg.senderId === currentUserId }">
          <div class="msg-content">{{ msg.content }}</div>
          <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
        </div>
        <div class="avatar-circle small" v-if="msg.senderId === currentUserId">
          {{ currentUserNickname?.charAt(0) || '?' }}
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="inputMessage"
        placeholder="输入消息..."
        @keyup.enter="sendMessage"
        :maxlength="2000"
        clearable
      />
      <el-button type="primary" @click="sendMessage" :disabled="!inputMessage.trim()">
        发送
      </el-button>
      <el-button
        v-if="canApplyDispute"
        type="warning"
        @click="showDisputeDialog = true"
      >
        申请平台介入
      </el-button>
    </div>

    <!-- 纠纷申请弹窗 -->
    <el-dialog v-model="showDisputeDialog" title="申请平台介入" width="500px">
      <el-form :model="disputeForm" label-width="80px">
        <el-form-item label="关联订单">
          <span>{{ disputeForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="纠纷类型">
          <el-radio-group v-model="disputeForm.disputeType">
            <el-radio v-for="(label, key) in DISPUTE_TYPE" :key="key" :value="key">
              {{ label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input
            v-model="disputeForm.description"
            type="textarea"
            :rows="4"
            placeholder="请详细描述纠纷情况（至少10字）"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDisputeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitDispute" :loading="submittingDispute">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { chatApi } from '@/api/chat'
import { disputeApi } from '@/api/dispute'
import { orderApi } from '@/api/order'
import { DISPUTE_TYPE, DISPUTE_STATUS } from '@/constants'
import eventBus from '@/utils/eventBus'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const userStore = useUserStore()

const conversationId = computed(() => route.params.conversationId)
const currentUserId = computed(() => userStore.userInfo.userId)
const currentUserNickname = computed(() => userStore.userInfo.nickname)

const messages = ref([])
const inputMessage = ref('')
const otherNickname = ref('聊天')
const otherUserId = ref(null)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = 20
const messagesContainer = ref(null)

const orderId = ref(null)
const orderNo = ref(null)
const orderStatus = ref(null)
const disputeInfo = ref(null)
const showDisputeDialog = ref(false)
const submittingDispute = ref(false)
const disputeForm = ref({
  orderId: null,
  orderNo: '',
  disputeType: 'OTHER',
  description: ''
})

const canApplyDispute = computed(() => {
  if (!orderId.value || disputeInfo.value) return false
  return ['PAID', 'TO_SHIP', 'TO_RETURN'].includes(orderStatus.value)
})

let ws = null
let reconnectTimer = null
let isUnmounted = false
let reconnectAttempts = 0
const MAX_RECONNECT = 5

const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).format('MM-DD HH:mm')
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const loadMessages = async (page = 1) => {
  try {
    const res = await chatApi.getMessages(conversationId.value, page, pageSize)
    const newMessages = res.data || []
    if (page === 1) {
      messages.value = newMessages
      scrollToBottom()
    } else {
      messages.value = [...newMessages, ...messages.value]
    }
    hasMore.value = newMessages.length >= pageSize
    currentPage.value = page
  } catch (e) {
    console.error('Failed to load messages:', e)
  }
}

const loadMore = () => {
  loadMessages(currentPage.value + 1)
}

const loadConversationInfo = async () => {
  try {
    const res = await chatApi.getConversation(conversationId.value)
    const conv = res.data
    if (conv) {
      otherNickname.value = conv.otherNickname
      otherUserId.value = conv.otherUserId
      if (conv.orderId) {
        orderId.value = conv.orderId
        loadOrderAndDispute(conv.orderId)
      }
    }
  } catch (e) {
    console.error('Failed to load conversation info:', e)
    try {
      const listRes = await chatApi.getConversations()
      const convs = listRes.data || []
      const conv = convs.find(c => c.id === Number(conversationId.value))
      if (conv) {
        otherNickname.value = conv.otherNickname
        otherUserId.value = conv.otherUserId
        if (conv.orderId) {
          orderId.value = conv.orderId
          loadOrderAndDispute(conv.orderId)
        }
      }
    } catch (listErr) {
      console.error('Failed to load conversation list:', listErr)
    }
  }
}

const loadOrderAndDispute = async (oid) => {
  try {
    const orderRes = await orderApi.getDetail(oid)
    if (orderRes.data) {
      orderStatus.value = orderRes.data.status
      orderNo.value = orderRes.data.orderNo
      disputeForm.value.orderId = oid
      disputeForm.value.orderNo = orderRes.data.orderNo
    }
  } catch (e) {
    console.error('Failed to load order info:', e)
  }
  try {
    const disputeRes = await disputeApi.getByOrderId(oid)
    disputeInfo.value = disputeRes.data || null
  } catch (e) {
    disputeInfo.value = null
  }
}

const submitDispute = async () => {
  if (!disputeForm.value.disputeType) {
    ElMessage.warning('请选择纠纷类型')
    return
  }
  if (disputeForm.value.description.length < 10) {
    ElMessage.warning('纠纷描述至少10个字')
    return
  }
  submittingDispute.value = true
  try {
    await disputeApi.create({
      orderId: disputeForm.value.orderId,
      disputeType: disputeForm.value.disputeType,
      description: disputeForm.value.description
    })
    ElMessage.success('纠纷已提交，平台将尽快处理')
    showDisputeDialog.value = false
    disputeForm.value.description = ''
    disputeForm.value.disputeType = 'OTHER'
    loadOrderAndDispute(disputeForm.value.orderId)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '提交失败')
  } finally {
    submittingDispute.value = false
  }
}

const disconnectWebSocket = () => {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (ws) {
    ws.close()
    ws = null
  }
}

const initChat = () => {
  disconnectWebSocket()
  messages.value = []
  otherNickname.value = '聊天'
  otherUserId.value = null
  hasMore.value = true
  currentPage.value = 1
  reconnectAttempts = 0
  isUnmounted = false

  loadConversationInfo()
  loadMessages(1)
  connectWebSocket()
  chatApi.markAsRead(conversationId.value).catch(() => {})
}

const connectWebSocket = () => {
  if (ws && ws.readyState === WebSocket.OPEN) return

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  const token = userStore.token
  ws = new WebSocket(`${protocol}//${host}/api/ws/chat?token=${token}`)

  ws.onopen = () => {
    console.log('WebSocket connected')
    reconnectAttempts = 0
  }

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)

      // 心跳 pong 响应
      if (data.type === 'ping') {
        ws.send(JSON.stringify({ type: 'pong' }))
        return
      }

      // 非当前会话消息 - 通知刷新未读计数
      if (data.conversationId && data.conversationId !== Number(conversationId.value)) {
        eventBus.emit('chat:message')
        return
      }

      // 避免重复添加已发送的消息
      const exists = messages.value.some(m => m.id === data.id)
      if (!exists) {
        messages.value.push(data)
        scrollToBottom()
      }
      // 标记已读
      if (data.senderId !== currentUserId.value) {
        chatApi.markAsRead(conversationId.value).catch(() => {})
      }
    } catch (e) {
      console.error('Parse message error:', e)
    }
  }

  ws.onclose = () => {
    console.log('WebSocket disconnected')
    if (isUnmounted) return
    if (reconnectAttempts >= MAX_RECONNECT) return
    reconnectAttempts++
    const delay = Math.min(3000 * Math.pow(2, reconnectAttempts - 1), 30000)
    reconnectTimer = setTimeout(() => {
      connectWebSocket()
    }, delay)
  }

  ws.onerror = (error) => {
    console.error('WebSocket error:', error)
  }
}

const sendMessage = () => {
  console.log('sendMessage called', {
    message: inputMessage.value.trim(),
    wsReady: ws?.readyState === WebSocket.OPEN,
    otherUserId: otherUserId.value,
    conversationId: conversationId.value
  })
  if (!inputMessage.value.trim() || !ws || ws.readyState !== WebSocket.OPEN) return
  if (!otherUserId.value) return

  const payload = {
    conversationId: Number(conversationId.value),
    receiverId: otherUserId.value,
    content: inputMessage.value.trim()
  }
  ws.send(JSON.stringify(payload))
  inputMessage.value = ''
}

onMounted(() => {
  initChat()
})

watch(conversationId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    initChat()
  }
})

onUnmounted(() => {
  isUnmounted = true
  disconnectWebSocket()
})
</script>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.dispute-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #fef3c7;
  border-bottom: 1px solid #fde68a;
  flex-shrink: 0;
}

.dispute-hint {
  font-size: 13px;
  color: #92400e;
}

.chat-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-left: 8px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f7f8fa;
}

.load-more {
  text-align: center;
  margin-bottom: 16px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
}

.message-row.self {
  flex-direction: row-reverse;
}

.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.avatar-circle.small {
  width: 36px;
  height: 36px;
}

.message-bubble {
  max-width: 60%;
  margin: 0 10px;
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.self-bubble {
  background: #409eff;
  color: #fff;
}

.self-bubble .msg-time {
  color: rgba(255, 255, 255, 0.7);
}

.msg-content {
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word;
}

.msg-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  text-align: right;
}

.chat-input {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  gap: 10px;
  flex-shrink: 0;
}

.chat-input .el-input {
  flex: 1;
}
</style>
