<template>
  <div class="order-create">
    <el-card v-loading="loading">
      <template #header>创建订单</template>

      <!-- 设备信息 -->
      <div class="device-info" v-if="device.id">
        <el-image
          :src="getFirstImage(device.imagePaths)"
          style="width: 120px; height: 120px"
          fit="cover"
        />
        <div class="device-detail">
          <h3>{{ device.title }}</h3>
          <p>{{ device.brand }} {{ device.model }}</p>
          <p class="price">日租金: ￥{{ device.rentPrice }}/天</p>
          <p class="deposit">押金: ￥{{ device.deposit }}</p>
        </div>
      </div>

      <el-divider />

      <!-- 租期选择 -->
      <el-form label-width="100px">
        <el-form-item label="租用时间" required>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :disabled-date="disabledDate"
            @change="calculateAmount"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="租期天数">
          <span class="days">{{ days }} 天</span>
        </el-form-item>

        <el-form-item label="租金">
          <span class="amount">￥{{ totalRent }}</span>
          <span class="hint">({{ days }}天 × ￥{{ device.rentPrice }}/天)</span>
        </el-form-item>

        <el-form-item label="押金">
          <span class="amount">￥{{ device.deposit }}</span>
        </el-form-item>

        <!-- 优惠券选择 -->
        <el-form-item label="优惠券">
          <el-select
            v-model="selectedCoupon"
            placeholder="选择优惠券"
            @change="calculateAmount"
            style="width: 300px"
          >
            <el-option label="不使用优惠券" :value="null" />
            <el-option
              v-for="coupon in availableCoupons"
              :key="coupon.id"
              :label="getCouponLabel(coupon)"
              :value="coupon.couponId"
              :disabled="isDisabled(coupon)"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="优惠" v-if="discountAmount > 0">
          <span class="discount">-￥{{ discountAmount }}</span>
        </el-form-item>

        <el-form-item label="实付金额">
          <span class="pay-amount">￥{{ payAmount }}</span>
        </el-form-item>
      </el-form>

      <div class="submit-button">
        <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
          提交订单
        </el-button>
        <el-button size="large" @click="$router.back()">
          取消
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deviceApi } from '@/api/device'
import { orderApi } from '@/api/order'
import { couponApi } from '@/api/coupon'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const device = ref({})
const dateRange = ref([])
const selectedCoupon = ref(null)
const availableCoupons = ref([])

const days = computed(() => {
  if (!dateRange.value || dateRange.value.length !== 2) return 0
  const start = new Date(dateRange.value[0])
  const end = new Date(dateRange.value[1])
  return Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1
})

const totalRent = computed(() => {
  if (!device.value.rentPrice) return '0.00'
  return (days.value * device.value.rentPrice).toFixed(2)
})

const discountAmount = computed(() => {
  if (!selectedCoupon.value || !availableCoupons.value.length) return '0.00'
  const coupon = availableCoupons.value.find(c => c.couponId === selectedCoupon.value)
  if (!coupon) return '0.00'

  const rent = parseFloat(totalRent.value)
  // 如果是满减券，检查是否达到门槛
  if (coupon.threshold && rent < coupon.threshold) return '0.00'

  return Math.min(coupon.denomination, rent).toFixed(2)
})

const payAmount = computed(() => {
  const rent = parseFloat(totalRent.value)
  const discount = parseFloat(discountAmount.value)
  return Math.max(0, rent - discount).toFixed(2)
})

const getFirstImage = (imagePaths) => {
  if (!imagePaths) return ''
  const paths = imagePaths.split(',')
  return paths[0] || ''
}

const getCouponLabel = (coupon) => {
  if (coupon.type === 'NO_THRESHOLD') {
    return `无门槛券 - ${coupon.denomination}元`
  }
  return `满${coupon.threshold}减${coupon.denomination}元`
}

const isDisabled = (coupon) => {
  if (!coupon.threshold) return false
  return parseFloat(totalRent.value) < coupon.threshold
}

onMounted(async () => {
  await Promise.all([loadDevice(), loadCoupons()])
})

const loadDevice = async () => {
  try {
    loading.value = true
    const { data } = await deviceApi.getById(route.params.deviceId)
    device.value = data

    // 检查设备是否可租
    if (data.status !== 'ACTIVE') {
      ElMessage.error('该设备暂不可租用')
      router.back()
    }
  } catch (error) {
    ElMessage.error('加载设备信息失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const loadCoupons = async () => {
  try {
    const { data } = await couponApi.getUserCoupons()
    // 只显示未使用的优惠券
    availableCoupons.value = data.filter(c => c.useStatus === 'UNUSED')
  } catch (error) {
    console.error('加载优惠券失败', error)
  }
}

const disabledDate = (date) => {
  // 禁用今天之前的日期
  return date < new Date(new Date().setHours(0, 0, 0, 0))
}

const calculateAmount = () => {
  // 触发计算，computed会自动更新
}

const handleSubmit = async () => {
  // 验证
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择租用时间')
    return
  }

  if (days.value < 1) {
    ElMessage.warning('租期至少为1天')
    return
  }

  try {
    submitting.value = true

    const request = {
      deviceId: device.value.id,
      startTime: dateRange.value[0],
      endTime: dateRange.value[1],
      couponId: selectedCoupon.value
    }

    const { data } = await orderApi.create(request)
    ElMessage.success('订单创建成功')

    // 跳转到订单列表或订单详情
    router.push('/orders')
  } catch (error) {
    ElMessage.error(error.message || '创建订单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.device-info {
  display: flex;
  gap: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.device-detail h3 {
  margin: 0 0 10px 0;
}

.device-detail p {
  margin: 5px 0;
}

.price, .deposit {
  font-size: 16px;
  color: #f56c6c;
  font-weight: bold;
}

.days {
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}

.amount, .pay-amount {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
}

.discount {
  font-size: 18px;
  color: #67c23a;
  font-weight: bold;
}

.hint {
  color: #909399;
  margin-left: 10px;
  font-size: 14px;
}

.submit-button {
  margin-top: 30px;
  text-align: center;
}
</style>
