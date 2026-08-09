<template>
  <div class="device-detail" v-loading="loading">
    <el-card v-if="device.id">
      <el-row :gutter="40">
        <el-col :span="12">
          <el-carousel height="400px">
            <el-carousel-item v-for="(image, index) in images" :key="index">
              <img :src="image" :alt="`设备图片 ${index + 1}`" />
            </el-carousel-item>
          </el-carousel>
        </el-col>
        <el-col :span="12">
          <div class="device-main-info">
            <h1>{{ device.title }}</h1>
            <p class="brand">{{ device.brand }} {{ device.model }}</p>
            <div class="price-box">
              <div class="rent-price">
                <span class="label">租金：</span>
                <span class="amount">¥{{ device.rentPrice }}</span>
                <span class="unit">/天</span>
              </div>
              <div class="deposit">
                <span class="label">押金：</span>
                <span class="amount">¥{{ device.deposit }}</span>
              </div>
            </div>
            <div class="info-item">
              <el-icon><Location /></el-icon>
              <span>{{ device.address }}</span>
            </div>
            <el-divider />
            <div class="actions">
              <el-button
                type="primary"
                size="large"
                @click="handleRent"
                :disabled="device.status !== 'ACTIVE'"
              >
                立即租用
              </el-button>
              <el-button size="large" @click="handleCollect" v-if="userStore.token">
                <el-icon><StarFilled v-if="isCollected" /><Star v-else /></el-icon>
                收藏
              </el-button>
              <el-button
                size="large"
                @click="handleContactSeller"
                v-if="userStore.token && device.userId !== userStore.userInfo.userId"
              >
                <el-icon><ChatDotRound /></el-icon>
                联系卖家
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-divider />

      <div class="device-description">
        <h3>设备详情</h3>
        <p>{{ device.description }}</p>
      </div>

      <el-divider />

      <!-- 评价列表 -->
      <div class="evaluation-section">
        <div class="evaluation-title-row">
          <h3>用户评价</h3>
          <div v-if="evaluations.length > 0" class="avg-score">
            <span class="avg-value">{{ avgScore }}</span>
            <el-rate :model-value="Number(avgScore)" disabled allow-half />
            <span class="avg-count">（{{ totalEvaluationCount }} 条评价）</span>
          </div>
        </div>
        <div v-loading="evaluationsLoading">
          <el-empty v-if="evaluations.length === 0" description="暂无评价" />

          <div v-else class="evaluation-list">
            <div v-for="evaluation in evaluations" :key="evaluation.id" class="evaluation-item">
              <div class="evaluation-header">
                <el-rate v-model="evaluation.score" disabled />
                <span class="evaluation-time">{{ formatTime(evaluation.createdAt) }}</span>
                <el-button
                  v-if="userStore.token && evaluation.evaluatorId === userStore.userInfo.userId"
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
                  style="width: 80px; height: 80px; margin-right: 10px"
                  :preview-src-list="evaluation.imagePaths.split(',')"
                />
              </div>
              <!-- 商家回复 -->
              <div v-if="evaluation.reply" class="evaluation-reply">
                <div class="reply-label">商家回复：</div>
                <div class="reply-content">{{ evaluation.reply }}</div>
              </div>
              <!-- 回复表单（仅设备拥有者可见且未回复时） -->
              <div v-if="isOwner && !evaluation.reply" class="reply-form">
                <el-input
                  v-model="replyText[evaluation.id]"
                  type="textarea"
                  :rows="2"
                  placeholder="回复该评价..."
                  maxlength="500"
                  show-word-limit
                />
                <el-button
                  type="primary"
                  size="small"
                  style="margin-top: 8px"
                  :loading="replying === evaluation.id"
                  @click="handleReply(evaluation.id)"
                >
                  回复
                </el-button>
              </div>
            </div>

            <div v-if="evaluationsTotal > evaluationsSize" class="evaluation-pagination">
              <el-pagination
                :current-page="evaluationsPage"
                :page-size="evaluationsSize"
                :total="evaluationsTotal"
                layout="prev, pager, next"
                @current-change="handleEvaluationsPageChange"
              />
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDeviceDetail } from '@/api/device'
import { favoriteApi } from '@/api/favorite'
import { evaluationApi } from '@/api/evaluation'
import { chatApi } from '@/api/chat'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Location, ChatDotRound } from '@element-plus/icons-vue'
import { formatDate } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const device = ref({})
const isCollected = ref(false)
const evaluations = ref([])
const evaluationsLoading = ref(false)
const evaluationsTotal = ref(0)
const evaluationsPage = ref(1)
const evaluationsSize = ref(10)
const replyText = ref({})
const replying = ref(null)

