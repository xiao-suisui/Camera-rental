<template>
  <div class="order-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>订单详情</span>
          <el-tag :type="getStatusType(order.status)">
            {{ getStatusText(order.status) }}
          </el-tag>
        </div>
      </template>

      <!-- 订单基本信息 -->
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusType(order.status)">{{ getStatusText(order.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(order.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="租期">
          {{ formatDate(order.startTime) }} 至 {{ formatDate(order.endTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="租金">￥{{ order.totalRent }}</el-descriptions-item>
        <el-descriptions-item label="押金">
          ￥{{ order.deposit }}
          <el-tag v-if="order.depositStatus === 'REFUNDED'" type="success" size="small">已退还</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="优惠金额" v-if="order.discountAmount > 0">
          -￥{{ order.discountAmount }}
        </el-descriptions-item>
        <el-descriptions-item label="实付金额">
          <span class="pay-amount">￥{{ order.payAmount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="物流单号" v-if="order.virtualOrderNo">
          {{ order.virtualOrderNo }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 设备信息 -->
      <el-divider>设备信息</el-divider>
      <div class="device-info" v-if="device.id">
        <el-image
          :src="getFirstImage(device.imagePaths)"
          style="width: 100px; height: 100px"
          fit="cover"
        />
        <div class="device-detail">
          <h3>{{ device.title }}</h3>
          <p>{{ device.brand }} {{ device.model }}</p>
          <p>日租金: ￥{{ device.rentPrice }}/天</p>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <!-- 租用者：待支付 -->
        <el-button
          v-if="order.status === 'PENDING_PAY' && !isLessor"
          type="primary"
          @click="handlePay"
        >
          立即支付
        </el-button>
        <el-button
          v-if="order.status === 'PENDING_PAY' && !isLessor"
          @click="handleCancel"
        >
          取消订单
        </el-button>

        <!-- 出租者：已支付，待发货 -->
        <el-button
          v-if="order.status === 'PAID' && isLessor"
          type="success"
          @click="handleShip"
        >
          确认发货
        </el-button>

        <!-- 租用者：待收货 -->
        <el-button
          v-if="order.status === 'TO_SHIP' && !isLessor"
          type="primary"
          @click="handleReceive"
        >
          确认收货
        </el-button>

        <!-- 租用者：使用中，待归还 -->
        <el-button
          v-if="order.status === 'TO_RETURN' && !isLessor"
          type="warning"
          @click="handleReturn"
        >
          申请归还
        </el-button>

        <!-- 出租者：已归还，待确认 -->
        <el-button
          v-if="(order.status === 'RETURNED' || order.status === 'OVERDUE') && isLessor"
          type="success"
          @click="handleConfirm"
        >
          确认归还
        </el-button>

        <!-- 租用者：已完成，可评价 -->
        <el-button
          v-if="order.status === 'FINISHED' && !isLessor && !hasEvaluated"
          type="primary"
          @click="handleEvaluate"
        >
          评价订单
        </el-button>

        <el-button
          v-if="order.userId && order.lessorId"
          @click="handleContact"
        >
          <el-icon><ChatDotRound /></el-icon>
          联系对方
        </el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </el-card>

    <!-- 评价列表 -->
    <el-card v-if="evaluations.length > 0" class="evaluations-card">
      <template #header>
        <span>订单评价</span>
      </template>
      <div v-loading="evaluationsLoading">
        <div v-for="evaluation in evaluations" :key="evaluation.id" class="evaluation-item">
          <div class="evaluation-header">
            <span class="evaluation-role">
              {{ evaluation.evaluatorId === userStore.userInfo.userId ? '我的评价' : '对方的评价' }}
            </span>
            <el-rate v-model="evaluation.score" disabled />
            <span class="evaluation-time">{{ formatTime(evaluation.createdAt) }}</span>
            <el-button
              v-if="evaluation.evaluatorId === userStore.userInfo.userId"
              type="danger"
              link
              size="small"
              @click="handleDeleteEvaluation(evaluation.id)"
            >
              删除
            </el-button>
          </div>
          <div class="evaluation-content">{{ evaluation.content }}</div>
          <div v-if="evaluation.imagePaths" class="evaluation-images">
            <el-image
              v-for="(image, index) in evaluation.imagePaths.split(',')"
              :key="index"
              :src="image"
              fit="cover"
              style="width: 60px; height: 60px; margin-right: 8px"
              :preview-src-list="evaluation.imagePaths.split(',')"
            />
          </div>
          <div v-if="evaluation.reply" class="evaluation-reply">
            <div class="reply-label">商家回复：</div>
            <div class="reply-content">{{ evaluation.reply }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 发货对话框 -->
    <el-dialog v-model="shipDialogVisible" title="确认发货" width="400px">
      <el-form :model="shipForm" label-width="80px">
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.trackingNumber" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmShip">确定</el-button>
      </template>
    </el-dialog>

    <!-- 归还对话框 -->
    <el-dialog v-model="returnDialogVisible" title="申请归还" width="400px">
      <el-form :model="returnForm" label-width="80px">
        <el-form-item label="物流单号">
          <el-input v-model="returnForm.trackingNumber" placeholder="请输入归还物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReturn">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { orderApi } from '@/api/order'
import { deviceApi } from '@/api/device'
import { evaluationApi } from '@/api/evaluation'
import { chatApi } from '@/api/chat'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ORDER_STATUS } from '@/constants'
import { ChatDotRound } from '@element-plus/icons-vue'
import { formatDate as formatDateUtil } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const order = ref({})
const device = ref({})
const evaluations = ref([])
const evaluationsLoading = ref(false)
const shipDialogVisible = ref(false)
const returnDialogVisible = ref(false)
const shipForm = ref({ trackingNumber: '' })
const returnForm = ref({ trackingNumber: '' })

const isLessor = computed(() => order.value.lessorId === userStore.userInfo.userId)

const hasEvaluated = computed(() => {
  return evaluations.value.some(e => e.evaluatorId === userStore.userInfo.userId)
})

const getStatusText = (status) => ORDER_STATUS[status]?.text || status
const getStatusType = (status) => ORDER_STATUS[status]?.type || 'info'

const formatTime = (time) => {
  if (!time) return ''
  return formatDateUtil(time, 'YYYY-MM-DD HH:mm:ss')
}

const formatDate = (time) => {
  if (!time) return ''
  return formatDateUtil(time, 'YYYY-MM-DD')
}

const getFirstImage = (imagePaths) => {
  if (!imagePaths) return ''
  const paths = imagePaths.split(',')
  return paths[0] || ''
}

onMounted(async () => {
  await loadOrderDetail()
})

const loadOrderDetail = async () => {
  try {
    loading.value = true
    const { data } = await orderApi.getDetail(route.params.id)
    order.value = data

    // 加载设备信息
    const deviceRes = await deviceApi.getById(data.deviceId)
    device.value = deviceRes.data

    // 加载评价信息
    await loadEvaluations()
  } catch (error) {
    ElMessage.error('加载订单详情失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadEvaluations = async () => {
  evaluationsLoading.value = true
  try {
    const { data } = await evaluationApi.getByOrder(route.params.id)
    evaluations.value = data || []
  } catch (error) {
    console.error('加载评价失败', error)
  } finally {
    evaluationsLoading.value = false
  }
}

const handleDeleteEvaluation = async (evaluationId) => {
  try {
    await ElMessageBox.confirm('确认删除该评价？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await evaluationApi.delete(evaluationId)
    ElMessage.success('删除成功')
    await loadEvaluations()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handlePay = async () => {
  try {
    await ElMessageBox.confirm('确认支付此订单？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderApi.pay(order.value.id)
    ElMessage.success('支付成功')
    await loadOrderDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('支付失败')
    }
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确认取消此订单？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderApi.cancel(order.value.id)
    ElMessage.success('订单已取消')
    await loadOrderDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消订单失败')
    }
  }
}

const handleShip = () => {
  shipDialogVisible.value = true
}

const confirmShip = async () => {
  if (!shipForm.value.trackingNumber) {
    ElMessage.warning('请输入物流单号')
    return
  }

  try {
    await orderApi.ship(order.value.id, shipForm.value)
    ElMessage.success('发货成功')
    shipDialogVisible.value = false
    shipForm.value.trackingNumber = ''
    await loadOrderDetail()
  } catch (error) {
    ElMessage.error('发货失败')
  }
}

const handleReturn = () => {
  returnDialogVisible.value = true
}

const handleReceive = async () => {
  try {
    await ElMessageBox.confirm('确认已收到出租者发货设备？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderApi.receive(order.value.id)
    ElMessage.success('确认收货成功，订单已进入使用中')
    await loadOrderDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('确认收货失败')
    }
  }
}

const confirmReturn = async () => {
  if (!returnForm.value.trackingNumber) {
    ElMessage.warning('请输入物流单号')
    return
  }

  try {
    await orderApi.applyReturn(order.value.id, returnForm.value)
    ElMessage.success('归还申请已提交')
    returnDialogVisible.value = false
    returnForm.value.trackingNumber = ''
    await loadOrderDetail()
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const handleConfirm = async () => {
  try {
    await ElMessageBox.confirm('确认已收到归还的设备？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderApi.confirmReturn(order.value.id, { condition: 'normal' })
    ElMessage.success('已确认归还，押金已退还')
    await loadOrderDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('确认失败')
    }
  }
}

const handleEvaluate = () => {
  router.push(`/orders/${order.value.id}/evaluate`)
}

const handleContact = async () => {
  const otherUserId = isLessor.value ? order.value.userId : order.value.lessorId
  try {
    const res = await chatApi.getOrCreateConversation({
      otherUserId,
      orderId: order.value.id
    })
    router.push(`/chat/${res.data.id}`)
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.device-info {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}

.device-detail h3 {
  margin: 0 0 10px 0;
}

.device-detail p {
  margin: 5px 0;
  color: #666;
}

.pay-amount {
  font-size: 18px;
  color: #f56c6c;
  font-weight: bold;
}

.action-buttons {
  margin-top: 30px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

.evaluations-card {
  margin-top: 20px;
}

.evaluation-item {
  padding: 16px;
  background: #f8f9fc;
  border-radius: 10px;
  margin-bottom: 12px;
}

.evaluation-item:last-child {
  margin-bottom: 0;
}

.evaluation-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.evaluation-role {
  font-weight: 600;
  color: #1a1a2e;
  font-size: 14px;
}

.evaluation-time {
  font-size: 13px;
  color: #94a3b8;
  margin-left: auto;
}

.evaluation-content {
  color: #475569;
  line-height: 1.6;
  margin-bottom: 10px;
}

.evaluation-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.evaluation-reply {
  margin-top: 10px;
  padding: 10px;
  background: #eef2ff;
  border-radius: 8px;
  border-left: 3px solid #6366f1;
}

.reply-label {
  font-size: 13px;
  font-weight: 600;
  color: #6366f1;
  margin-bottom: 4px;
}

.reply-content {
  color: #475569;
  line-height: 1.6;
}
</style>
