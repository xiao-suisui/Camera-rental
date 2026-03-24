/**
 * 本地存储工具类
 */
class StorageUtil {
  /**
   * 设置localStorage
   */
  static setLocal(key, value) {
    try {
      const data = JSON.stringify(value)
      localStorage.setItem(key, data)
    } catch (error) {
      console.error('存储失败:', error)
    }
  }

  /**
   * 获取localStorage
   */
  static getLocal(key) {
    try {
      const data = localStorage.getItem(key)
      return data ? JSON.parse(data) : null
    } catch (error) {
      console.error('读取失败:', error)
      return null
    }
  }

  /**
   * 删除localStorage
   */
  static removeLocal(key) {
    localStorage.removeItem(key)
  }

  /**
   * 清空localStorage
   */
  static clearLocal() {
    localStorage.clear()
  }

  /**
   * 设置sessionStorage
   */
  static setSession(key, value) {
    try {
      const data = JSON.stringify(value)
      sessionStorage.setItem(key, data)
    } catch (error) {
      console.error('存储失败:', error)
    }
  }

  /**
   * 获取sessionStorage
   */
  static getSession(key) {
    try {
      const data = sessionStorage.getItem(key)
      return data ? JSON.parse(data) : null
    } catch (error) {
      console.error('读取失败:', error)
      return null
    }
  }
}

export default StorageUtil

