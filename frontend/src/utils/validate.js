/**
 * 表单验证规则
 */

/**
 * 手机号验证
 */
export const validatePhone = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入手机号'))
  }
  if (!/^1\d{10}$/.test(value)) {
    return callback(new Error('手机号格式不正确'))
  }
  callback()
}

/**
 * 邮箱验证
 */
export const validateEmail = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入邮箱'))
  }
  if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(value)) {
    return callback(new Error('邮箱格式不正确'))
  }
  callback()
}

/**
 * 密码强度验证
 */
export const validatePassword = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入密码'))
  }
  if (value.length < 6) {
    return callback(new Error('密码长度至少6位'))
  }
  if (!/\d/.test(value) || !/[a-zA-Z]/.test(value)) {
    return callback(new Error('密码必须包含数字和字母'))
  }
  callback()
}

/**
 * 身份证验证
 */
export const validateIdCard = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入身份证号'))
  }
  if (!/^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(value)) {
    return callback(new Error('身份证号格式不正确'))
  }
  callback()
}

/**
 * URL验证
 */
export const validateUrl = (rule, value, callback) => {
  if (!value) {
    return callback()
  }
  if (!/^(https?:\/\/)/.test(value)) {
    return callback(new Error('URL必须以http://或https://开头'))
  }
  callback()
}

/**
 * 金额验证
 */
export const validateMoney = (rule, value, callback) => {
  if (!value && value !== 0) {
    return callback(new Error('请输入金额'))
  }
  if (!/^\d+(\.\d{1,2})?$/.test(value)) {
    return callback(new Error('金额格式不正确，最多两位小数'))
  }
  callback()
}

