<template>
  <div class="profile-edit">
    <el-card>
      <template #header>
        <h2>编辑个人资料</h2>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-width: 600px"
      >
        <el-form-item label="头像">
          <ImageUpload
            v-model="avatarList"
            :max-count="1"
            :max-size="5"
          />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input v-model="userStore.userInfo.phone" disabled />
          <span class="tip">手机号不可修改</span>
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称（2-20字符）"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="所在地" prop="address">
          <el-input
            v-model="form.address"
            placeholder="请输入所在地"
            maxlength="100"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            保存修改
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import ImageUpload from '@/components/ImageUpload.vue'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)
const avatarList = ref([])

const form = reactive({
  nickname: '',
  address: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为2-20个字符', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadUserInfo()
})

const loadUserInfo = async () => {
  try {
    const { data } = await request.get(`/user/${userStore.userInfo.userId}`)
    form.nickname = data.nickname || ''
    form.address = data.address || ''
    if (data.avatar) {
      avatarList.value = [data.avatar]
    }
  } catch (error) {
    ElMessage.error('加载用户信息失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    submitting.value = true

    await request.put(`/user/${userStore.userInfo.userId}/profile`, {
      nickname: form.nickname,
      address: form.address,
      avatar: avatarList.value.length > 0 ? avatarList.value[0] : null
    })

    // 更新store中的用户信息（同步到 localStorage）
    userStore.setUserInfo({
      ...userStore.userInfo,
      nickname: form.nickname,
      address: form.address,
      avatar: avatarList.value.length > 0 ? avatarList.value[0] : userStore.userInfo.avatar
    })

    ElMessage.success('保存成功')
    router.push('/profile')
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    submitting.value = false
  }
}

const handleReset = () => {
  loadUserInfo()
}
</script>

<style scoped>
h2 {
  margin: 0;
}

.tip {
  font-size: 12px;
  color: #999;
  margin-left: 10px;
}
</style>

