<template>
  <div class="device-list">
    <!-- 搜索筛选 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.keyword"
        placeholder="搜索设备名称、品牌..."
        clearable
        size="large"
        class="search-input"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div class="filter-row">
        <div class="filter-tags">
          <el-check-tag
            v-for="brand in brands"
            :key="brand"
            :checked="searchForm.brand === brand"
            @change="toggleBrand(brand)"
          >
            {{ brand }}
          </el-check-tag>
        </div>
        <div class="filter-extra">
          <el-input-number
            v-model="searchForm.minPrice"
            placeholder="最低价"
            :min="0"
            :controls="false"
            size="small"
            style="width: 100px"
            @change="handleSearch"
          />
          <span class="price-sep">—</span>
          <el-input-number
            v-model="searchForm.maxPrice"
            placeholder="最高价"
            :min="0"
            :controls="false"
            size="small"
            style="width: 100px"
            @change="handleSearch"
          />
          <el-select
            v-model="searchForm.sortBy"
            placeholder="排序"
            clearable
            size="small"
            style="width: 120px; margin-left: 12px"
            @change="handleSearch"
          >
            <el-option label="最新发布" value="time_desc" />
            <el-option label="价格从低到高" value="price_asc" />
            <el-option label="价格从高到低" value="price_desc" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 设备列表 -->
    <div class="device-grid" v-loading="loading">
      <div
        class="device-card"
        v-for="device in devices"
        :key="device.id"
        @click="$router.push(`/devices/${device.id}`)"
      >
        <div class="card-img">
          <img :src="device.imagePaths?.split(',')[0] || '/placeholder.jpg'" alt="" />
          <div class="card-tag">¥{{ device.rentPrice }}/天</div>
        </div>
        <div class="card-body">
          <h4>{{ device.title }}</h4>
          <p class="card-brand">{{ device.brand }} {{ device.model }}</p>
          <div class="card-footer">
            <span class="card-deposit">押金 ¥{{ device.deposit }}</span>
            <span class="card-location" v-if="device.address">📍 {{ device.address }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && devices.length === 0" description="没有找到相关设备" />

    <!-- 分页 -->
    <div class="pagination" v-if="pagination.total > 0">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[12, 24, 36]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadDevices"
        @size-change="handleSearch"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { searchDevices } from '@/api/device'
import { Search } from '@element-plus/icons-vue'

const loading = ref(false)
const devices = ref([])
const brands = ['全部', '佳能', '尼康', '索尼', '富士', '松下']
const searchForm = ref({ keyword: '', brand: '', sortBy: '', minPrice: undefined, maxPrice: undefined })
const pagination = ref({ page: 1, size: 12, total: 0 })

const toggleBrand = (brand) => {
  searchForm.value.brand = (brand === '全部' || searchForm.value.brand === brand) ? '' : brand
  handleSearch()
}

const loadDevices = async () => {
  loading.value = true
  try {
    const res = await searchDevices({
      ...searchForm.value,
      page: pagination.value.page,
      size: pagination.value.size
    })
    devices.value = res.data.records || []
    pagination.value.total = res.data.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.value.page = 1; loadDevices() }
onMounted(() => { loadDevices() })
</script>

<style scoped>
.search-bar {
  margin-bottom: 24px;
}
.search-input {
  margin-bottom: 16px;
}
.search-input :deep(.el-input__wrapper) {
  border-radius: 12px !important;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06) !important;
  padding: 4px 16px !important;
}
.filter-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.filter-tags .el-check-tag {
  border-radius: 8px !important;
  font-size: 13px;
}
.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.filter-extra {
  display: flex;
  align-items: center;
  gap: 6px;
}
.price-sep {
  color: #cbd5e1;
  font-size: 12px;
}

.device-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  min-height: 200px;
}
.device-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s;
}
.device-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
}
.card-img {
  position: relative;
  height: 200px;
  overflow: hidden;
}
.card-img img {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.4s;
}
.device-card:hover .card-img img { transform: scale(1.05); }
.card-tag {
  position: absolute; bottom: 10px; left: 10px;
  background: rgba(0,0,0,0.65); color: #fff;
  padding: 4px 10px; border-radius: 6px;
  font-size: 13px; font-weight: 600;
  backdrop-filter: blur(4px);
}
.card-body { padding: 16px; }
.card-body h4 {
  font-size: 15px; font-weight: 600; color: #1a1a2e;
  margin-bottom: 6px; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap;
}
.card-brand { font-size: 13px; color: #94a3b8; margin-bottom: 12px; }
.card-footer {
  display: flex; justify-content: space-between;
  align-items: center; font-size: 12px; color: #94a3b8;
}
.card-deposit {
  background: #f8f9fc; padding: 3px 8px; border-radius: 4px;
}

.pagination {
  display: flex; justify-content: center;
  margin-top: 40px; padding-bottom: 20px;
}
</style>
