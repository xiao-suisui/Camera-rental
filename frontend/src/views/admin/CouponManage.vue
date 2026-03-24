<template>
  <div class="coupon-manage">
    <div class="page-card">
      <div class="toolbar">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon> 创建优惠券
        </el-button>
      </div>

      <!-- 优惠券列表 -->
      <el-table :data="coupons" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            {{ row.type === 'NO_THRESHOLD' ? '无门槛券' : '满减券' }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="150">
          <template #default="{ row }">
            <span v-if="row.type === 'NO_THRESHOLD'">{{ row.denomination }}元</span>
            <span v-else>满{{ row.threshold }}减{{ row.denomination }}元</span>
          </template>
        </el-table-column>
        <el-table-column label="发放/使用" width="150">
          <template #default="{ row }">
            {{ row.usedCount }} / {{ row.totalCount }}
          </template>
        </el-table-column>
        <el-table-column prop="validDays" label="有效天数" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PUBLISHED'"
              type="danger"
              size="small"
              @click="handleOffline(row)"
            >
              下架
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建优惠券对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建优惠券" width="500px">
      <el-form :model="couponForm" label-width="100px">
        <el-form-item label="优惠券类型">
          <el-radio-group v-model="couponForm.type">
            <el-radio label="NO_THRESHOLD">无门槛券</el-radio>
            <el-radio label="THRESHOLD">满减券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="面额">
          <el-input-number v-model="couponForm.denomination" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="门槛金额" v-if="couponForm.type === 'THRESHOLD'">
          <el-input-number v-model="couponForm.threshold" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="发放数量">
          <el-input-number v-model="couponForm.totalCount" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="有效天数">
          <el-input-number v-model="couponForm.validDays" :min="1" :max="365" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { couponApi } from '@/api/coupon'

const loading = ref(false)
const coupons = ref([])
const createDialogVisible = ref(false)
const couponForm = ref({
  type: 'NO_THRESHOLD',
  denomination: 10,
  threshold: 0,
  totalCount: 100,
  validDays: 7
})

onMounted(() => {
  loadCoupons()
})

const loadCoupons = async () => {
  loading.value = true
  try {
    const res = await couponApi.adminList()
    coupons.value = res.data || []
  } catch (error) {
    ElMessage.error('加载优惠券列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  couponForm.value = {
    type: 'NO_THRESHOLD',
    denomination: 10,
    threshold: 0,
    totalCount: 100,
    validDays: 7
  }
  createDialogVisible.value = true
}

const confirmCreate = async () => {
  try {
    await couponApi.create(couponForm.value)
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    loadCoupons()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const handleOffline = async (coupon) => {
  try {
    await ElMessageBox.confirm('确认下架该优惠券？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await couponApi.adminOffline(coupon.id)
    ElMessage.success('下架成功')
    loadCoupons()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const getStatusText = (status) => {
  const map = { 'DRAFT': '草稿', 'PUBLISHED': '已发布', 'EXPIRED': '已过期', 'OFFLINE': '已下架' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { 'DRAFT': 'info', 'PUBLISHED': 'success', 'EXPIRED': 'danger', 'OFFLINE': 'warning' }
  return map[status] || 'info'
}
</script>

<style scoped>
.page-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
</style>

