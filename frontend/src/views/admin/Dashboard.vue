<template>
  <div class="admin-dashboard">
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card" :class="card.theme">
        <div class="stat-card-inner">
          <div class="stat-card-top">
            <span class="stat-label">{{ card.label }}</span>
            <div class="stat-icon-wrap">
              <el-icon :size="20"><component :is="card.icon" /></el-icon>
            </div>
          </div>
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-footer" v-if="card.footer">
            {{ card.footer }}
          </div>
        </div>
      </div>
    </div>

    <!-- 中间行：图表 + 快捷操作 -->
    <div class="content-grid">
      <div class="chart-card">
        <div class="card-header">
          <h3>订单趋势</h3>
          <span class="card-desc">近7天订单量变化</span>
        </div>
        <div class="chart-body">
          <div class="chart-empty">
            <el-icon :size="48" color="#d1d5db"><DataAnalysis /></el-icon>
            <p>ECharts 图表预留位</p>
            <p class="chart-hint">安装 echarts 后接入真实趋势数据</p>
          </div>
        </div>
      </div>

      <div class="quick-card">
        <div class="card-header">
          <h3>快捷操作</h3>
        </div>
        <div class="quick-actions">
          <div class="quick-btn" @click="router.push('/admin/devices')">
            <div class="quick-icon purple"><el-icon><Camera /></el-icon></div>
            <span>设备审核</span>
          </div>
          <div class="quick-btn" @click="router.push('/admin/users')">
            <div class="quick-icon blue"><el-icon><User /></el-icon></div>
            <span>用户管理</span>
          </div>
          <div class="quick-btn" @click="router.push('/admin/coupons')">
            <div class="quick-icon orange"><el-icon><Ticket /></el-icon></div>
            <span>优惠券</span>
          </div>
          <div class="quick-btn" @click="router.push('/admin/orders')">
            <div class="quick-icon cyan"><el-icon><Document /></el-icon></div>
            <span>订单管理</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 待处理事项 -->
    <div class="task-card">
      <div class="card-header">
        <h3>待处理事项</h3>
        <el-tag v-if="pendingTasks.length" type="danger" size="small" round>{{ pendingTasks.length }} 条</el-tag>
        <el-tag v-else type="success" size="small" round>暂无</el-tag>
      </div>
      <div v-if="pendingTasks.length" class="task-list">
        <div v-for="(task, idx) in pendingTasks" :key="idx" class="task-item" @click="handleTask(task)">
          <div class="task-dot" :class="task.type === '设备审核' ? 'warn' : 'danger'"></div>
          <div class="task-body">
            <span class="task-content">{{ task.content }}</span>
            <span class="task-time">{{ task.time }}</span>
          </div>
          <el-button type="primary" size="small" round>处理</el-button>
        </div>
      </div>
      <div v-else class="task-empty">
        <el-icon :size="32" color="#d1d5db"><CircleCheck /></el-icon>
        <p>所有事项已处理完毕</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Camera, Document, Ticket, DataAnalysis, CircleCheck, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()

const stats = ref({
  totalUsers: 0,
  totalDevices: 0,
  totalOrders: 0,
  pendingAudit: 0
})

const pendingTasks = ref([])

const statCards = computed(() => [
  { label: '总用户数', value: stats.value.totalUsers, icon: User, theme: 'purple', footer: '平台注册用户' },
  { label: '设备总数', value: stats.value.totalDevices, icon: Camera, theme: 'pink', footer: '含上架与下架' },
  { label: '订单总数', value: stats.value.totalOrders, icon: Document, theme: 'blue', footer: '全部历史订单' },
  { label: '待审核设备', value: stats.value.pendingAudit, icon: Warning, theme: 'orange', footer: stats.value.pendingAudit > 0 ? '需要尽快处理' : '暂无待审核' }
])

onMounted(async () => {
  await loadDashboardData()
})

