<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <span class="hero-badge">📸 专业相机租赁平台</span>
        <h1>好设备，<br/>不必拥有，只需体验</h1>
        <p class="hero-subtitle">从入门微单到旗舰单反，数百款设备随租随用，让创作不再受限</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" class="hero-btn" @click="$router.push('/devices')">
            浏览设备
          </el-button>
          <el-button size="large" class="hero-btn-outline" @click="$router.push('/device/publish')">
            我要出租
          </el-button>
        </div>
        <div class="hero-stats">
          <div class="stat"><strong>500+</strong><span>在租设备</span></div>
          <div class="stat-divider"></div>
          <div class="stat"><strong>2000+</strong><span>注册用户</span></div>
          <div class="stat-divider"></div>
          <div class="stat"><strong>98%</strong><span>好评率</span></div>
        </div>
      </div>
    </section>

    <!-- 优势 -->
    <section class="features">
      <div class="section-header">
        <h2>为什么选择 LensGo</h2>
        <p>我们致力于打造最便捷、安全的相机租赁体验</p>
      </div>
      <div class="feature-grid">
        <div class="feature-card" v-for="f in features" :key="f.title">
          <div class="feature-icon" :style="{ background: f.bg }">
            {{ f.emoji }}
          </div>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 热门设备 -->
    <section class="hot-section">
      <div class="section-header">
        <h2>热门设备</h2>
        <el-button text type="primary" @click="$router.push('/devices')">
          查看全部 →
        </el-button>
      </div>
      <div class="device-grid" v-loading="loading">
        <div
          class="device-card"
          v-for="device in hotDevices"
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
              <span class="card-location" v-if="device.address">
                📍 {{ device.address }}
              </span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="!loading && hotDevices.length === 0" style="text-align:center;padding:60px 0;color:#94a3b8">
        暂无设备数据
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { searchDevices } from '@/api/device'

const loading = ref(false)
const hotDevices = ref([])

const features = [
  { emoji: '🎯', title: '海量设备', desc: '佳能、索尼、尼康、富士等主流品牌一应俱全', bg: 'linear-gradient(135deg,#eef2ff,#e0e7ff)' },
  { emoji: '✅', title: '品质保障', desc: '所有设备经过严格审核，确保功能完好', bg: 'linear-gradient(135deg,#ecfdf5,#d1fae5)' },
  { emoji: '🔒', title: '安全交易', desc: '完善的押金机制和订单保护，租得安心', bg: 'linear-gradient(135deg,#fef3c7,#fde68a)' },
  { emoji: '⚡', title: '快捷租赁', desc: '在线下单、即时确认，最快当天可取', bg: 'linear-gradient(135deg,#fce7f3,#fbcfe8)' }
]

const loadHotDevices = async () => {
  loading.value = true
  try {
    const res = await searchDevices({ page: 1, size: 8 })
    hotDevices.value = res.data.records || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadHotDevices() })
</script>

<style scoped>
/* Hero */
.hero {
  position: relative;
  padding: 80px 0 60px;
  overflow: hidden;
}
.hero-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #eef2ff 0%, #faf5ff 50%, #fce7f3 100%);
  z-index: 0;
}
.hero-content {
  position: relative; z-index: 1;
  max-width: 680px;
  padding-left: 60px;
}
.hero-badge {
  display: inline-block;
  padding: 6px 14px;
  background: rgba(79,70,229,0.08);
  color: #4f46e5;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 20px;
}
.hero h1 {
  font-size: 48px;
  font-weight: 700;
  line-height: 1.2;
  color: #1a1a2e;
  letter-spacing: -1px;
  margin-bottom: 16px;
}
.hero-subtitle {
  font-size: 17px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 32px;
}
.hero-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 48px;
}
.hero-btn {
  padding: 12px 32px !important;
  font-size: 15px !important;
  background: linear-gradient(135deg, #4f46e5, #7c3aed) !important;
  border: none !important;
}
.hero-btn:hover {
  opacity: 0.9;
}
.hero-btn-outline {
  padding: 12px 32px !important;
  font-size: 15px !important;
  border-color: #cbd5e1 !important;
  color: #334155 !important;
}
.hero-btn-outline:hover {
  border-color: #4f46e5 !important;
  color: #4f46e5 !important;
}
.hero-stats {
  display: flex;
  align-items: center;
  gap: 24px;
}
.stat {
  display: flex;
  flex-direction: column;
}
.stat strong {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}
.stat span {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 2px;
}
.stat-divider {
  width: 1px;
  height: 32px;
  background: #e2e8f0;
}

/* Features */
.features {
  padding: 64px 0;
}
.section-header {
  margin-bottom: 40px;
}
.section-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
}
.section-header p {
  font-size: 15px;
  color: #94a3b8;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}
.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.feature-card {
  padding: 28px 24px;
  background: #fff;
  border-radius: 14px;
  transition: transform 0.25s, box-shadow 0.25s;
}
.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
}
.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-bottom: 16px;
}
.feature-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 8px;
}
.feature-card p {
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.5;
}

/* Hot Devices */
.hot-section {
  padding: 0 0 40px;
}
.device-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
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
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}
.device-card:hover .card-img img {
  transform: scale(1.05);
}
.card-tag {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(0,0,0,0.65);
  color: #fff;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  backdrop-filter: blur(4px);
}
.card-body {
  padding: 16px;
}
.card-body h4 {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-brand {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 12px;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #94a3b8;
}
.card-deposit {
  background: #f8f9fc;
  padding: 3px 8px;
  border-radius: 4px;
}
</style>
