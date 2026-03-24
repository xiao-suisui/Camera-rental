<template>
  <div class="change-password">
    <el-card>
      <template #header>
        <h2>修改密码</h2>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-width: 600px"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码（至少6位）"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            确认修改
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        title="密码修改说明"
        type="info"
        :closable="false"
        style="margin-top: 20px"
      >
        <ul style="margin: 0; padding-left: 20px">
          <li>密码长度至少6位</li>
          <li>新密码不能与旧密码相同</li>
          <li>修改成功后需要重新登录</li>
        </ul>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 自定义验证规则
const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    if (form.oldPassword === form.newPassword) {
      ElMessage.warning('新密码不能与旧密码相同')
      return
    }

    submitting.value = true

    await request.post(`/user/${userStore.userInfo.userId}/change-password`, {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })

    ElMessage.success('密码修改成功，请重新登录')

    // 等待2秒后退出登录
    setTimeout(async () => {
      await userStore.logout()
      router.push('/login')
    }, 2000)
  } catch (error) {
    ElMessage.error(error.message || '修改失败')
  } finally {
    submitting.value = false
  }
}

const handleReset = () => {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}
</script>

<style scoped>
h2 {
  margin: 0;
}
</style>