const images = computed(() => {
  const paths = device.value.imagePaths
  if (!paths) return []
  return Array.isArray(paths) ? paths : paths.split(',')
})

const isOwner = computed(() => {
  return userStore.token && device.value.userId === userStore.userInfo.userId
})

const avgScore = computed(() => {
  if (evaluations.value.length === 0) return 0
  const sum = evaluations.value.reduce((acc, e) => acc + (e.score || 0), 0)
  return (sum / evaluations.value.length).toFixed(1)
})

const totalEvaluationCount = computed(() => evaluationsTotal.value || evaluations.value.length)

const loadDeviceDetail = async () => {
  loading.value = true
  try {
    const res = await getDeviceDetail(route.params.id)
    device.value = res.data

    // 如果已登录，检查是否已收藏
    if (userStore.token) {
      await checkFavoriteStatus()
    }

    // 加载评价列表
    await loadEvaluations()
  } catch (error) {
    console.error(error)
    ElMessage.error('加载设备详情失败')
  } finally {
    loading.value = false
  }
}

const loadEvaluations = async () => {
  evaluationsLoading.value = true
  try {
    const { data } = await evaluationApi.getByDevice(route.params.id, {
      page: evaluationsPage.value,
      size: evaluationsSize.value
    })
    evaluations.value = data.records || []
    evaluationsTotal.value = data.total || 0
  } catch (error) {
    console.error('加载评价失败', error)
  } finally {
    evaluationsLoading.value = false
  }
}

const handleEvaluationsPageChange = (page) => {
  evaluationsPage.value = page
  loadEvaluations()
}

const handleReply = async (evaluationId) => {
  const text = replyText.value[evaluationId]
  if (!text || !text.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = evaluationId
  try {
    await evaluationApi.reply(evaluationId, text.trim())
    ElMessage.success('回复成功')
    replyText.value[evaluationId] = ''
    await loadEvaluations()
  } catch (error) {
    ElMessage.error(error.message || '回复失败')
  } finally {
    replying.value = null
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

const formatTime = (time) => {
  if (!time) return ''
  return formatDate(time, 'YYYY-MM-DD HH:mm')
}

const checkFavoriteStatus = async () => {
  try {
    const { data } = await favoriteApi.check(route.params.id)
    isCollected.value = data
  } catch (error) {
    console.error('检查收藏状态失败', error)
  }
}

const handleRent = () => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  // 跳转到订单创建页面
  router.push(`/devices/${device.value.id}/order`)
}

const handleCollect = async () => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    if (isCollected.value) {
      // 取消收藏
      await favoriteApi.remove(device.value.id)
      isCollected.value = false
      ElMessage.success('取消收藏成功')
    } else {
      // 添加收藏
      await favoriteApi.add(device.value.id)
      isCollected.value = true
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleContactSeller = async () => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    const res = await chatApi.getOrCreateConversation({
      otherUserId: device.value.userId,
      deviceId: device.value.id
    })
    router.push(`/chat/${res.data.id}`)
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}

onMounted(() => {
  loadDeviceDetail()
})
</script>

<style scoped>
.device-main-info h1 {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 10px;
}

.brand {
  color: #94a3b8;
  font-size: 15px;
  margin-bottom: 20px;
}

.price-box {
  background: #f8f9fc;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.rent-price, .deposit {
  margin-bottom: 10px;
}

.label {
  color: #64748b;
  margin-right: 10px;
}

.amount {
  color: #e11d48;
  font-size: 28px;
  font-weight: 700;
}

.unit {
  color: #94a3b8;
  font-size: 15px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  margin-bottom: 10px;
}

.device-description {
  margin-top: 30px;
}

.device-description h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 15px;
}

.device-description p {
  color: #64748b;
  line-height: 1.8;
}

.el-carousel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
}

.actions {
  display: flex;
  gap: 12px;
}

.evaluation-section {
  margin-top: 30px;
}

.evaluation-section h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 0;
}

.evaluation-title-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.avg-score {
  display: flex;
  align-items: center;
  gap: 6px;
}

.avg-value {
  font-size: 22px;
  font-weight: 700;
  color: #f59e0b;
}

.avg-count {
  font-size: 13px;
  color: #94a3b8;
}

.evaluation-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.evaluation-item {
  padding: 20px;
  background: #f8f9fc;
  border-radius: 12px;
}

.evaluation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.evaluation-time {
  font-size: 13px;
  color: #94a3b8;
}

.evaluation-content {
  color: #475569;
  line-height: 1.6;
  margin-bottom: 10px;
}

.evaluation-images {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.evaluation-reply {
  margin-top: 12px;
  padding: 12px;
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

.reply-form {
  margin-top: 12px;
}

.evaluation-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>

