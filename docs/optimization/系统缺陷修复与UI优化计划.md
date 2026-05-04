# 缺陷修复 + 前端界面优化计划

> 生成时间：2026-05-04
> 审查范围：后端 5 个微服务 + common 模块 + 前端 Vue 3 应用

## Context

对项目进行全面回归审查，发现后端存在严重业务逻辑缺陷（优惠券未消耗、设备无库存锁定、密码写入日志等），前端存在多个功能 Bug（分页缺失、表单按钮永久禁用、审核理由丢失等），以及大量 UI/UX 体验问题（零移动端适配、无设计令牌、颜色不统一、缺少骨架屏等）。

本计划分 4 个阶段：先修致命缺陷，再修功能 Bug，然后修 HIGH 级别问题，最后做 UI/UX 优化。

---

## 第一阶段：后端严重缺陷（P0 — 必须立即修复）

### 1. 密码明文写入操作日志（CRITICAL-Security）
- **文件**: `service/camera-rental-common/.../aspect/OperationLogAspect.java:62-66`
- **文件**: `service/camera-rental-user-service/.../controller/UserController.java` (changePassword 方法)
- **问题**: `@OperationLog(saveRequestData=true)` 把所有参数序列化写入 DB，包含 `ChangePasswordRequest` 的 `oldPassword`/`newPassword` 明文
- **方案**: (a) `changePassword` 的 `@OperationLog` 设 `saveRequestData=false`；(b) 在 OperationLogAspect 中对序列化前的参数做敏感字段过滤（password/token/secret 字段替换为 `***`）

### 2. 创建订单时优惠券从未标记为已使用（CRITICAL-Business）
- **文件**: `service/camera-rental-order-service/.../service/OrderService.java:79-97`
- **问题**: `createOrder()` 应用优惠券后只算了折扣金额，但从未：(a) 把 UserCoupon 状态改为 USED；(b) 关联 orderId；(c) 增加 Coupon.usedCount。同一张券可无限次使用
- **方案**: 在 order-service 中通过 Feign 调用 coupon-service 的标记接口，或发送 MQ 事件让 coupon-service 消费后标记。更简单的方案：在 `createOrder()` 中，通过 couponFeignClient 调用一个 `consumeCoupon(couponId, orderId, userId)` 接口，由 coupon-service 原子地更新 UserCoupon 状态 + Coupon.usedCount

### 3. 创建订单时无设备库存锁定（CRITICAL-Business）
- **文件**: `service/camera-rental-order-service/.../service/OrderService.java:56-67`
- **问题**: `createOrder()` 只检查 device.status==ACTIVE 但不修改状态，多用户可同时为同一设备下单
- **方案**: 创建订单后通过 Feign/MQ 将设备状态改为 RENTED；需要 device-service 提供 `lockDevice(deviceId)` 接口，使用条件更新 `UPDATE device SET status='RENTED' WHERE id=? AND status='ACTIVE'`，affected rows==0 则拒绝下单

### 4. WebSocket 允许过期 JWT 连接（CRITICAL-Security）
- **文件**: `service/camera-rental-user-service/.../config/JwtService.java:117-128`
- **文件**: `service/camera-rental-user-service/.../websocket/ChatWebSocketHandler.java`
- **问题**: `parseToken()` 捕获 `ExpiredJwtException` 后仍然返回 claims，WebSocket 用它认证，过期 token 可永久连接
- **方案**: (a) 给 JwtService 新增 `parseTokenStrict(token)` 方法，过期时抛异常不返回 claims；(b) WebSocket handler 调用 `parseTokenStrict` 而非 `parseToken`

### 5. 设备有活跃订单时可被删除（CRITICAL-Business）
- **文件**: `service/camera-rental-device-service/.../service/DeviceService.java:202-216`
- **问题**: `deleteDevice()` 直接软删除，不检查是否有进行中的订单
- **方案**: 删除前通过 Feign 调 order-service 查询该设备是否存在非终态订单（PENDING_PAY/PAID/TO_SHIP/TO_RETURN），有则拒绝删除

---

## 第二阶段：前端严重 Bug（P0 — 功能性缺陷）

