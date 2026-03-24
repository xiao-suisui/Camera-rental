/**
 * 格式化金额
 */
export const formatMoney = (amount, decimals = 2) => {
  if (!amount && amount !== 0) return '0.00'
  return Number(amount).toFixed(decimals)
}

/**
 * 格式化百分比
 */
export const formatPercent = (value, decimals = 0) => {
  if (!value && value !== 0) return '0%'
  return Number(value).toFixed(decimals) + '%'
}

/**
 * 手机号脱敏
 */
export const maskPhone = (phone) => {
  if (!phone || phone.length !== 11) return phone
  return phone.substring(0, 3) + '****' + phone.substring(7)
}

/**
 * 身份证号脱敏
 */
export const maskIdCard = (idCard) => {
  if (!idCard || idCard.length < 8) return idCard
  return idCard.substring(0, 4) + '**********' + idCard.substring(idCard.length - 4)
}

/**
 * 银行卡号脱敏
 */
export const maskBankCard = (cardNo) => {
  if (!cardNo || cardNo.length < 8) return cardNo
  return cardNo.substring(0, 4) + ' **** **** ' + cardNo.substring(cardNo.length - 4)
}

/**
 * 邮箱脱敏
 */
export const maskEmail = (email) => {
  if (!email || !email.includes('@')) return email
  const [username, domain] = email.split('@')
  if (username.length <= 2) return '**@' + domain
  return username.substring(0, 2) + '****@' + domain
}

/**
 * 文件大小格式化
 */
export const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

/**
 * 数字千分位格式化
 */
export const formatNumber = (num) => {
  if (!num && num !== 0) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

