import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 是否正在刷新 Token
let isRefreshing = false
// 等待队列
let pendingRequests = []
// 防止重复弹出过期提示
let hasShownExpired = false

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 0) {
      return res
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  async error => {
    const originalRequest = error.config
    const userStore = useUserStore()

    // Token 过期，尝试用 RefreshToken 静默刷新
    if (error.response?.status === 401 && userStore.refreshToken && !originalRequest._retry) {
      if (isRefreshing) {
        // 正在刷新中，把请求加入等待队列
        return new Promise((resolve) => {
          pendingRequests.push(() => {
            originalRequest.headers['Authorization'] = `Bearer ${userStore.token}`
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 双令牌刷新：将 refreshToken + userId 放在请求体中，不依赖 AccessToken
        const res = await axios.post('/api/user/refresh-token', {
          userId: userStore.userInfo.userId,
          refreshToken: userStore.refreshToken
        })
        if (res.data.code === 0) {
          const { token, refreshToken, userId, nickname, role } = res.data.data
          userStore.setToken(token)
          userStore.setRefreshToken(refreshToken)
          userStore.setUserInfo({ userId, nickname, role })

          // 重放等待队列
          pendingRequests.forEach(cb => cb())
          pendingRequests = []

          // 重试原始请求
          originalRequest.headers['Authorization'] = `Bearer ${token}`
          return request(originalRequest)
        }
      } catch (refreshError) {
        // 刷新也失败（RefreshToken 无效/过期），退出登录
      } finally {
        isRefreshing = false
      }

      // 刷新失败，清理并跳转登录
      if (!hasShownExpired) {
        hasShownExpired = true
        ElMessage.error('登录已过期，请重新登录')
        await userStore.logout()
        router.push('/login')
        setTimeout(() => { hasShownExpired = false }, 3000)
      }
    } else if (error.response?.status === 401) {
      if (!hasShownExpired) {
        hasShownExpired = true
        ElMessage.error('登录已过期，请重新登录')
        await userStore.logout()
        router.push('/login')
        setTimeout(() => { hasShownExpired = false }, 3000)
      }
    } else if (error.response?.status === 429) {
      ElMessage.warning(error.response.data?.message || '操作过于频繁，请稍后再试')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request

