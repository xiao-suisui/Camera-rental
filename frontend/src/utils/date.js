import dayjs from 'dayjs'

/**
 * 日期格式化
 */
export const formatDate = (date, format = 'YYYY-MM-DD') => {
  if (!date) return ''
  return dayjs(date).format(format)
}

/**
 * 日期时间格式化
 */
export const formatDateTime = (date) => {
  return formatDate(date, 'YYYY-MM-DD HH:mm:ss')
}

/**
 * 相对时间
 */
export const fromNow = (date) => {
  if (!date) return ''
  return dayjs(date).fromNow()
}

/**
 * 计算天数差
 */
export const diffDays = (startDate, endDate) => {
  return dayjs(endDate).diff(dayjs(startDate), 'day')
}

/**
 * 是否是今天
 */
export const isToday = (date) => {
  return dayjs(date).isSame(dayjs(), 'day')
}

/**
 * 是否过期
 */
export const isExpired = (date) => {
  return dayjs(date).isBefore(dayjs())
}

