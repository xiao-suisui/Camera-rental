<template>
  <div class="main-layout">
    <!-- 顶部导航 -->
    <header class="header" :class="{ 'header-scrolled': isScrolled }">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon">
            <el-icon><Camera /></el-icon>
          </div>
          <span class="logo-text">LensGo</span>
        </div>

        <nav class="nav-links">
          <router-link to="/" class="nav-item" exact-active-class="active">首页</router-link>
          <router-link to="/devices" class="nav-item" active-class="active">设备列表</router-link>
          <router-link to="/coupon-center" class="nav-item" active-class="active">优惠券</router-link>
          <router-link to="/device/publish" class="nav-item" active-class="active" v-if="userStore.isLogin">发布设备</router-link>
        </nav>

        <div class="header-actions">
          <template v-if="userStore.isLogin">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
              <el-button class="icon-btn" circle @click="$router.push('/messages')">
                <el-icon :size="18"><Bell /></el-icon>
              </el-button>
            </el-badge>

            <el-dropdown @command="handleCommand" trigger="click">
              <div class="user-avatar">
                <div class="avatar-circle">
                  {{ userStore.userInfo.nickname?.charAt(0) || 'U' }}
                </div>
                <span class="user-name">{{ userStore.userInfo.nickname }}</span>
                <el-icon :size="12"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="orders">
                    <el-icon><Document /></el-icon>我的订单
                  </el-dropdown-item>
                  <el-dropdown-item command="lessor-orders">
                    <el-icon><Document /></el-icon>出租订单
                  </el-dropdown-item>
                  <el-dropdown-item command="my-devices">
                    <el-icon><Camera /></el-icon>我的设备
                  </el-dropdown-item>
                  <el-dropdown-item command="favorites">
                    <el-icon><Star /></el-icon>我的收藏
                  </el-dropdown-item>
                  <el-dropdown-item command="coupons">
                    <el-icon><Ticket /></el-icon>我的优惠券
                  </el-dropdown-item>
                  <el-dropdown-item command="profile" divided>
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="change-password">
                    <el-icon><Lock /></el-icon>修改密码
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="admin"
                    v-if="userStore.userInfo.role === 'ADMIN'"
                    divided
                  >
                    <el-icon><Setting /></el-icon>管理后台
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text class="login-btn" @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">免费注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view />
    </main>

    <!-- 底部 -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-top">
          <div class="footer-brand">
            <div class="logo" style="cursor:default">
              <div class="logo-icon small">
                <el-icon><Camera /></el-icon>
              </div>
              <span class="logo-text" style="color:#fff">LensGo</span>
            </div>
            <p class="footer-desc">专业相机租赁平台，让每个人都能用上好设备</p>
          </div>
          <div class="footer-links">
            <div class="link-group">
              <h4>平台</h4>
              <a @click="$router.push('/devices')">浏览设备</a>
              <a @click="$router.push('/coupon-center')">优惠活动</a>
            </div>
            <div class="link-group">
              <h4>帮助</h4>
              <a href="javascript:void(0)">租赁指南</a>
              <a href="javascript:void(0)">常见问题</a>
            </div>
          </div>
        </div>
        <div class="footer-bottom">
          <span>&copy; 2026 LensGo. All rights reserved.</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import {
  Bell, ArrowDown, Camera, Document, Star, Ticket,
  User, Lock, Setting, SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const isScrolled = ref(false)
const unreadCount = ref(0)

const handleScroll = () => { isScrolled.value = window.scrollY > 10 }
onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  if (userStore.token) { loadUnreadCount(); startPolling() }
})
onUnmounted(() => { window.removeEventListener('scroll', handleScroll); stopPolling() })