const loadDashboardData = async () => {
  try {
    const [usersRes, devicesRes, ordersRes, pendingRes] = await Promise.allSettled([
      request.get('/user/stats/count'),
      request.get('/device/stats/count'),
      request.get('/order/stats/count'),
      request.get('/device/stats/pending')
    ])

    stats.value = {
      totalUsers: usersRes.status === 'fulfilled' ? (usersRes.value.data || 0) : 0,
      totalDevices: devicesRes.status === 'fulfilled' ? (devicesRes.value.data || 0) : 0,
      totalOrders: ordersRes.status === 'fulfilled' ? (ordersRes.value.data || 0) : 0,
      pendingAudit: pendingRes.status === 'fulfilled' ? (pendingRes.value.data || 0) : 0
    }

    const tasks = []
    if (stats.value.pendingAudit > 0) {
      tasks.push({
        type: '设备审核',
        content: `有 ${stats.value.pendingAudit} 个设备待审核`,
        time: new Date().toISOString().slice(0, 16).replace('T', ' ')
      })
    }
    pendingTasks.value = tasks
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const handleTask = (task) => {
  if (task.type === '设备审核') {
    router.push('/admin/devices')
  } else {
    ElMessage.info('功能开发中')
  }
}
</script>

<style scoped>
.admin-dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ===== 统计卡片网格 ===== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat-card {
  border-radius: 14px;
  padding: 22px 24px;
  background: #fff;
  position: relative;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.08);
}
.stat-card::before {
  content: '';
  position: absolute; top: 0; left: 0;
  width: 100%; height: 3px;
}
.stat-card.purple::before { background: linear-gradient(90deg, #6366f1, #8b5cf6); }
.stat-card.pink::before { background: linear-gradient(90deg, #ec4899, #f43f5e); }
.stat-card.blue::before { background: linear-gradient(90deg, #0ea5e9, #06b6d4); }
.stat-card.orange::before { background: linear-gradient(90deg, #f59e0b, #ef4444); }

.stat-card-top {
  display: flex; justify-content: space-between; align-items: center;
}
.stat-label { font-size: 13px; color: #6b7280; font-weight: 500; }
.stat-icon-wrap {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
}
.stat-card.purple .stat-icon-wrap { background: #eef2ff; color: #6366f1; }
.stat-card.pink .stat-icon-wrap { background: #fdf2f8; color: #ec4899; }
.stat-card.blue .stat-icon-wrap { background: #ecfeff; color: #0ea5e9; }
.stat-card.orange .stat-icon-wrap { background: #fffbeb; color: #f59e0b; }

.stat-value {
  font-size: 32px; font-weight: 800; color: #111827;
  margin: 8px 0 4px; line-height: 1.1; letter-spacing: -1px;
}
.stat-footer { font-size: 12px; color: #9ca3af; }

/* ===== 中间行 ===== */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
}
.chart-card, .quick-card, .task-card {
  background: #fff; border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.card-header {
  display: flex; align-items: center; gap: 10px;
  padding: 18px 22px; border-bottom: 1px solid #f3f4f6;
}
.card-header h3 {
  margin: 0; font-size: 15px; font-weight: 700; color: #111827;
}
.card-desc { font-size: 12px; color: #9ca3af; }

.chart-body { padding: 24px; }
.chart-empty {
  height: 240px; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 8px;
  color: #9ca3af; font-size: 14px;
}
.chart-hint { font-size: 12px; color: #d1d5db; }

/* 快捷操作 */
.quick-actions {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 12px; padding: 20px;
}
.quick-btn {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 20px 12px; border-radius: 12px; cursor: pointer;
  background: #f9fafb; transition: all 0.2s; border: 1px solid transparent;
}
.quick-btn:hover { background: #f3f4f6; border-color: #e5e7eb; }
.quick-btn span { font-size: 13px; color: #374151; font-weight: 500; }
.quick-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; color: #fff;
}
.quick-icon.purple { background: linear-gradient(135deg, #6366f1, #8b5cf6); }
.quick-icon.blue { background: linear-gradient(135deg, #0ea5e9, #06b6d4); }
.quick-icon.orange { background: linear-gradient(135deg, #f59e0b, #f97316); }
.quick-icon.cyan { background: linear-gradient(135deg, #14b8a6, #06b6d4); }

/* ===== 待处理事项 ===== */
.task-list { padding: 8px 12px; }
.task-item {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 12px; border-radius: 10px;
  cursor: pointer; transition: background 0.15s;
}
.task-item:hover { background: #f9fafb; }
.task-dot {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
}
.task-dot.warn { background: #f59e0b; }
.task-dot.danger { background: #ef4444; }
.task-body { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.task-content { font-size: 14px; color: #374151; font-weight: 500; }
.task-time { font-size: 12px; color: #9ca3af; }
.task-empty {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; padding: 40px 0; gap: 8px;
  color: #9ca3af; font-size: 14px;
}
</style>

