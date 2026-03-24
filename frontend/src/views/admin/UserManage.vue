<template>
  <div class="user-manage">
    <div class="page-card">
      <!-- 搜索 -->
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索手机号 / 昵称"
          clearable
          style="width: 280px"
          @keyup.enter="loadUsers"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadUsers">查询</el-button>
      </div>

      <!-- 用户列表 -->
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="address" label="所在地" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" size="small" round>
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small" round>
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ACTIVE' && row.role !== 'ADMIN'"
              type="danger" text size="small"
              @click="handleDisable(row)"
            >禁用</el-button>
            <el-button
              v-if="row.status === 'DISABLED'"
              type="success" text size="small"
              @click="handleEnable(row)"
            >启用</el-button>
            <span v-if="row.role === 'ADMIN'" class="text-muted">—</span>
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
          @current-change="loadUsers"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { adminListUsers, adminDisableUser, adminEnableUser } from '@/api/user'

const loading = ref(false)
const users = ref([])
const keyword = ref('')
const pagination = ref({
  page: 1,
  size: 10,
  total: 0
})

onMounted(() => {
  loadUsers()
})

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await adminListUsers({
      page: pagination.value.page,
      size: pagination.value.size,
      keyword: keyword.value || undefined
    })
    users.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleDisable = async (user) => {
  try {
    await ElMessageBox.confirm(`确认禁用用户「${user.nickname}」？禁用后该用户将无法登录。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await adminDisableUser(user.id)
    ElMessage.success('禁用成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleEnable = async (user) => {
  try {
    await adminEnableUser(user.id)
    ElMessage.success('启用成功')
    loadUsers()
  } catch (error) {
    ElMessage.error('操作失败')
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
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.text-muted { color: #d1d5db; font-size: 13px; }
</style>

