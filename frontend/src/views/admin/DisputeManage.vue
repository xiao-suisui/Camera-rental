<template>
  <div class="admin-dispute-list">
    <div class="page-card">
      <el-form :inline="true" :model="searchForm" style="margin-bottom: 16px">
        <el-form-item label="纠纷状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="纠纷类型">
          <el-select v-model="searchForm.disputeType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in DISPUTE_TYPE" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadDisputes">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="disputes" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column label="纠纷类型" width="120">
          <template #default="{ row }">
            <el-tag type="warning" size="small">{{ DISPUTE_TYPE[row.disputeType] || row.disputeType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="initiatorId" label="发起人ID" width="90" />
        <el-table-column prop="respondentId" label="被申请人ID" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="DISPUTE_STATUS[row.status]?.type || 'info'" size="small">
              {{ DISPUTE_STATUS[row.status]?.text || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="200">
          <template #default="{ row }">
            <span class="desc-preview">{{ row.description?.substring(0, 50) }}{{ row.description?.length > 50 ? '...' : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">处理</el-button>
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
          @current-change="loadDisputes"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="纠纷详情" width="600px">
      <template v-if="currentDispute">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="纠纷ID">{{ currentDispute.id }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ currentDispute.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="纠纷类型">
            <el-tag type="warning" size="small">{{ DISPUTE_TYPE[currentDispute.disputeType] || currentDispute.disputeType }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="DISPUTE_STATUS[currentDispute.status]?.type || 'info'" size="small">
              {{ DISPUTE_STATUS[currentDispute.status]?.text || currentDispute.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发起人ID">{{ currentDispute.initiatorId }}</el-descriptions-item>
          <el-descriptions-item label="被申请人ID">{{ currentDispute.respondentId }}</el-descriptions-item>
          <el-descriptions-item label="设备ID">{{ currentDispute.deviceId }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentDispute.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="纠纷描述" :span="2">{{ currentDispute.description }}</el-descriptions-item>
          <el-descriptions-item v-if="currentDispute.adminRemark" label="处理备注" :span="2">{{ currentDispute.adminRemark }}</el-descriptions-item>
        </el-descriptions>

        <el-divider v-if="canUpdate" />

        <el-form v-if="canUpdate" :model="updateForm" label-width="80px" style="margin-top: 16px">
          <el-form-item label="处理结果">
            <el-select v-model="updateForm.status" style="width: 200px">
              <el-option
                v-for="s in nextStatuses"
                :key="s.value"
                :label="s.label"
                :value="s.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="处理备注">
            <el-input
              v-model="updateForm.adminRemark"
              type="textarea"
              :rows="3"
              placeholder="请输入处理意见"
              maxlength="2000"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="canUpdate" type="primary" @click="handleUpdate" :loading="updating">提交处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { disputeApi } from '@/api/dispute'
import { DISPUTE_TYPE, DISPUTE_STATUS } from '@/constants'

const loading = ref(false)
const disputes = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchForm = ref({ status: '', disputeType: '' })

const detailVisible = ref(false)
const currentDispute = ref(null)
const updating = ref(false)
const updateForm = ref({ status: '', adminRemark: '' })

const canUpdate = computed(() => {
  if (!currentDispute.value) return false
  return ['SUBMITTED', 'PROCESSING'].includes(currentDispute.value.status)
})

const nextStatuses = computed(() => {
  if (!currentDispute.value) return []
  const s = currentDispute.value.status
  if (s === 'SUBMITTED') return [{ label: '开始处理', value: 'PROCESSING' }]
  if (s === 'PROCESSING') return [
    { label: '已解决', value: 'RESOLVED' },
    { label: '已驳回', value: 'REJECTED' }
  ]
  return []
})

onMounted(() => { loadDisputes() })

const loadDisputes = async () => {
  loading.value = true
  try {
    const res = await disputeApi.adminList({
      status: searchForm.value.status || undefined,
      disputeType: searchForm.value.disputeType || undefined,
      page: currentPage.value,
      size: pageSize.value
    })
    disputes.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载纠纷列表失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadDisputes()
}

const handleReset = () => {
  searchForm.value = { status: '', disputeType: '' }
  currentPage.value = 1
  loadDisputes()
}

const viewDetail = (row) => {
  currentDispute.value = row
  updateForm.value = {
    status: row.status === 'SUBMITTED' ? 'PROCESSING' : '',
    adminRemark: ''
  }
  detailVisible.value = true
}

const handleUpdate = async () => {
  if (!updateForm.value.status) {
    ElMessage.warning('请选择处理结果')
    return
  }
  updating.value = true
  try {
    await disputeApi.adminUpdate(currentDispute.value.id, updateForm.value)
    ElMessage.success('处理成功')
    detailVisible.value = false
    loadDisputes()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '处理失败')
  } finally {
    updating.value = false
  }
}
</script>

<style scoped>
.page-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.desc-preview {
  font-size: 13px;
  color: #666;
}
</style>