const loadUnreadCount = async () => {
  if (!userStore.token || !userStore.userInfo.userId) return
  try {
    const res = await axios.get(`/api/message/unread-count`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` },
      timeout: 5000
    })
    if (res.data?.code === 0) {
      unreadCount.value = res.data.data || 0
    }
  } catch (e) { /* 静默，不影响页面 */ }
}

let timer = null
const startPolling = () => { if (timer) clearInterval(timer); timer = setInterval(loadUnreadCount, 30000) }
const stopPolling = () => { if (timer) { clearInterval(timer); timer = null } }

watch(() => userStore.token, (v) => {
  if (v) { loadUnreadCount(); startPolling() }
  else { unreadCount.value = 0; stopPolling() }
})

const handleCommand = async (cmd) => {
  const map = {
    profile: '/profile', 'edit-profile': '/profile/edit', 'change-password': '/change-password',
    'my-devices': '/my-devices', favorites: '/favorites', orders: '/orders', 'lessor-orders': '/lessor-orders',
    coupons: '/coupons', messages: '/messages', admin: '/admin/dashboard'
  }
  if (cmd === 'logout') { stopPolling(); await userStore.logout(); ElMessage.success('退出成功'); router.push('/'); return }
  if (map[cmd]) router.push(map[cmd])
}
</script>

<style scoped>
.main-layout { min-height: 100vh; display: flex; flex-direction: column; }

/* Header */
.header {
  position: sticky; top: 0; z-index: 999;
  background: rgba(255,255,255,0.88);
  backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  transition: box-shadow 0.3s;
}
.header-scrolled { box-shadow: 0 1px 12px rgba(0,0,0,0.06); }
.header-inner {
  max-width: 1240px; margin: 0 auto;
  display: flex; align-items: center; justify-content: space-between;
  height: 64px; padding: 0 24px;
}

.logo { display: flex; align-items: center; gap: 10px; cursor: pointer; text-decoration: none; }
.logo-icon {
  width: 36px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 18px;
}
.logo-icon.small { width: 30px; height: 30px; font-size: 15px; }
.logo-text { font-size: 20px; font-weight: 700; color: #1a1a2e; letter-spacing: -0.5px; }

.nav-links { display: flex; align-items: center; gap: 4px; }
.nav-item {
  padding: 8px 16px; border-radius: 8px; font-size: 14px; font-weight: 500;
  color: #64748b; text-decoration: none; transition: all 0.2s;
}
.nav-item:hover { color: #1a1a2e; background: #f1f5f9; }
.nav-item.active { color: #4f46e5; background: #eef2ff; }

.header-actions { display: flex; align-items: center; gap: 12px; }
.icon-btn { border: none !important; background: #f1f5f9 !important; color: #475569 !important; }
.icon-btn:hover { background: #e2e8f0 !important; color: #1a1a2e !important; }
.login-btn { font-weight: 500 !important; color: #475569 !important; }

.user-avatar {
  display: flex; align-items: center; gap: 8px; cursor: pointer;
  padding: 4px 8px 4px 4px; border-radius: 24px; transition: background 0.2s;
}
.user-avatar:hover { background: #f1f5f9; }
.avatar-circle {
  width: 32px; height: 32px; border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600;
}
.user-name {
  font-size: 14px; font-weight: 500; color: #334155;
  max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

/* Main */
.main-content { flex: 1; max-width: 1240px; width: 100%; margin: 0 auto; padding: 24px; }

/* Footer */
.footer { background: #1a1a2e; color: #94a3b8; margin-top: 40px; }
.footer-inner { max-width: 1240px; margin: 0 auto; padding: 0 24px; }
.footer-top {
  display: flex; justify-content: space-between;
  padding: 48px 0 32px; border-bottom: 1px solid rgba(255,255,255,0.08);
}
.footer-brand { max-width: 300px; }
.footer-desc { margin-top: 12px; font-size: 14px; line-height: 1.6; color: #64748b; }
.footer-links { display: flex; gap: 64px; }
.link-group h4 { color: #e2e8f0; font-size: 14px; font-weight: 600; margin-bottom: 16px; }
.link-group a {
  display: block; color: #64748b; font-size: 14px; margin-bottom: 10px;
  text-decoration: none; cursor: pointer; transition: color 0.2s;
}
.link-group a:hover { color: #e2e8f0; }
.footer-bottom { padding: 20px 0; font-size: 13px; text-align: center; color: #475569; }
</style>
