<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand-area">
        <div class="brand-logo">📸</div>
        <h2>LensGo</h2>
        <p>专业相机租赁，让创作自由</p>
      </div>
    </div>
    <div class="login-right">
      <div class="form-wrapper">
        <h2>欢迎回来</h2>
        <p class="form-subtitle">登录你的账号继续使用</p>
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" prefix-icon="Phone" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password prefix-icon="Lock" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="submit-btn" @click="handleLogin" :loading="loading">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="form-footer">
          还没有账号？<router-link to="/register" class="link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = ref({ phone: '', password: '' })

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const success = await userStore.doLogin(form.value.phone, form.value.password)
    if (success) {
      ElMessage.success('登录成功')
      const redirect = route.query.redirect
      router.push(redirect || '/')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
}
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1a1a2e 0%, #312e81 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-area {
  text-align: center;
  color: #fff;
}
.brand-logo {
  font-size: 64px;
  margin-bottom: 16px;
}
.brand-area h2 {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 8px;
}
.brand-area p {
  font-size: 16px;
  color: #a5b4fc;
}
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}
.form-wrapper {
  width: 380px;
}
.form-wrapper h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
}
.form-subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 32px;
}
.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px !important;
  background: linear-gradient(135deg, #4f46e5, #7c3aed) !important;
  border: none !important;
}
.form-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #64748b;
}
.link {
  color: #4f46e5;
  text-decoration: none;
  font-weight: 500;
}
.link:hover {
  text-decoration: underline;
}
</style>
