<template>
  <div class="coupon-list">
    <el-card>
      <h2>我的优惠券</h2>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="可用优惠券" name="unused">
          <el-empty v-if="unusedCoupons.length === 0" description="暂无可用优惠券" />
          <el-row :gutter="20" v-else>
            <el-col :span="8" v-for="coupon in unusedCoupons" :key="coupon.id">
              <div class="coupon-card available">
                <div class="coupon-left">
                  <div class="amount">
                    <span class="symbol">¥</span>
                    <span class="value">{{ coupon.denomination }}</span>
                  </div>
                  <div class="threshold" v-if="coupon.threshold">
                    满{{ coupon.threshold }}可用
                  </div>
                  <div class="threshold" v-else>
                    无门槛券
                  </div>
                </div>
                <div class="coupon-right">
                  <div class="coupon-info">
                    <div class="type">{{ getCouponTypeText(coupon.type) }}</div>
                    <div class="expire-time">
                      有效期至: {{ formatDate(coupon.expireTime) }}
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="已使用" name="used">
          <el-empty v-if="usedCoupons.length === 0" description="暂无已使用优惠券" />
          <el-row :gutter="20" v-else>
            <el-col :span="8" v-for="coupon in usedCoupons" :key="coupon.id">
              <div class="coupon-card used">
                <div class="coupon-left">
                  <div class="amount">
                    <span class="symbol">¥</span>
                    <span class="value">{{ coupon.denomination }}</span>
                  </div>
                  <div class="status-tag">已使用</div>
                </div>
                <div class="coupon-right">
                  <div class="coupon-info">
                    <div class="type">{{ getCouponTypeText(coupon.type) }}</div>
                    <div class="use-time">
                      使用时间: {{ formatDate(coupon.receiveTime) }}
                    </div>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserCoupons } from '@/api/coupon'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
const activeTab = ref('unused')
const coupons = ref([])

const unusedCoupons = computed(() => {
  return coupons.value.filter(c => c.useStatus === 'UNUSED')
})

const usedCoupons = computed(() => {
  return coupons.value.filter(c => c.useStatus === 'USED')
})

const loadCoupons = async () => {
  try {
    const res = await getUserCoupons()
    coupons.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const getCouponTypeText = (type) => {
  return type === 'NO_THRESHOLD' ? '无门槛券' : '满减券'
}

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD')
}

onMounted(() => {
  loadCoupons()
})
</script>

<style scoped>
h2 {
  margin-bottom: 20px;
}

.coupon-card {
  display: flex;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.coupon-card.available {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.coupon-card.used {
  background: #f5f5f5;
  color: #999;
}

.coupon-left {
  flex: 0 0 150px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-right: 2px dashed rgba(255,255,255,0.3);
  padding: 20px;
}

.amount {
  display: flex;
  align-items: baseline;
}

.symbol {
  font-size: 20px;
  margin-right: 5px;
}

.value {
  font-size: 48px;
  font-weight: bold;
}

.threshold {
  margin-top: 10px;
  font-size: 14px;
  opacity: 0.9;
}

.status-tag {
  margin-top: 10px;
  font-size: 14px;
  padding: 4px 12px;
  background: rgba(0,0,0,0.1);
  border-radius: 12px;
}

.coupon-right {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.coupon-info .type {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
}

.expire-time, .use-time {
  font-size: 13px;
  opacity: 0.8;
}
</style>

