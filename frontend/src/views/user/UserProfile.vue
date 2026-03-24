<template>
  <div class="user-profile">
    <el-card>
      <h2>个人中心</h2>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户ID">
          {{ userStore.userInfo.userId }}
        </el-descriptions-item>
        <el-descriptions-item label="昵称">
          {{ userStore.userInfo.nickname }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag>{{ userStore.userInfo.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
h2 {
  margin-bottom: 20px;
}
</style>

