export const ORDER_STATUS = {
  PENDING_PAY: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: 'success' },
  TO_SHIP: { text: '待收货', type: 'info' },
  TO_RETURN: { text: '使用中', type: 'primary' },
  RETURNED: { text: '已归还', type: 'success' },
  OVERDUE: { text: '逾期归还', type: 'danger' },
  FINISHED: { text: '已完成', type: 'info' },
  CANCELED: { text: '已取消', type: 'info' }
}

export const DEPOSIT_STATUS = {
  UNPAID: { text: '未支付', type: 'warning' },
  PAID: { text: '已支付', type: 'success' },
  REFUNDED: { text: '已退还', type: 'info' }
}

export const DEVICE_STATUS = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已拒绝', type: 'danger' },
  ACTIVE: { text: '上架中', type: 'success' },
  OFFLINE: { text: '已下架', type: 'info' }
}

export const COUPON_TYPE = {
  NO_THRESHOLD: '无门槛券',
  THRESHOLD: '满减券'
}

export const COUPON_STATUS = {
  UNUSED: { text: '未使用', type: 'success' },
  USED: { text: '已使用', type: 'info' },
  EXPIRED: { text: '已过期', type: 'danger' }
}

export const USER_ROLE = {
  USER: '普通用户',
  ADMIN: '管理员'
}

export const BRAND_LIST = [
  '佳能',
  '尼康',
  '索尼',
  '富士',
  '松下',
  '奥林巴斯',
  '其他'
]

export const DISPUTE_TYPE = {
  DEVICE_DAMAGE: '设备损坏',
  DEPOSIT_DISPUTE: '押金纠纷',
  OVERDUE: '逾期纠纷',
  DESCRIPTION_MISMATCH: '描述不符',
  OTHER: '其他'
}

export const DISPUTE_STATUS = {
  SUBMITTED: { text: '已提交', type: 'warning' },
  PROCESSING: { text: '处理中', type: 'primary' },
  RESOLVED: { text: '已解决', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' }
}

