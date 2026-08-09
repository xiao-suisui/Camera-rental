import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'
import { userApi } from '@/api/user'
import StorageUtil from '@/utils/storage'

const TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refreshToken'
const USER_INFO_KEY = 'userInfo'

export const useUserStore = defineStore('user', () => {
  const token = ref(StorageUtil.getLocal(TOKEN_KEY) || '')
  const refreshToken = ref(StorageUtil.getLocal(REFRESH_TOKEN_KEY) || '')
  const userInfo = ref(StorageUtil.getLocal(USER_INFO_KEY) || {})

  const isLogin = computed(() => !!token.value)

  const setToken = (newToken) => {
    token.value = newToken
    StorageUtil.setLocal(TOKEN_KEY, newToken)
  }

  const setRefreshToken = (newRefreshToken) => {
    refreshToken.value = newRefreshToken
    StorageUtil.setLocal(REFRESH_TOKEN_KEY, newRefreshToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    StorageUtil.setLocal(USER_INFO_KEY, info)
  }

  const doLogin = async (phone, password) => {
    const res = await userApi.login({ phone, password })
    if (res.code === 0) {
      setToken(res.data.token)
      setRefreshToken(res.data.refreshToken)
      setUserInfo({
        userId: res.data.userId,
        nickname: res.data.nickname,
        role: res.data.role,
        avatar: res.data.avatar,
        address: res.data.address,
        phone: res.data.phone
      })
      return true
    }
    return false
  }

  const doRegister = async (data) => {
    const res = await userApi.register(data)
    return res.code === 0
  }

  const logout = async () => {
    // 调用后端清除 RefreshToken（用原生 axios 避免触发 request 拦截器循环）
    try {
      if (token.value) {
        await axios.post('/api/user/logout', null, {
          headers: { 'Authorization': `Bearer ${token.value}` }
        })
      }
    } catch (e) {
      // 忽略，不影响前端退出流程
    }
    token.value = ''
    refreshToken.value = ''
    userInfo.value = {}
    StorageUtil.removeLocal(TOKEN_KEY)
    StorageUtil.removeLocal(REFRESH_TOKEN_KEY)
    StorageUtil.removeLocal(USER_INFO_KEY)
  }

  return {
    token,
    refreshToken,
    userInfo,
    isLogin,
    setToken,
    setRefreshToken,
    setUserInfo,
    doLogin,
    doRegister,
    logout
  }
})

