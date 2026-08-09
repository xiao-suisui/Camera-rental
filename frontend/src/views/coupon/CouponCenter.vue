<template>
  <div class="coupon-center">
    <el-card>
      <template #header>
        <h2>优惠券领取中心</h2>
      </template>

      <div v-loading="loading" style="min-height: 400px">
        <el-empty v-if="coupons.length === 0" description="暂无可领取的优惠券" />

        <el-row :gutter="20" v-else>
          <el-col :span="6" v-for="coupon in coupons" :key="coupon.id" style="margin-bottom: 20px">
            <el-card class="coupon-card" :body-style="{ padding: '0' }">
              <div class="coupon-main">
                <div class="coupon-amount">
                  <span class="symbol">¥</span>
                  <span class="value">{{ coupon.denomination }}</span>
                </div>
                <div class="coupon-desc">
                  {{ getCouponDesc(coupon) }}
                </div>
              </div>
              <div class="coupon-footer">
                <div class="coupon-info">
                  <div class="info-item">有效期：{{ coupon.validDays }}天</div>
                  <div class="info-item">剩余：{{ coupon.totalCount - coupon.usedCount }}张</div>
                </div>
                <el-button
                  :type="coupon.received ? 'info' : 'primary'"
                  size="small"
                  @click="handleReceive(coupon)"
                  :disabled="isReceiving || coupon.received"
                >
                  {{ coupon.received ? '已领取' : '立即领取' }}
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { couponApi } from '@/api/coupon'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const isReceiving = ref(false)
const coupons = ref([])

const getCouponDesc = (coupon) => {
  if (coupon.type === 'NO_THRESHOLD') {
    return '无门槛券'
  }
  return `满${coupon.threshold}元可用`
}

onMounted(async () => {
  await loadCoupons()
})

const loadCoupons = async () => {
  loading.value = true
  try {
    const { data } = await couponApi.getAvailable()
    coupons.value = data || []
  } catch (error) {
    ElMessage.error('加载优惠券失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleReceive = async (coupon) => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (coupon.totalCount - coupon.usedCount <= 0) {
    ElMessage.warning('优惠券已被领完')
    return
  }

  isReceiving.value = true
  try {
    await couponApi.receive({
      couponId: coupon.id
    })
    ElMessage.success('领取成功')
    await loadCoupons()
  } catch (error) {
    ElMessage.error(error.message || '领取失败')
  } finally {
    isReceiving.value = false
  }
}
</script>

<style scoped>
h2 {
  margin: 0;
}

.coupon-card {
  border-radius: 14px;
  overflow: hidden;
  transition: transform 0.25s, box-shadow 0.25s;
}

.coupon-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.coupon-main {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: white;
  padding: 28px 20px;
  text-align: center;
}

.coupon-amount {
  margin-bottom: 8px;
}

.coupon-amount .symbol {
  font-size: 18px;
  font-weight: 700;
}

.coupon-amount .value {
  font-size: 44px;
  font-weight: 700;
  margin-left: 4px;
}

.coupon-desc {
  font-size: 14px;
  opacity: 0.85;
}

.coupon-footer {
  padding: 16px 20px;
  background: white;
}

.coupon-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.info-item {
  flex: 1;
}

.coupon-footer .el-button {
  width: 100%;
}
</style>

