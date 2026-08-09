<template>
  <div class="device-audit">
    <div class="page-card">
      <!-- 筛选条件 -->
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.auditStatus" placeholder="全部" clearable>
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌">
          <el-select v-model="searchForm.brand" placeholder="全部品牌" clearable>
            <el-option label="佳能" value="佳能" />
            <el-option label="尼康" value="尼康" />
            <el-option label="索尼" value="索尼" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadDevices">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 设备列表 -->
      <el-table :data="devices" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="设备图片" width="100">
          <template #default="{ row }">
            <el-image
              :src="getFirstImage(row.imagePaths)"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px"
              :preview-src-list="row.imagePaths?.split(',')"
            />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="设备名称" width="200" />
        <el-table-column label="品牌型号" width="150">
          <template #default="{ row }">
            {{ row.brand }} {{ row.model }}
          </template>
        </el-table-column>
        <el-table-column label="租金/押金" width="120">
          <template #default="{ row }">
            <div>￥{{ row.rentPrice }}/天</div>
            <div>押金￥{{ row.deposit }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="所在地" width="120" />
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.auditStatus)">
              {{ getStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.auditStatus === 'PENDING'"
              type="success"
              size="small"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.auditStatus === 'PENDING'"
              type="danger"
              size="small"
              @click="handleReject(row)"
            >
              拒绝
            </el-button>
            <el-button
              type="info"
              size="small"
              @click="handleViewDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="loadDevices"
        />
      </div>
    </div>

    <!-- 拒绝对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝原因" width="500px">
      <el-form :model="rejectForm">
        <el-form-item label="拒绝原因">
          <el-select v-model="rejectForm.reason" placeholder="请选择拒绝原因">
            <el-option label="设备描述不完整" value="设备描述不完整" />
            <el-option label="图片模糊无法识别" value="图片模糊无法识别" />
            <el-option label="设备信息与实际不符" value="设备信息与实际不符" />
            <el-option label="其他原因" value="其他原因" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" v-if="rejectForm.reason === '其他原因'">
          <el-input v-model="rejectForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { deviceApi, auditDevice } from '@/api/device'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const devices = ref([])
const searchForm = ref({
  auditStatus: 'PENDING',
  brand: ''
})

const pagination = ref({
  page: 1,
  size: 10,
  total: 0
})

const rejectDialogVisible = ref(false)
const rejectForm = ref({
  reason: '',
  remark: ''
})
const currentDevice = ref(null)

onMounted(() => {
  loadDevices()
})

const loadDevices = async () => {
  loading.value = true
  try {
    const res = await deviceApi.adminList({
      auditStatus: searchForm.value.auditStatus || undefined,
      brand: searchForm.value.brand || undefined,
      page: pagination.value.page,
      size: pagination.value.size
    })
    devices.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch (error) {
    ElMessage.error('加载设备列表失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.value = { auditStatus: 'PENDING', brand: '' }
  pagination.value.page = 1
  loadDevices()
}

const getFirstImage = (imagePaths) => {
  if (!imagePaths) return ''
  return imagePaths.split(',')[0] || ''
}

const getStatusText = (status) => {
  const map = { 'PENDING': '待审核', 'APPROVED': '已通过', 'REJECTED': '已拒绝' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { 'PENDING': 'warning', 'APPROVED': 'success', 'REJECTED': 'danger' }
  return map[status] || 'info'
}

const handleApprove = async (device) => {
  try {
    await ElMessageBox.confirm('确认通过该设备的审核？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
    await auditDevice({ deviceId: device.id, auditStatus: 'APPROVED' })
    ElMessage.success('审核通过')
    loadDevices()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleReject = (device) => {
  currentDevice.value = device
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectForm.value.reason) {
    ElMessage.warning('请选择拒绝原因')
    return
  }
  try {
    await auditDevice({
      deviceId: currentDevice.value.id,
      auditStatus: 'REJECTED',
      reason: rejectForm.value.reason,
      remark: rejectForm.value.remark
    })
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    rejectForm.value = { reason: '', remark: '' }
    loadDevices()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleViewDetail = (device) => {
  window.open(`/devices/${device.id}`, '_blank')
}
</script>

<style scoped>
.page-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

