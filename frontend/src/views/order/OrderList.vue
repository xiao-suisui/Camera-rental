<template>
  <div class="order-list">
    <el-card>
      <h2>我的订单</h2>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="searchForm" style="margin-bottom: 20px">
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="待支付" value="PENDING_PAY" />
            <el-option label="已支付" value="PAID" />
            <el-option label="待收货" value="TO_SHIP" />
            <el-option label="待归还" value="TO_RETURN" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="逾期归还" value="OVERDUE" />
            <el-option label="已完成" value="FINISHED" />
            <el-option label="已取消" value="CANCELED" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="搜索订单号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadOrders">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="orders" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="设备信息" width="120">
          <template #default="{ row }">
            <div class="device-info">
              <span>设备ID: {{ row.deviceId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="租赁时间" width="200">
          <template #default="{ row }">
            <div>{{ formatDate(row.startTime) }}</div>
            <div>至</div>
            <div>{{ formatDate(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="150">
          <template #default="{ row }">
            <div>租金: ¥{{ row.totalRent }}</div>
            <div v-if="row.discountAmount > 0" style="color: #67c23a">
              优惠: -¥{{ row.discountAmount }}
            </div>
            <div style="color: #f56c6c; font-weight: bold">
              实付: ¥{{ row.payAmount }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="押金" width="120">
          <template #default="{ row }">
            <div>¥{{ row.deposit }}</div>
            <el-tag :type="getDepositStatusType(row.depositStatus)" size="small">
              {{ getDepositStatusText(row.depositStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="viewDetail(row.id)"
            >
              查看详情
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_PAY'"
              type="success"
              size="small"
              @click="handlePay(row.id)"
            >
              支付
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadOrders"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '@/api/order'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDateTime } from '@/utils/date'
import { ORDER_STATUS, DEPOSIT_STATUS } from '@/constants'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const orders = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const searchForm = ref({
  status: '',
  orderNo: ''
})

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await orderApi.getList({
      status: searchForm.value.status || undefined,
      orderNo: searchForm.value.orderNo || undefined,
      page: currentPage.value,
      size: pageSize.value
    })
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error(error)
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadOrders()
}

const handleReset = () => {
  searchForm.value = { status: '', orderNo: '' }
  currentPage.value = 1
  loadOrders()
}

const formatDate = (date) => {
  return formatDateTime(date)
}

const getStatusType = (status) => {
  return ORDER_STATUS[status]?.type || 'info'
}

const getStatusText = (status) => {
  return ORDER_STATUS[status]?.text || status
}

const getDepositStatusType = (status) => {
  return DEPOSIT_STATUS[status]?.type || 'info'
}

const getDepositStatusText = (status) => {
  return DEPOSIT_STATUS[status]?.text || status
}

const handlePay = async (orderId) => {
  try {
    await ElMessageBox.confirm('确认支付该订单？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderApi.pay(orderId)
    ElMessage.success('支付成功')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('支付失败')
      console.error(error)
    }
  }
}

const viewDetail = (orderId) => {
  router.push(`/orders/${orderId}`)
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
h2 {
  margin-bottom: 20px;
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
</style>

