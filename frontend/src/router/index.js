import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue')
      },
      {
        path: 'devices',
        name: 'DeviceList',
        component: () => import('@/views/device/DeviceList.vue')
      },
      {
        path: 'devices/:id',
        name: 'DeviceDetail',
        component: () => import('@/views/device/DeviceDetail.vue')
      },
      {
        path: 'device/publish',
        name: 'DevicePublish',
        component: () => import('@/views/device/DevicePublish.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'device/edit/:id',
        name: 'DeviceEdit',
        component: () => import('@/views/device/DeviceEdit.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'my-devices',
        name: 'MyDevices',
        component: () => import('@/views/device/MyDevices.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'lessor-orders',
        name: 'LessorOrderList',
        component: () => import('@/views/order/LessorOrderList.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/OrderDetail.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'orders/:orderId/evaluate',
        name: 'OrderEvaluate',
        component: () => import('@/views/order/OrderEvaluate.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'devices/:deviceId/order',
        name: 'OrderCreate',
        component: () => import('@/views/order/OrderCreate.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'coupons',
        name: 'CouponList',
        component: () => import('@/views/coupon/CouponList.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'coupon-center',
        name: 'CouponCenter',
        component: () => import('@/views/coupon/CouponCenter.vue')
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/UserProfile.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'profile/edit',
        name: 'ProfileEdit',
        component: () => import('@/views/user/ProfileEdit.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/user/ChangePassword.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'messages',
        name: 'MessageCenter',
        component: () => import('@/views/user/MessageCenter.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'favorites',
        name: 'FavoriteList',
        component: () => import('@/views/user/FavoriteList.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'chat',
        name: 'ChatList',
        component: () => import('@/views/chat/ChatList.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'chat/:conversationId',
        name: 'ChatRoom',
        component: () => import('@/views/chat/ChatRoom.vue'),
        meta: { requireAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue')
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requireAuth: true, requireAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue')
      },
      {
        path: 'devices',
        name: 'AdminDevices',
        component: () => import('@/views/admin/DeviceAudit.vue')
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManage.vue')
      },
      {
        path: 'coupons',
        name: 'AdminCoupons',
        component: () => import('@/views/admin/CouponManage.vue')
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/AdminOrderList.vue')
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 需要登录
  if (to.meta.requireAuth && !userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 需要管理员权限
  if (to.meta.requireAdmin) {
    if (!userStore.token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    if (userStore.userInfo.role !== 'ADMIN') {
      next('/')
      return
    }
  }

  next()
})

export default router

