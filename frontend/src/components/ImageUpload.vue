<template>
  <div
    class="image-upload"
    :class="{ 'is-dragging': dragActive }"
    @dragover.prevent="handleDragOver"
    @dragleave.prevent="handleDragLeave"
    @drop.prevent="handleDrop"
  >
    <div class="image-list">
      <div v-for="(image, index) in imageList" :key="index" class="image-item">
        <el-image :src="image" fit="cover" class="uploaded-image" :preview-src-list="imageList" />
        <div class="image-actions">
          <el-icon class="action-icon" @click="handlePreview">
            <ZoomIn />
          </el-icon>
          <el-icon class="action-icon" @click="handleRemove(index)">
            <Delete />
          </el-icon>
        </div>
        <div v-if="uploading && index === imageList.length - 1" class="upload-progress">
          <el-progress :percentage="uploadProgress" />
        </div>
      </div>

      <div
        v-if="imageList.length < maxCount"
        class="upload-btn"
        @click="handleUploadClick"
      >
        <el-icon><Plus /></el-icon>
        <span>上传图片</span>
      </div>
    </div>

    <div class="upload-tips">
      <p>支持jpg、png格式，单张不超过{{ maxSize }}MB，最多{{ maxCount }}张</p>
      <p v-if="failedFiles.length > 0" class="retry-line">
        {{ failedFiles.length }} 张图片上传失败
        <el-button link type="primary" @click="retryFailedUploads">重试</el-button>
      </p>
    </div>

    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/jpg,image/png"
      :multiple="maxCount > 1"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { uploadApi } from '@/api/upload'
import { ElMessage } from 'element-plus'
import { Plus, Delete, ZoomIn } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  maxCount: {
    type: Number,
    default: 5
  },
  maxSize: {
    type: Number,
    default: 2 // MB
  }
})

const emit = defineEmits(['update:modelValue', 'uploading-change'])

const fileInput = ref(null)
const imageList = ref([...props.modelValue])
const uploading = ref(false)
const uploadProgress = ref(0)
const dragActive = ref(false)
const failedFiles = ref([])

watch(() => props.modelValue, (newVal) => {
  imageList.value = [...newVal]
})

watch(imageList, (newVal) => {
  emit('update:modelValue', newVal)
}, { deep: true })

watch(uploading, (newVal) => {
  emit('uploading-change', newVal)
})

const handleUploadClick = () => {
  if (uploading.value) return
  fileInput.value.click()
}

const handleFileChange = async (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  await processFiles(files)
  event.target.value = '' // 清空input
}

const processFiles = async (files) => {
  if (uploading.value) {
    ElMessage.info('图片上传中，请稍候')
    return
  }

  // 检查数量限制
  if (imageList.value.length + files.length > props.maxCount) {
    ElMessage.warning(`最多只能上传${props.maxCount}张图片`)
    return
  }

  // 检查文件大小
  const oversizedFiles = files.filter(file => file.size > props.maxSize * 1024 * 1024)
  if (oversizedFiles.length > 0) {
    ElMessage.warning(`图片大小不能超过${props.maxSize}MB`)
    return
  }

  // 检查文件类型
  const invalidFiles = files.filter(file => !file.type.startsWith('image/'))
  if (invalidFiles.length > 0) {
    ElMessage.warning('只能上传图片文件')
    return
  }

  uploading.value = true
  uploadProgress.value = 0
  failedFiles.value = []

  try {
    let successCount = 0
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      try {
        const { data } = await uploadApi.uploadImage(file)
        imageList.value.push(data)
        successCount++
      } catch (error) {
        failedFiles.value.push(file)
      }
      uploadProgress.value = Math.round(((i + 1) / files.length) * 100)
    }

    if (successCount > 0) {
      ElMessage.success(`成功上传 ${successCount} 张图片`)
    }
    if (failedFiles.value.length > 0) {
      ElMessage.warning(`${failedFiles.value.length} 张图片上传失败，可点击重试`)
    }
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

const retryFailedUploads = async () => {
  if (failedFiles.value.length === 0) return
  const retryFiles = [...failedFiles.value]
  await processFiles(retryFiles)
}

const handleDragOver = () => {
  if (imageList.value.length >= props.maxCount) return
  dragActive.value = true
}

const handleDragLeave = () => {
  dragActive.value = false
}

const handleDrop = async (event) => {
  dragActive.value = false
  const files = Array.from(event.dataTransfer?.files || [])
  if (files.length === 0) return
  await processFiles(files)
}

const handlePreview = () => {
  // Element Plus的图片预览会自动处理
}

const handleRemove = (index) => {
  imageList.value.splice(index, 1)
  ElMessage.success('删除成功')
}
</script>

<style scoped>
.image-upload {
  width: 100%;
}

.image-upload.is-dragging .upload-btn {
  border-color: #409eff;
  background: #ecf5ff;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.uploaded-image {
  width: 100%;
  height: 100%;
}

.image-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-item:hover .image-actions {
  opacity: 1;
}

.action-icon {
  font-size: 20px;
  color: white;
  cursor: pointer;
  transition: transform 0.3s;
}

.action-icon:hover {
  transform: scale(1.2);
}

.upload-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 5px;
  background: rgba(255, 255, 255, 0.9);
}

.upload-btn {
  width: 120px;
  height: 120px;
  border: 1px dashed #ddd;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.3s;
}

.upload-btn:hover {
  border-color: #409eff;
}

.upload-btn :deep(.el-icon) {
  font-size: 28px;
  color: #999;
  margin-bottom: 8px;
}

.upload-btn span {
  font-size: 14px;
  color: #999;
}

.upload-tips {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.upload-tips p {
  margin: 0;
}

.retry-line {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>

