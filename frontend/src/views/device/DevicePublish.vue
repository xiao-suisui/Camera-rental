<template>
  <div class="device-publish">
    <el-card>
      <h2>发布设备</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="发布方式">
          <el-radio-group v-model="publishType">
            <el-radio label="single">单个发布</el-radio>
            <el-radio label="batch">批量发布</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="发布数量" v-if="publishType === 'batch'">
          <el-input-number v-model="batchQuantity" :min="1" :max="10" />
          <span class="tip">批量发布最多10台</span>
        </el-form-item>

        <el-form-item label="设备标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入设备标题" />
        </el-form-item>

        <el-form-item label="品牌" prop="brand">
          <el-select v-model="form.brand" placeholder="请选择品牌">
            <el-option label="佳能" value="佳能" />
            <el-option label="尼康" value="尼康" />
            <el-option label="索尼" value="索尼" />
            <el-option label="富士" value="富士" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="型号" prop="model">
          <el-input v-model="form.model" placeholder="请输入型号" />
        </el-form-item>

        <el-form-item label="设备描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="请详细描述设备信息（至少50字）"
          />
        </el-form-item>

        <el-form-item label="设备图片" prop="imagePaths">
          <ImageUpload
            v-model="form.imagePaths"
            :max-count="5"
            :max-size="5"
            @uploading-change="handleImageUploadingChange"
          />
        </el-form-item>

        <el-form-item label="日租金" prop="rentPrice">
          <el-input-number v-model="form.rentPrice" :precision="2" :min="0.01" :step="10" />
          <span style="margin-left: 10px">元/天</span>
        </el-form-item>

        <el-form-item label="押金" prop="deposit">
          <el-input-number v-model="form.deposit" :precision="2" :min="0" :step="100" />
          <span style="margin-left: 10px">元</span>
        </el-form-item>

        <el-form-item label="所在地" prop="address">
          <el-input v-model="form.address" placeholder="请输入所在地" />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="handleSubmit"
            :loading="submitting"
            :disabled="imageUploading"
          >
            提交审核
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { publishDevice, batchPublishDevice } from '@/api/device'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import ImageUpload from '@/components/ImageUpload.vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const submitting = ref(false)
const imageUploading = ref(false)
const publishType = ref('single')
const batchQuantity = ref(1)

const form = ref({
  userId: userStore.userInfo.userId,
  title: '',
  brand: '',
  model: '',
  description: '',
  imagePaths: [],
  rentPrice: 100,
  deposit: 1000,
  address: ''
})

const rules = {
  title: [{ required: true, message: '请输入设备标题', trigger: 'blur' }],
  brand: [{ required: true, message: '请选择品牌', trigger: 'change' }],
  model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
  description: [
    { required: true, message: '请输入设备描述', trigger: 'blur' },
    { min: 50, message: '描述至少50字', trigger: 'blur' }
  ],
  imagePaths: [
    { required: true, message: '请至少添加一张图片', trigger: 'change' }
  ],
  rentPrice: [{ required: true, message: '请设置日租金', trigger: 'change' }],
  deposit: [{ required: true, message: '请设置押金', trigger: 'change' }],
  address: [{ required: true, message: '请输入所在地', trigger: 'blur' }]
}

const handleImageUploadingChange = (status) => {
  imageUploading.value = status
}

const handleSubmit = async () => {
  if (imageUploading.value) {
    ElMessage.warning('图片上传中，请稍后再提交')
    return
  }
  await formRef.value.validate()
  submitting.value = true
  try {
    if (publishType.value === 'batch') {
      await batchPublishDevice({
        ...form.value,
        quantity: batchQuantity.value
      })
      ElMessage.success(`成功提交${batchQuantity.value}台设备，请等待审核`)
    } else {
      await publishDevice(form.value)
      ElMessage.success('提交成功，请等待审核')
    }
    router.push('/devices')
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
h2 {
  margin-bottom: 30px;
}


.tip {
  margin-left: 10px;
  color: #999;
  font-size: 14px;
}
</style>