### 6. 表单 validate() 抛异常导致提交按钮永久禁用（CRITICAL）
- **文件**: `ProfileEdit.vue:101-124`, `ChangePassword.vue`, `OrderEvaluate.vue`, `DevicePublish.vue`, `DeviceEdit.vue`
- **问题**: `await formRef.value.validate()` 在校验失败时抛异常，后续 `submitting.value = true` 和 `finally { submitting.value = false }` 都不执行，`:loading="submitting"` 的按钮永久 disabled
- **方案**: 将 `validate()` 包在 try-catch 中，只有校验通过后才设 `submitting.value = true`

### 7. 审核拒绝理由/备注未传给后端（CRITICAL）
- **文件**: `frontend/src/views/admin/DeviceAudit.vue:214-231`
- **问题**: `confirmReject()` 调用 `auditDevice({ deviceId, auditStatus: 'REJECTED' })` 但未传 `reason` 和 `remark`，管理员选的拒绝原因被静默丢弃
- **方案**: 将 `rejectForm.reason` 和 `rejectForm.remark` 加入请求体

### 8. 订单列表分页完全缺失（CRITICAL）
- **文件**: `OrderList.vue`（租客订单）、`AdminOrderList.vue`（管理端订单）、`LessorOrderList.vue`（出租订单）
- **问题**: 三个页面都没有 `<el-pagination>` 组件。OrderList/AdminOrderList 定义了 `currentPage`/`pageSize` 但从未连接 UI；LessorOrderList 硬编码 `page:1, size:20`，状态过滤还是客户端过滤前 20 条
- **方案**: 三个页面都添加 `<el-pagination>` 组件，绑定 `currentPage`/`pageSize`/`total`，在分页事件中重新请求 API

### 9. ProfileEdit 头像上传未保存 + store 不同步（HIGH）
- **文件**: `frontend/src/views/user/ProfileEdit.vue:107-113`
- **问题**: (a) `handleSubmit` 只传 `nickname` 和 `address`，`avatarList`（头像）被忽略；(b) 直接修改 `userStore.userInfo.nickname` 而非调用 `setUserInfo()`，localStorage 不同步，刷新后旧值恢复
- **方案**: (a) 将头像 URL 加入 API 请求体；(b) 改用 `userStore.setUserInfo({ ...userStore.userInfo, nickname: form.nickname })`

### 10. DeviceDetail 平均评分只计算当前页（HIGH）
- **文件**: `frontend/src/views/device/DeviceDetail.vue:184-188`
- **问题**: `avgScore` 对 `evaluations.value`（当前页 10 条）求平均，多页评价时分数不准
- **方案**: 后端返回的评价列表中每条已有 `score`，但均分应由后端在设备详情中返回 `avgScore` 字段，或前端只在第一页时计算并显示

### 11. DeviceEdit 解析 imagePaths 可能崩溃（HIGH）
- **文件**: `DeviceEdit.vue:127`, `DeviceDetail.vue:177`, `MyDevices.vue:17`, `FavoriteList.vue:85`
- **问题**: `.split(',')` 假设 imagePaths 是逗号分隔字符串，若后端返回数组则 `TypeError`
- **方案**: 增加类型判断：`Array.isArray(paths) ? paths : paths.split(',')`

### 12. Token 刷新逻辑控制流错误（HIGH）
- **文件**: `frontend/src/utils/request.js:54-121`
- **问题**: refresh 成功后 `return request(originalRequest)` 如果重试也失败，会走到 lines 100-107 触发 logout，即使 refresh 刚成功
- **方案**: 重构控制流：refresh 成功分支只处理重试和 pending 队列，refresh 失败分支才做 logout，确保互斥

---

## 第三阶段：中等问题修复（P1）

### 13. 设备标题/描述无 XSS 清理（HIGH-Security）
- **文件**: `DeviceService.java:47-62`
- **方案**: 对 title/brand/model/description/address 调用 `ValidationUtils.sanitizeHtml()`

### 14. 用户状态可设任意值（HIGH-Security）
- **文件**: `UserService.java:211-226`
- **方案**: 校验 status 是否在 `UserStatus` 枚举值内

### 15. 优惠券过期未校验（HIGH-Business）
- **文件**: `OrderService.java:79-97`
- **方案**: 通过 Feign 获取 UserCoupon 记录，校验 `expireTime > now()`

