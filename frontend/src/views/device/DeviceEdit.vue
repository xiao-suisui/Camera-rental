<template>
  <div class="device-edit">
    <el-card>
      <h2>编辑设备</h2>
      <el-alert title="编辑后设备将重新提交审核，审核通过前不会展示给租户" type="info" show-icon :closable="false" style="margin-bottom: 20px" />
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" v-loading="loading">
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
            :max-size="10"
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
            保存修改
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getDeviceDetail, updateDevice } from '@/api/device'
import { ElMessage } from 'element-plus'
import ImageUpload from '@/components/ImageUpload.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const imageUploading = ref(false)

const form = ref({
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
    { type: 'array', required: true, min: 1, message: '请至少添加一张图片', trigger: 'change' }
  ],
  rentPrice: [{ required: true, message: '请设置日租金', trigger: 'change' }],
  deposit: [{ required: true, message: '请设置押金', trigger: 'change' }],
  address: [{ required: true, message: '请输入所在地', trigger: 'blur' }]
}

const handleImageUploadingChange = (status) => {
  imageUploading.value = status
}

const loadDeviceData = async () => {
  loading.value = true
  try {
    const res = await getDeviceDetail(route.params.id)
    if (res.data) {
      form.value = {
        title: res.data.title,
        brand: res.data.brand,
        model: res.data.model,
        description: res.data.description,
        imagePaths: res.data.imagePaths ? res.data.imagePaths.split(',') : [],
        rentPrice: res.data.rentPrice,
        deposit: res.data.deposit,
        address: res.data.address
      }
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('加载设备信息失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (imageUploading.value) {
    ElMessage.warning('图片上传中，请稍后再提交')
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await updateDevice(route.params.id, form.value)
    ElMessage.success('更新成功，已重新提交审核')
    router.back()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '更新失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDeviceData()
})
</script>

<style scoped>
h2 {
  margin-bottom: 30px;
}

</style>

