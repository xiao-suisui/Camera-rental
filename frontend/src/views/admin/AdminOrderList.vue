<template>
  <div class="admin-order-list">
    <div class="page-card">
      <!-- 筛选 -->
      <el-form :inline="true" :model="searchForm" style="margin-bottom: 16px">
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 160px">
            <el-option label="待支付" value="PENDING_PAY" />
            <el-option label="已支付" value="PAID" />
            <el-option label="待收货" value="TO_SHIP" />
            <el-option label="待归还" value="TO_RETURN" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="逾期" value="OVERDUE" />
            <el-option label="已完成" value="FINISHED" />
            <el-option label="已取消" value="CANCELED" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="输入订单号搜索" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadOrders">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 订单列表 -->
      <el-table :data="orders" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="220" />
        <el-table-column prop="userId" label="租用者ID" width="90" />
        <el-table-column prop="lessorId" label="出租者ID" width="90" />
        <el-table-column prop="deviceId" label="设备ID" width="80" />
        <el-table-column label="租金" width="100">
          <template #default="{ row }">
            ¥{{ row.totalRent }}
          </template>
        </el-table-column>
        <el-table-column label="实付" width="100">
          <template #default="{ row }">
            ¥{{ row.payAmount }}
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="押金状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getDepositType(row.depositStatus)" size="small">{{ getDepositText(row.depositStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
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
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { orderApi } from '@/api/order'

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchForm = ref({ status: '', orderNo: '' })

onMounted(() => {
  loadOrders()
})

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await orderApi.adminList({
      status: searchForm.value.status || undefined,
      orderNo: searchForm.value.orderNo || undefined,
      page: currentPage.value,
      size: pageSize.value
    })
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
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

const getStatusText = (s) => ({
  PENDING_PAY: '待支付', PAID: '已支付', TO_SHIP: '待收货', TO_RETURN: '待归还',
  RETURNED: '已归还', OVERDUE: '逾期', FINISHED: '已完成', CANCELED: '已取消'
})[s] || s

const getStatusType = (s) => ({
  PENDING_PAY: 'warning', PAID: 'primary', TO_RETURN: '', RETURNED: 'info',
  OVERDUE: 'danger', FINISHED: 'success', CANCELED: 'info'
})[s] || 'info'

const getDepositText = (s) => ({ UNPAID: '未付', PAID: '已付', REFUNDED: '已退' })[s] || s
const getDepositType = (s) => ({ UNPAID: 'warning', PAID: 'success', REFUNDED: 'info' })[s] || 'info'
</script>

<style scoped>
.page-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
</style>