### 16. 设备取消释放缓存未清除（HIGH-Data）
- **文件**: `DeviceService.java:290-307` (handleOrderCancelledReleaseStock)
- **方案**: 添加 `@CacheEvict(value = "device", key = "#event.deviceId")`

### 17. MQ Consumer 冗余 setRollbackOnly（HIGH-Error）
- **文件**: `OrderCancelledReleaseStockConsumer.java:62-68`, `OrderCancelledRollbackCouponConsumer.java:62-68`
- **方案**: 删除 `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()` 调用

### 18. OrderTimeoutJob 事务范围过大（HIGH-Data）
- **文件**: `OrderTimeoutJob.java`
- **方案**: 改为逐条事务处理，或分批提交

### 19. 文件上传 Content-Type 可伪造（MEDIUM-Security）
- **文件**: `UploadController.java:65-75`
- **方案**: 校验文件魔数（magic bytes）而非 Content-Type

### 20. MinioService NPE 风险（MEDIUM）
- **文件**: `MinioService.java:52-53`
- **方案**: 增加 `originalFilename` null 检查和扩展名提取保护

### 21. 统计端点应限管理员（MEDIUM-Security）
- **文件**: 上一轮已加了 `@RequestHeader("X-User-Id")`，但需确认是否应加 `@AdminOnly`
- **方案**: 检查前端 Dashboard 是否只有管理员调用，是则改为 `@AdminOnly`

### 22. AdminOrderList 状态映射与常量不一致（MEDIUM）
- **文件**: `AdminOrderList.vue:104-107`
- **方案**: 改用 `ORDER_STATUS` 常量

---

## 第四阶段：前端 UI/UX 优化（P2）

### 23. 定义 CSS 设计令牌（Design Tokens）
- **新建文件**: `frontend/src/styles/tokens.css`
- **问题**: 颜色/间距/字号等硬编码在各组件中，不统一
- **方案**: 定义 CSS 变量（`--color-primary: #4f46e5`, `--color-danger: #e11d48`, `--spacing-sm/md/lg/xl` 等），在 `App.vue` 中引入，逐步替换各组件硬编码值

### 24. 统一颜色系统
- **涉及文件**: `OrderCreate.vue`, `FavoriteList.vue`, `ChatRoom.vue`, `ChatList.vue`, `ImageUpload.vue`, `CouponList.vue`
- **问题**: 混用 Element Plus 默认色（`#409eff`, `#f56c6c`）与品牌色（`#4f46e5`, `#e11d48`）
- **方案**: 将所有 `#409eff` → 品牌主色, `#f56c6c` → 品牌危险色, `CouponList.vue` 渐变统一为 `#4f46e5 → #7c3aed`

### 25. 提取可复用组件
- **新建**: `PageHeader.vue`（标题 + 返回按钮 + 面包屑）
- **新建**: `EmptyState.vue`（统一空状态，封装 el-empty）
- **新建**: `SkeletonList.vue`（骨架屏替代 v-loading 旋转器）
- **问题**: 各页面重复实现返回按钮、空状态、加载状态

### 26. 响应式适配（移动端）
- **涉及文件**: `MainLayout.vue`, `Home.vue`, `DeviceList.vue`, `CouponCenter.vue`, `Login.vue`, `ChatRoom.vue`, 所有表格页面
- **问题**: 全站零移动端支持，grid 全部硬编码 4 列
- **方案**:
  - `MainLayout`: 添加 hamburger 菜单，nav-links 在 <768px 隐藏为抽屉
  - 所有 `grid: repeat(4,1fr)` → `repeat(auto-fill, minmax(280px, 1fr))`
  - 表格页面：移动端改用卡片布局或 el-table 的 `max-height` + 横向滚动
  - Login/Register：移动端取消分屏，品牌面板改为顶部横条
  - AdminLayout：移动端 sidebar 改为抽屉

### 27. 页面过渡动画
- **文件**: `MainLayout.vue`, `AdminLayout.vue`
- **方案**: `<router-view>` 包裹 `<transition name="fade">`，添加全局 fade 样式

### 28. 缺失状态补全
- **OrderList.vue / LessorOrderList.vue / AdminOrderList.vue**: 添加 `<el-empty description="暂无订单">`
- **Home.vue**: 热门设备空状态改为 `<el-empty>` 统一组件
- **CouponList.vue**: 添加 `v-loading` 指令

