<template>
  <div class="admin-layout">
    <!-- 左侧菜单 -->
    <aside class="admin-aside">
      <div class="admin-logo" @click="router.push('/admin/dashboard')">
        <div class="logo-icon">
          <el-icon><Camera /></el-icon>
        </div>
        <span class="logo-text">LensGo</span>
        <span class="logo-badge">Admin</span>
      </div>

      <nav class="admin-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: activeMenu === item.path }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
        </router-link>
      </nav>

      <div class="aside-footer">
        <div class="admin-user">
          <div class="admin-avatar">{{ userStore.userInfo.nickname?.charAt(0) || 'A' }}</div>
          <div class="admin-user-info">
            <span class="admin-user-name">{{ userStore.userInfo.nickname }}</span>
            <span class="admin-user-role">超级管理员</span>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <div class="admin-body">
      <header class="admin-header">
        <div class="header-left">
          <h1 class="page-title">{{ pageTitle }}</h1>
          <span class="page-breadcrumb">管理后台 / {{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <el-button @click="backToHome" round>
            <el-icon><HomeFilled /></el-icon>
            <span>返回前台</span>
          </el-button>
          <el-button type="danger" text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出</span>
          </el-button>
        </div>
      </header>

      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import {
  Camera, DataAnalysis, User, Ticket, Document,
  HomeFilled, SwitchButton
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const pendingCount = ref(0)

const menuItems = computed(() => [
  { path: '/admin/dashboard', label: '数据看板', icon: DataAnalysis },
  { path: '/admin/devices', label: '设备审核', icon: Camera, badge: pendingCount.value > 0 ? pendingCount.value : null },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/coupons', label: '优惠券管理', icon: Ticket },
  { path: '/admin/orders', label: '订单管理', icon: Document }
])

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const found = menuItems.value.find(i => i.path === route.path)
  return found?.label || '管理后台'
})

const backToHome = () => router.push('/')

const handleLogout = async () => {
  await userStore.logout()
  ElMessage.success('退出成功')
  router.push('/login')
}

onMounted(async () => {
  try {
    const res = await request.get('/device/stats/pending')
    pendingCount.value = res.data || 0
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: #f0f2f5;
}

/* ===== 侧边栏 ===== */
.admin-aside {
  width: 240px;
  min-width: 240px;
  height: 100vh;
  background: #1a1a2e;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 24px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}
.logo-icon {
  width: 34px; height: 34px; border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 17px;
}
.logo-text {
  font-size: 18px; font-weight: 700; color: #fff; letter-spacing: -0.3px;
}
.logo-badge {
  font-size: 10px; font-weight: 600; color: #a78bfa;
  background: rgba(167,139,250,0.12); padding: 2px 8px;
  border-radius: 10px; margin-left: auto; letter-spacing: 0.5px;
}

/* 导航菜单 */
.admin-nav {
  flex: 1; overflow-y: auto; padding: 12px 12px;
  display: flex; flex-direction: column; gap: 2px;
}
.nav-item {
  display: flex; align-items: center; gap: 12px;
  padding: 11px 16px; border-radius: 10px;
  color: #94a3b8; font-size: 14px; font-weight: 500;
  text-decoration: none; transition: all 0.2s;
  position: relative;
}
.nav-item .el-icon { font-size: 18px; }
.nav-item:hover {
  color: #e2e8f0; background: rgba(255,255,255,0.06);
}
.nav-item.active {
  color: #fff; background: linear-gradient(135deg, #4f46e5, #6366f1);
  box-shadow: 0 4px 12px rgba(79,70,229,0.3);
}
.nav-badge {
  margin-left: auto; background: #ef4444; color: #fff;
  font-size: 11px; font-weight: 600; min-width: 20px;
  height: 20px; border-radius: 10px; display: flex;
  align-items: center; justify-content: center; padding: 0 6px;
}

/* 底部用户信息 */
.aside-footer {
  flex-shrink: 0; padding: 16px; border-top: 1px solid rgba(255,255,255,0.06);
}
.admin-user {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 12px; border-radius: 10px; background: rgba(255,255,255,0.04);
}
.admin-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #a78bfa);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; flex-shrink: 0;
}
.admin-user-info { display: flex; flex-direction: column; overflow: hidden; }
.admin-user-name {
  font-size: 13px; font-weight: 600; color: #e2e8f0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.admin-user-role { font-size: 11px; color: #64748b; }

/* ===== 右侧主体 ===== */
.admin-body {
  flex: 1; display: flex; flex-direction: column;
  min-width: 0; height: 100vh; overflow: hidden;
}

.admin-header {
  flex-shrink: 0; height: 64px;
  background: #fff; padding: 0 28px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.header-left { display: flex; flex-direction: column; }
.page-title {
  margin: 0; font-size: 18px; font-weight: 700; color: #111827; line-height: 1.3;
}
.page-breadcrumb {
  font-size: 12px; color: #9ca3af; margin-top: 1px;
}
.header-right { display: flex; align-items: center; gap: 8px; }

.admin-main {
  flex: 1; overflow-y: auto; padding: 24px 28px;
  background: #f0f2f5;
}
</style>

