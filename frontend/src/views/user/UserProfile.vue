<template>
  <div class="user-profile">
    <el-card class="profile-summary">
      <div class="summary-content">
        <el-avatar :size="80" :src="userInfo.avatar">
          <span style="font-size: 32px">{{ (userInfo.nickname || '?').charAt(0) }}</span>
        </el-avatar>
        <div class="summary-info">
          <h2>{{ userInfo.nickname }}</h2>
          <p>{{ userInfo.phone || '未绑定手机' }}</p>
        </div>
        <el-button type="primary" @click="$router.push('/profile/edit')">编辑资料</el-button>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 24px">
      <el-col :xs="12" :sm="8" :md="6" v-for="item in quickEntries" :key="item.route" style="margin-bottom: 20px">
        <el-card class="entry-card" shadow="hover" @click="$router.push(item.route)">
          <el-icon :size="32" color="#4f46e5"><component :is="item.icon" /></el-icon>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, markRaw } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo } from '@/api/user'
import { ElMessage } from 'element-plus'
import {
  Document, Camera, Ticket, Star, Bell, ChatDotRound, Lock
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const userInfo = ref({})

const quickEntries = [
  { icon: markRaw(Document), title: '我的订单', desc: '查看租赁订单', route: '/orders' },
  { icon: markRaw(Document), title: '出租订单', desc: '管理出租订单', route: '/lessor-orders' },
  { icon: markRaw(Camera), title: '我的设备', desc: '管理发布设备', route: '/my-devices' },
  { icon: markRaw(Ticket), title: '我的优惠券', desc: '查看优惠券', route: '/coupons' },
  { icon: markRaw(Star), title: '我的收藏', desc: '收藏的设备', route: '/favorites' },
  { icon: markRaw(Bell), title: '我的消息', desc: '系统通知', route: '/messages' },
  { icon: markRaw(ChatDotRound), title: '我的聊天', desc: '会话列表', route: '/chat' },
  { icon: markRaw(Lock), title: '修改密码', desc: '账户安全', route: '/change-password' },
]

onMounted(async () => {
  try {
    const res = await getUserInfo(userStore.userInfo.userId)
    if (res.code === 0) {
      userInfo.value = res.data
      userStore.setUserInfo({
        ...userStore.userInfo,
        avatar: res.data.avatar,
        address: res.data.address,
        phone: res.data.phone
      })
    }
  } catch {
    ElMessage.error('加载用户信息失败')
  }
})
</script>

<style scoped>
.summary-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.summary-info {
  flex: 1;
}

.summary-info h2 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #1a1a2e;
}

.summary-info p {
  margin: 0;
  font-size: 14px;
  color: #94a3b8;
}

.entry-card {
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border-radius: 12px;
}

.entry-card:hover {
  transform: translateY(-4px);
}

.entry-card h3 {
  margin: 12px 0 4px;
  font-size: 15px;
  color: #1a1a2e;
}

.entry-card p {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
}
</style>