### 29. 面包屑导航
- **文件**: `MainLayout.vue`
- **方案**: header 下方添加 `<el-breadcrumb>`，根据当前路由自动生成路径

### 30. UserProfile 页面丰富
- **文件**: `frontend/src/views/user/UserProfile.vue`
- **问题**: 只显示 userId/nickname/role 三个字段
- **方案**: 添加头像展示、地址、注册时间、统计数据（发布设备数、订单数、收藏数）

### 31. DevicePublish 表单分区
- **文件**: `DevicePublish.vue`, `DeviceEdit.vue`
- **问题**: 8+ 字段的长表单无视觉分组
- **方案**: 用 `<el-divider>` 或 `<el-collapse>` 分为"基本信息""价格与日租""位置与图片"三个区块
- **同时**: 将 DeviceEdit 和 DevicePublish 的重复代码提取为共享组件 `DeviceForm.vue`

### 32. 骨架屏加载
- **涉及**: 设备列表、设备详情、订单列表等高频页面
- **方案**: 数据加载时显示灰色占位块（骨架屏），替代 v-loading 遮罩旋转器

### 33. 首页"我要出租"按钮登录态判断
- **文件**: `Home.vue:14`
- **方案**: 添加 `v-if="userStore.isLogin"` 或点击时引导登录

---

## 涉及文件清单

| 阶段 | 文件 | 改动 |
|------|------|------|
| P0 | `OperationLogAspect.java` | 敏感字段过滤 + changePassword 不记录请求体 |
| P0 | `OrderService.java` | 创建订单时标记优惠券已使用 + 锁定设备 |
| P0 | `CouponService.java` + Feign/MQ | 新增 consumeCoupon 接口 |
| P0 | `DeviceService.java` | 新增 lockDevice 条件更新 |
| P0 | `JwtService.java` | 新增 parseTokenStrict |
| P0 | `ChatWebSocketHandler.java` | 改用 parseTokenStrict |
| P0 | 5 个前端表单页面 | validate() 包 try-catch |
| P0 | `DeviceAudit.vue` | 传 reason/remark 到 API |
| P0 | `OrderList.vue` / `AdminOrderList.vue` / `LessorOrderList.vue` | 添加 el-pagination |
| P0 | `ProfileEdit.vue` | 头像保存 + store 同步 |
| P1 | `DeviceDetail.vue` / `DeviceEdit.vue` / `MyDevices.vue` / `FavoriteList.vue` | imagePaths 类型保护 |
| P1 | `request.js` | token 刷新控制流重构 |
| P1 | `DeviceService.java` | XSS 清理 + cache evict |
| P1 | `UserService.java` | 状态枚举校验 |
| P1 | `UploadController.java` | 文件魔数校验 |
| P1 | `MinioService.java` | NPE 保护 |
| P1 | MQ Consumer 文件 | 删除冗余 setRollbackOnly |
| P2 | `tokens.css` (新建) | CSS 设计令牌 |
| P2 | 6 个前端页面 | 颜色统一 |
| P2 | `PageHeader.vue` / `EmptyState.vue` / `SkeletonList.vue` / `DeviceForm.vue` (新建) | 可复用组件 |
| P2 | `MainLayout.vue` / `AdminLayout.vue` | 响应式 + 过渡动画 + 面包屑 |
| P2 | `UserProfile.vue` | 页面丰富 |
| P2 | `Home.vue` | 空状态 + 登录态按钮 |

## 验证方式

1. **编译**: `cd service && mvn clean compile` + `cd frontend && npm run build`
2. **优惠券**: 创建订单后检查 UserCoupon.useStatus==USED, Coupon.usedCount+1
3. **设备锁定**: 同一设备同时两用户下单，仅一个成功
4. **密码日志**: 修改密码后查 operation_log 表，request_params 不含密码明文
5. **WebSocket**: 使用过期 token 连接 WS → 应被拒绝
6. **分页**: 订单列表 >10 条时应显示分页组件，可翻页
7. **表单**: 校验失败后提交按钮不应永久 disabled
8. **审核**: 拒绝设备时选择原因 → 后端应收到 reason 字段
9. **响应式**: Chrome DevTools 模拟 375px 宽度，所有页面应正常显示
10. **颜色**: 全局搜 `#409eff` 和 `#f56c6c`，应全部替换为品牌色
