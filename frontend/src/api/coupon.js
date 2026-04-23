import request from '@/utils/request'

export const createCoupon = (data) => {
  return request.post('/coupon/create', data)
}

export const receiveCoupon = (data) => {
  return request.post('/coupon/receive', data)
}

export const getUserCoupons = () => {
  return request.get('/coupon/user/me')
}

export const getCouponDetail = (couponId) => {
  return request.get(`/coupon/${couponId}`)
}

// 统一API对象
export const couponApi = {
  create: createCoupon,
  receive: receiveCoupon,
  getUserCoupons: getUserCoupons,
  getById: getCouponDetail,
  getAvailable: () => request.get('/coupon/available'),
  // 管理员接口
  adminList: () => request.get('/coupon/admin/list'),
  adminOffline: (couponId) => request.put(`/coupon/admin/${couponId}/offline`)
}

