# Camera Rental — 前端

> 基于 Vue 3 + Element Plus + Vite 的相机租赁平台前端，包含用户端和管理后台。

> 文档说明：`frontend/README.md` 为前端唯一有效说明文档，历史版本已归档到 `docs/archived/frontend/`。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | 3.3.4 | Composition API + `<script setup>` |
| Vite | 4.4.9 | 构建工具，开发秒启动 |
| Vue Router | 4.2.4 | 路由管理 + 导航守卫 |
| Pinia | 2.1.6 | 状态管理（用户 Token / 信息） |
| Element Plus | 2.4.0 | UI 组件库（中文化） |
| Axios | 1.5.0 | HTTP 请求（拦截器 + Token 刷新） |
| Day.js | 1.11.9 | 日期格式化 |
| Sass | 1.66.1 | CSS 预处理 |

## 功能清单

### 用户端（前台 `/`）

| 页面 | 路由 | 说明 |
|------|------|------|
| 首页 | `/` | Hero 展示 + 平台优势 + 热门设备 |
| 设备列表 | `/devices` | 搜索 + 品牌/价格/排序筛选 + 分页 |
| 设备详情 | `/devices/:id` | 图片轮播 + 设备信息 + 评价列表 + 收藏 |
| 发布设备 | `/device/publish` | 🔐 表单 + 图片上传 |
| 编辑设备 | `/device/edit/:id` | 🔐 预填表单 + 更新 |
| 我的设备 | `/my-devices` | 🔐 设备管理（上/下架、编辑、删除） |
| 创建订单 | `/devices/:deviceId/order` | 🔐 选择租期 + 优惠券 + 计算金额 |
| 订单列表 | `/orders` | 🔐 我的订单（状态筛选 + 订单号搜索） |
| 订单详情 | `/orders/:id` | 🔐 操作按钮（支付/发货/收货/归还/评价） |
| 订单评价 | `/orders/:orderId/evaluate` | 🔐 评分 + 文字 + 图片评价 |
| 优惠券中心 | `/coupon-center` | 可领取优惠券 |
| 我的优惠券 | `/coupons` | 🔐 已领取优惠券列表 |
| 个人中心 | `/profile` | 🔐 用户信息展示 |
| 编辑资料 | `/profile/edit` | 🔐 修改昵称/头像/地址 |
| 修改密码 | `/change-password` | 🔐 旧密码验证 + 新密码 |
| 消息中心 | `/messages` | 🔐 站内消息列表 + 已读/全部已读 |
| 收藏夹 | `/favorites` | 🔐 收藏设备列表 |

> 🔐 = 需要登录

### 管理后台（`/admin`）

| 页面 | 路由 | 说明 |
|------|------|------|
| 数据看板 | `/admin/dashboard` | 用户数 / 设备数 / 订单数统计卡片 |
| 设备审核 | `/admin/devices` | 待审核列表，通过/驳回操作 |
| 用户管理 | `/admin/users` | 用户搜索 + 启用/禁用 |
| 优惠券管理 | `/admin/coupons` | 创建优惠券 + 下架 |
| 订单管理 | `/admin/orders` | 全量订单查询 |

> 管理后台需要 `role = ADMIN` 的账号登录。

### 布局

- **MainLayout** — 前台布局：顶部导航 + 主内容区 + 页脚
- **AdminLayout** — 后台布局：左侧菜单 + 顶部栏 + 主内容区（100vh 全屏）

## 目录结构

```
frontend/
├── src/
│   ├── api/                    # API 接口层
│   │   ├── user.js             #   用户：注册/登录/资料/管理
│   │   ├── device.js           #   设备：CRUD/搜索/上下架
│   │   ├── order.js            #   订单：创建/支付/发货/归还
│   │   ├── coupon.js           #   优惠券：领取/查询/管理
│   │   ├── evaluation.js       #   评价：创建/查询/删除
│   │   ├── favorite.js         #   收藏：添加/取消/列表
│   │   ├── message.js          #   消息：列表/已读/删除
│   │   └── upload.js           #   图片上传
│   ├── views/
│   │   ├── Home.vue            # 首页
│   │   ├── auth/               # 登录 / 注册
│   │   ├── device/             # 设备相关页面（5 个）
│   │   ├── order/              # 订单相关页面（4 个）
│   │   ├── coupon/             # 优惠券页面（2 个）
│   │   ├── user/               # 用户中心页面（5 个）
│   │   └── admin/              # 管理后台页面（5 个）
│   ├── layouts/
│   │   ├── MainLayout.vue      # 前台布局
│   │   └── AdminLayout.vue     # 管理后台布局
│   ├── router/index.js         # 路由定义 + 导航守卫
│   ├── stores/user.js          # Pinia 用户状态
│   ├── utils/
│   │   ├── request.js          # Axios 封装（拦截器/Token刷新/401处理）
│   │   ├── storage.js          # localStorage / sessionStorage 工具
│   │   ├── validate.js         # 表单验证规则（手机号/邮箱/密码/身份证）
│   │   ├── date.js             # 日期格式化
│   │   ├── format.js           # 数据格式化
│   │   ├── common.js           # 通用工具
│   │   └── message.js          # 消息提示封装
│   ├── constants/index.js      # 业务常量（状态映射/品牌列表）
│   ├── components/
│   │   └── ImageUpload.vue     # 图片上传组件
│   ├── App.vue                 # 根组件
│   └── main.js                 # 入口（Element Plus + 图标注册）
├── index.html
├── vite.config.js              # Vite 配置
└── package.json
```

## 快速开始

```bash
# 安装依赖
npm install

# 开发模式（Port 3000）
npm run dev

# 生产构建
npm run build

# 预览构建产物
npm run preview
```

## 开发配置

### API 代理

`vite.config.js` 中配置了开发代理，将 `/api` 前缀请求转发到 Gateway：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // Gateway 地址
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

> 前端所有请求以 `/api` 前缀发出，代理自动去除前缀后转发到 Gateway。

### 构建优化

Vite 构建已配置 `manualChunks` 代码分割：

| 分块 | 包含 |
|------|------|
| `element-plus` | Element Plus + 图标 |
| `vue-vendor` | Vue + Vue Router + Pinia |
| `utils` | Axios + Day.js |

### Token 管理

- 登录后 Token 存储在 `localStorage`
- Axios 请求拦截器自动附加 `Authorization: Bearer <token>`
- 网关返回 `{"code":2005,"message":"TOKEN_EXPIRED"}` 时触发静默刷新（调用 `/user/refresh-token`）
- 其他 401（无 token / 非法 token / 权限不足）不走刷新，直接按未授权流程处理
- 刷新期间的请求进入等待队列，刷新成功后重放
- 刷新失败则清空状态跳转登录页

### 管理面访问说明

- `/doc.html`、`/v3/api-docs/**`、`/**/druid/**`、`/actuator/info`、`/actuator/metrics` 等管理面接口由网关做内网/IP 白名单限制。
- 前端生产环境不应依赖上述管理面地址作为业务流程的一部分。

### 路由守卫

```
meta.requireAuth  → 未登录跳转 /login
meta.requireAdmin → 非 ADMIN 角色跳转 /
```

## 全局样式

- 字体：Inter + Noto Sans SC（Google Fonts）
- 主色：`#4f46e5`（Indigo 600）
- 背景：`#f8f9fc`
- 卡片圆角：`12px`
- 统一滚动条美化
- Element Plus 按钮/表格/对话框圆角覆盖
