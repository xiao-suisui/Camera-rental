import { ElLoading, ElMessage } from 'element-plus'

let loadingInstance = null

/**
 * 显示全局loading
 */
export function showLoading(text = '加载中...') {
  if (loadingInstance) {
    return
  }
  loadingInstance = ElLoading.service({
    lock: true,
    text,
    background: 'rgba(0, 0, 0, 0.7)'
  })
}

/**
 * 隐藏全局loading
 */
export function hideLoading() {
  if (loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
}

/**
 * 成功提示
 */
export function success(message) {
  ElMessage.success(message)
}

/**
 * 错误提示
 */
export function error(message) {
  ElMessage.error(message)
}

/**
 * 警告提示
 */
export function warning(message) {
  ElMessage.warning(message)
}

/**
 * 信息提示
 */
export function info(message) {
  ElMessage.info(message)
}

