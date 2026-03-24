<template>
  <div class="order-list">
    <el-card>
      <h2>我出租的订单</h2>

      <el-form :inline="true" :model="searchForm" style="margin-bottom: 20px">
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="已支付" value="PAID" />
            <el-option label="待收货" value="TO_SHIP" />
            <el-option label="使用中" value="TO_RETURN" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="逾期归还" value="OVERDUE" />
            <el-option label="已完成" value="FINISHED" />
            <el-option label="已取消" value="CANCELED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadOrders">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="orders" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="210" />
        <el-table-column prop="userId" label="租用者ID" width="110" />
        <el-table-column prop="deviceId" label="设备ID" width="100" />
        <el-table-column label="租期" width="200">
          <template #default="{ row }">
            <div>{{ formatDate(row.startTime) }}</div>
            <div>至 {{ formatDate(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="实付金额" width="120">
          <template #default="{ row }">¥{{ row.payAmount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { orderApi } from '@/api/order'
import { ElMessage } from 'element-plus'
import { ORDER_STATUS } from '@/constants'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const orders = ref([])

const searchForm = ref({
  status: ''
})

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await orderApi.getLessorList({
      lessorId: userStore.userInfo.userId,
      page: 1,
      size: 20
    })
    const records = res.data?.records || []
    orders.value = searchForm.value.status
      ? records.filter(item => item.status === searchForm.value.status)
      : records
  } catch (error) {
    ElMessage.error('加载出租订单失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.value.status = ''
  loadOrders()
}

const formatDate = (date) => formatDateTime(date)
const getStatusType = (status) => ORDER_STATUS[status]?.type || 'info'
const getStatusText = (status) => ORDER_STATUS[status]?.text || status
const viewDetail = (orderId) => router.push(`/orders/${orderId}`)

onMounted(loadOrders)
</script>

<style scoped>
h2 {
  margin-bottom: 20px;
}
</style>

