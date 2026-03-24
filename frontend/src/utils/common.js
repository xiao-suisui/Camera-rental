/**
 * 防抖函数
 * @param {Function} func 要防抖的函数
 * @param {Number} wait 等待时间（毫秒）
 * @returns {Function}
 */
export function debounce(func, wait = 500) {
  let timeout
  return function(...args) {
    const context = this
    clearTimeout(timeout)
    timeout = setTimeout(() => {
      func.apply(context, args)
    }, wait)
  }
}

/**
 * 节流函数
 * @param {Function} func 要节流的函数
 * @param {Number} wait 等待时间（毫秒）
 * @returns {Function}
 */
export function throttle(func, wait = 500) {
  let previous = 0
  return function(...args) {
    const now = Date.now()
    const context = this
    if (now - previous > wait) {
      func.apply(context, args)
      previous = now
    }
  }
}

/**
 * 深拷贝
 * @param {Object} obj 要拷贝的对象
 * @returns {Object}
 */
export function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj)
  if (obj instanceof Array) return obj.map(item => deepClone(item))

  const cloneObj = {}
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      cloneObj[key] = deepClone(obj[key])
    }
  }
  return cloneObj
}

/**
 * 获取URL参数
 * @param {String} name 参数名
 * @returns {String}
 */
export function getQueryString(name) {
  const reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)', 'i')
  const r = window.location.search.substr(1).match(reg)
  if (r != null) {
    return decodeURIComponent(r[2])
  }
  return null
}

/**
 * 下载文件
 * @param {String} url 文件地址
 * @param {String} filename 文件名
 */
export function downloadFile(url, filename) {
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.style.display = 'none'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

/**
 * 复制到剪贴板
 * @param {String} text 要复制的文本
 */
export async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch (err) {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    const result = document.execCommand('copy')
    document.body.removeChild(textarea)
    return result
  }
}

/**
 * 滚动到顶部
 * @param {Number} duration 动画时长（毫秒）
 */
export function scrollToTop(duration = 500) {
  const start = window.pageYOffset
  const startTime = Date.now()

  const scroll = () => {
    const now = Date.now()
    const time = Math.min(1, (now - startTime) / duration)
    window.scrollTo(0, Math.ceil(start * (1 - time)))

    if (time < 1) {
      requestAnimationFrame(scroll)
    }
  }

  requestAnimationFrame(scroll)
}

