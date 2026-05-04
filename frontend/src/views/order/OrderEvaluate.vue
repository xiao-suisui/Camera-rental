<template>
  <div class="evaluation-create">
    <el-card>
      <template #header>评价订单</template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="评分" prop="score">
          <el-rate v-model="form.score" :max="5" :min="1" show-text />
        </el-form-item>

        <el-form-item label="评价内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请描述您的使用体验（至少10个字）"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="上传图片">
          <ImageUpload
            v-model="imageList"
            :max-count="3"
            :max-size="2"
            @uploading-change="handleImageUploadingChange"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="handleSubmit"
            :loading="submitting"
            :disabled="imageUploading"
          >
            提交评价
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { evaluationApi } from '@/api/evaluation'
import { orderApi } from '@/api/order'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import ImageUpload from '@/components/ImageUpload.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)
const order = ref({})
const imageList = ref([])
const imageUploading = ref(false)

const form = reactive({
  score: 5,
  content: '',
  imagePaths: ''
})

const rules = {
  score: [
    { required: true, message: '请选择评分', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入评价内容', trigger: 'blur' },
    { min: 10, message: '评价内容至少10个字', trigger: 'blur' }
  ]
}

const handleImageUploadingChange = (status) => {
  imageUploading.value = status
}

onMounted(async () => {
  await loadOrderDetail()
  await checkCanEvaluate()
})

const loadOrderDetail = async () => {
  try {
    const { data } = await orderApi.getDetail(route.params.orderId)
    order.value = data
  } catch (error) {
    ElMessage.error('加载订单详情失败')
    router.back()
  }
}

const checkCanEvaluate = async () => {
  try {
    const { data } = await evaluationApi.canEvaluate(route.params.orderId)
    if (!data) {
      ElMessage.warning('该订单无法评价')
      router.back()
    }
  } catch (error) {
    console.error('检查评价权限失败', error)
  }
}

const handleSubmit = async () => {
  if (imageUploading.value) {
    ElMessage.warning('图片上传中，请稍后再提交')
    return
  }
  try {
    await formRef.value.validate()

    submitting.value = true

    const request = {
      orderId: order.value.id,
      evaluatorId: userStore.userInfo.userId,
      evaluatedId: order.value.lessorId, // 租用者评价出租者
      deviceId: order.value.deviceId,
      score: form.score,
      content: form.content,
      imagePaths: imageList.value.join(',')
    }

    await evaluationApi.create(request)
    ElMessage.success('评价成功')
    router.push('/orders')
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
</style>

