<template>
  <div class="favorite-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的收藏</span>
          <el-tag type="info">{{ favoriteCount }}/50</el-tag>
        </div>
      </template>

      <div v-loading="loading" style="min-height: 400px">
        <el-empty v-if="devices.length === 0" description="暂无收藏" />

        <el-row :gutter="20" v-else>
          <el-col
            :span="6"
            v-for="device in devices"
            :key="device.id"
            style="margin-bottom: 20px"
          >
            <el-card
              :body-style="{ padding: '0px' }"
              shadow="hover"
              class="device-card"
            >
              <img
                :src="getFirstImage(device.imagePaths)"
                class="device-image"
                @click="goToDetail(device.id)"
              />
              <div class="device-info">
                <h4 class="device-title" @click="goToDetail(device.id)">
                  {{ device.title }}
                </h4>
                <p class="device-brand">{{ device.brand }} {{ device.model }}</p>
                <div class="device-price">
                  <span class="price">¥{{ device.rentPrice }}</span>
                  <span class="unit">/天</span>
                </div>
                <div class="device-actions">
                  <el-button
                    type="primary"
                    size="small"
                    @click="handleRent(device.id)"
                    :disabled="device.status !== 'ACTIVE'"
                  >
                    立即租用
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    text
                    @click="handleRemoveFavorite(device.id)"
                  >
                    <el-icon><Delete /></el-icon>
                    取消收藏
                  </el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { favoriteApi } from '@/api/favorite'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const devices = ref([])
const favoriteCount = ref(0)

const getFirstImage = (imagePaths) => {
  if (!imagePaths) return ''
  const paths = Array.isArray(imagePaths) ? imagePaths : imagePaths.split(',')
  return paths[0] || ''
}

const loadFavorites = async () => {
  loading.value = true
  try {
    const [listRes, countRes] = await Promise.all([
      favoriteApi.getList(),
      favoriteApi.getCount()
    ])

    devices.value = listRes.data || []
    favoriteCount.value = countRes.data || 0
  } catch (error) {
    ElMessage.error('加载收藏列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const goToDetail = (deviceId) => {
  router.push(`/devices/${deviceId}`)
}

const handleRent = (deviceId) => {
  router.push(`/devices/${deviceId}/order`)
}

const handleRemoveFavorite = async (deviceId) => {
  try {
    await ElMessageBox.confirm('确认取消收藏该设备？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await favoriteApi.remove(deviceId)
    ElMessage.success('取消收藏成功')
    await loadFavorites()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.device-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.device-card:hover {
  transform: translateY(-5px);
}

.device-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  cursor: pointer;
}

.device-info {
  padding: 15px;
}

.device-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-title:hover {
  color: #409eff;
}

.device-brand {
  margin: 0 0 10px 0;
  color: #999;
  font-size: 14px;
}

.device-price {
  margin-bottom: 15px;
}

.price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
}

.unit {
  color: #999;
  font-size: 14px;
  margin-left: 5px;
}

.device-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

