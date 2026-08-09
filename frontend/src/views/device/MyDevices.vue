<template>
  <div class="my-devices">
    <el-card>
      <div class="header">
        <h2>我的设备</h2>
        <el-button type="primary" @click="$router.push('/device/publish')">
          <el-icon><Plus /></el-icon>
          发布新设备
        </el-button>
      </div>

      <el-table :data="devices" v-loading="loading" stripe>
        <el-table-column prop="id" label="设备ID" width="80" />
        <el-table-column label="设备信息" min-width="200">
          <template #default="{ row }">
            <div class="device-info">
              <img :src="row.imagePaths?.split(',')[0]" class="device-img" />
              <div>
                <div class="device-title">{{ row.title }}</div>
                <div class="device-brand">{{ row.brand }} {{ row.model }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="rentPrice" label="日租金" width="100">
          <template #default="{ row }">
            ¥{{ row.rentPrice }}
          </template>
        </el-table-column>
        <el-table-column prop="deposit" label="押金" width="100">
          <template #default="{ row }">
            ¥{{ row.deposit }}
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getAuditStatusType(row.auditStatus)">
              {{ getAuditStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row.id)">查看</el-button>
            <el-button link type="primary" @click="handleEdit(row.id)" v-if="canEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="handleOffline(row.id)" v-if="row.status === 'ACTIVE'">下架</el-button>
            <el-button link type="success" @click="handleOnline(row.id)" v-if="row.status === 'OFFLINE' && row.auditStatus === 'APPROVED'">上架</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)" v-if="canDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyDevices, deleteDevice, offlineDevice, onlineDevice } from '@/api/device'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { formatDate } from '@/utils/date'

const router = useRouter()
const userStore = useUserStore()
const devices = ref([])
const loading = ref(false)

const loadDevices = async () => {
  loading.value = true
  try {
    const res = await getMyDevices()
    console.log('[loadDevices] response:', res)
    devices.value = res.data || []
  } catch (error) {
    console.error('[loadDevices] error:', error)
    ElMessage.error('加载设备列表失败')
  } finally {
    loading.value = false
  }
}

const handleView = (deviceId) => {
  router.push(`/devices/${deviceId}`)
}

const handleEdit = (deviceId) => {
  router.push(`/device/edit/${deviceId}`)
}

const handleOffline = async (deviceId) => {
  try {
    await ElMessageBox.confirm('确定要下架该设备吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await offlineDevice(deviceId)
    ElMessage.success('下架成功')
    loadDevices()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error('下架失败')
    }
  }
}

const handleOnline = async (deviceId) => {
  try {
    await onlineDevice(deviceId)
    ElMessage.success('上架成功')
    loadDevices()
  } catch (error) {
    console.error(error)
    ElMessage.error('上架失败')
  }
}

const handleDelete = async (deviceId) => {
  try {
    await ElMessageBox.confirm('确定要删除该设备吗？删除后无法恢复。', '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    const res = await deleteDevice(deviceId)
    console.log('[deleteDevice] response:', res)
    ElMessage.success('删除成功')
    await loadDevices()
  } catch (error) {
    console.error('[deleteDevice] error:', error)
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const canEdit = (device) => {
  // 已下架的设备可以编辑（下架后重新提交审核）
  return device.status === 'OFFLINE'
}

const canDelete = (device) => {
  // 未通过审核或已下架的设备可以删除
  return device.status === 'OFFLINE'
}

const getAuditStatusText = (status) => {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

const getAuditStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    ACTIVE: '已上架',
    OFFLINE: '已下架'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = {
    ACTIVE: 'success',
    OFFLINE: 'info'
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadDevices()
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

h2 {
  margin: 0;
}

.device-info {
  display: flex;
  gap: 10px;
  align-items: center;
}

.device-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.device-title {
  font-weight: bold;
  margin-bottom: 5px;
}

.device-brand {
  font-size: 12px;
  color: #666;
}
</style>

