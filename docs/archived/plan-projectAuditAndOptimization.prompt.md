# 相机租赁平台全面审查 — 优化 · 冗余排查 · 执行计划

> 基于项目深度审查 + 用户反馈，制定以下 11 步执行计划。  
> 最后更新：2026-03-04

---

## 用户确认的决策

| # | 问题 | 决策 |
|---|------|------|
| 1 | 订单流程是否需要 `TO_SHIP` | ✅ 需要完整流程：支付→发货→确认收货→使用中→归还 |
| 2 | MinIO 图片访问方式 | ✅ 长期公开访问（永久链接） |
| 3 | OpenFeign 使用意向 | ✅ 需要使用，保留各服务的 `@EnableFeignClients` |
| 4 | 操作日志持久化 | ✅ 需要写入数据库表 |
| 5 | `frontend/dist/` 纳入 Git | ❌ 不纳入（已在 `.gitignore`，需确认已 untrack） |
| 6 | 数据库拆分 | ✅ 准备逐步拆分，本阶段先出规划文档 |

---

## Step 1：修复订单状态流程（补全 TO_SHIP 状态）

**目标：** 实现完整订单生命周期 `PENDING_PAY → PAID → TO_SHIP → TO_RETURN → RETURNED/OVERDUE → FINISHED`

### 1.1 后端 — OrderService.java

**文件：** `service/camera-rental-order-service/src/main/java/com/example/camerarental/order/service/OrderService.java`

- `ship()` 方法（约第 123 行）：将 `order.setStatus("TO_RETURN")` 改为 `order.setStatus("TO_SHIP")`
- 新增 `confirmReceive(Long orderId)` 方法：
  - 校验订单存在且状态为 `TO_SHIP`
  - 将状态改为 `TO_RETURN`
  - 发送站内信通知出租者"租用者已确认收货"

### 1.2 后端 — OrderController.java

**文件：** `service/camera-rental-order-service/src/main/java/com/example/camerarental/order/controller/OrderController.java`

- 新增端点：`POST /{orderId}/confirm-receive` → 调用 `orderService.confirmReceive(orderId)`

### 1.3 前端 — API 层

**文件：** `frontend/src/api/order.js`

- 在 `orderApi` 中新增：`confirmReceive: (orderId) => request.post(`/order/${orderId}/confirm-receive`)`

### 1.4 前端 — 常量统一

**文件：** `frontend/src/constants/index.js`

- `ORDER_STATUS` 补全 `OVERDUE: { text: '逾期', type: 'danger' }`
- 确认 `TO_SHIP: { text: '待收货', type: 'info' }` 存在且描述正确

### 1.5 前端 — OrderDetail.vue

**文件：** `frontend/src/views/order/OrderDetail.vue`

- 新增"确认收货"按钮：当 `order.status === 'TO_SHIP' && !isLessor` 时显示
- "申请归还"按钮条件保持 `order.status === 'TO_RETURN'`

### 1.6 网关白名单

无需改动，`/order/**` 路径已受 JWT 保护，新端点自动受保护。

**风险：** `TO_SHIP` 值已存在于数据库 ENUM（schema.sql 第 68 行），无需 DDL 变更。已有 `PAID` 状态的订单将走新流程。

---

## Step 2：提取业务状态枚举（替换硬编码字符串）

**目标：** 消除后端所有魔法字符串，前端同步对齐

### 2.1 新增枚举类

**目录：** `service/camera-rental-common/src/main/java/com/example/camerarental/common/enums/`

| 文件 | 枚举值 |
|------|--------|
| `OrderStatus.java` | `PENDING_PAY, PAID, TO_SHIP, TO_RETURN, RETURNED, OVERDUE, FINISHED, CANCELED` |
| `DepositStatus.java` | `UNPAID, PAID, REFUNDED` |
| `DeviceStatus.java` | `ACTIVE, OFFLINE` |
| `AuditStatus.java` | `PENDING, APPROVED, REJECTED` |
| `UserRole.java` | `USER, ADMIN` |
| `CouponStatus.java` | `DRAFT, PUBLISHED, EXPIRED, OFFLINE` |
| `MessageType.java` | `DEVICE_AUDIT, ORDER_STATUS, COUPON, SYSTEM` |

每个枚举实现 `getValue()` 返回 `name()`，方便与数据库字符串匹配。

### 2.2 重构涉及文件

- `OrderService.java` — 约 15 处 `.equals("xxx")` / `.setStatus("xxx")`
- `DeviceService.java` — 约 10 处
- `CouponService.java` — 约 5 处
- `UserService.java` — 约 5 处 (`"DISABLED"`, `"ACTIVE"`, `"ADMIN"`, `"USER"`)
- `AdminInterceptor.java` — `"ADMIN"` → `UserRole.ADMIN.name()`
- `OrderTimeoutJob.java` — `"PENDING_PAY"` / `"CANCELED"`
- `MessageService.java` — `"UNREAD"` / `"READ"`

### 2.3 前端同步

**文件：** `frontend/src/constants/index.js`

- 补齐所有缺失状态值（特别是 `OVERDUE`），`TO_SHIP` 描述改为"待收货"
- 移除 `AdminOrderList.vue` 中硬编码的状态映射，统一从 `constants` 导入

---

## Step 3：修复 MinIO — 返回永久公开链接

**目标：** 上传图片返回永久可访问的公开 URL，替换 7 天过期的预签名链接

### 3.1 MinioService.java

**文件：** `service/camera-rental-common/src/main/java/com/example/camerarental/common/service/MinioService.java`

- 注入 `@Value("${minio.endpoint}") private String endpoint;`
- `uploadFile()` 中：桶不存在时创建后，调用 `setBucketPolicy()` 设置公开读策略：
  ```json
  {
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"AWS": ["*"]},
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::camera-rental/*"]
    }]
  }
  ```
- `getFileUrl()` 方法改为简单拼接：`return endpoint + "/" + bucketName + "/" + fileName;`
- 移除 `getPresignedObjectUrl` 调用及相关 import

### 3.2 已有数据迁移

提供一次性 SQL 脚本（放入 `sql/migration/` 目录），将 `device.image_paths` 中的预签名 URL 截取为永久格式：
```sql
UPDATE device SET image_paths = REGEXP_REPLACE(image_paths, '\\?.*', '') WHERE image_paths LIKE '%?X-Amz%';
```

**风险：** 需确保 MinIO 服务端允许 Bucket Policy 设置。自建 MinIO 默认支持。

---

## Step 4：操作日志持久化到数据库

**目标：** `@OperationLog` 注解标记的操作，除日志输出外，同时写入 `operation_log` 表

### 4.1 新增数据库表

**文件：** `sql/schema.sql`（追加）

```sql
CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  type VARCHAR(64) NOT NULL COMMENT '操作类型',
  description VARCHAR(256) NOT NULL COMMENT '操作描述',
  method VARCHAR(256) DEFAULT NULL COMMENT '请求方法（类名.方法名）',
  url VARCHAR(512) DEFAULT NULL COMMENT '请求URL',
  request_method VARCHAR(16) DEFAULT NULL COMMENT 'HTTP方法',
  request_params TEXT DEFAULT NULL COMMENT '请求参数JSON',
  response_result TEXT DEFAULT NULL COMMENT '响应结果JSON',
  user_id BIGINT DEFAULT NULL COMMENT '操作人ID（来自X-User-Id）',
  ip VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
  execution_time BIGINT DEFAULT NULL COMMENT '执行耗时（ms）',
  status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT '执行状态',
  error_msg TEXT DEFAULT NULL COMMENT '异常信息',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_oplog_type (type),
  KEY idx_oplog_user (user_id),
  KEY idx_oplog_created (created_at)
) ENGINE=InnoDB COMMENT='操作日志';
```

### 4.2 新增实体和 Mapper

**目录：** `service/camera-rental-common/src/main/java/com/example/camerarental/common/`

- `entity/OperationLogEntity.java` — MyBatis-Plus 实体，`@TableName("operation_log")`
- `mapper/OperationLogMapper.java` — `extends BaseMapper<OperationLogEntity>`

### 4.3 重构 OperationLogAspect.java

**文件：** `service/camera-rental-common/src/main/java/com/example/camerarental/common/aspect/OperationLogAspect.java`

- `@Autowired(required = false) OperationLogMapper operationLogMapper;`
- 在 `finally` 块中构建 `OperationLogEntity`，填充所有字段
  - `userId` 从 `request.getHeader("X-User-Id")` 获取
  - `ip` 从 request 获取
  - `executionTime` 从计时计算
  - `status` = SUCCESS / ERROR
  - `errorMsg` 从异常获取
- 使用 `@Async` 异步插入，避免阻塞业务
- 保留原有 `log.info()` 输出

### 4.4 各服务 MapperScan 扩展

每个业务服务的 Application 类的 `@MapperScan` 需追加 `com.example.camerarental.common.mapper`：

- `UserServiceApplication.java` — `@MapperScan({"com.example.camerarental.user.mapper", "com.example.camerarental.common.mapper"})`
- `DeviceServiceApplication.java` — 同理
- `OrderServiceApplication.java` — 同理
- `CouponServiceApplication.java` — 同理

### 4.5（可选）管理后台查询接口

后续可在 user-service 或单独的 admin-service 中提供操作日志查询接口，本阶段暂不实现。

**风险：** 异步写入可能丢失少量日志（服务崩溃时）。生产环境可考虑用消息队列保证可靠性，当前阶段 `@Async` 足够。

---

## Step 5：补全 Feign Fallback 降级类

**目标：** 所有 Feign Client 均配备 Fallback，服务不可用时优雅降级

### 5.1 新增 Fallback 类

**目录：** `service/camera-rental-order-service/src/main/java/com/example/camerarental/order/fallback/`

- `CouponFeignClientFallback.java` — 实现 `CouponFeignClient`
  - `getById()` → `return Result.error("优惠券服务暂不可用，请稍后重试");`

- `MessageFeignClientFallback.java` — 实现 `MessageFeignClient`
  - `sendMessage()` → log.error + `return Result.error("消息服务暂不可用");`

### 5.2 修改 Feign Client 注解

- `CouponFeignClient.java` — `@FeignClient(name=..., path=..., fallback = CouponFeignClientFallback.class)`
- `MessageFeignClient.java` — `@FeignClient(name=..., contextId=..., fallback = MessageFeignClientFallback.class)`

### 5.3 确认 Sentinel 降级配置

**文件：** `nacos_config/DEFAULT_GROUP/camera-rental-order-service.yaml`

确认已有 `feign.sentinel.enabled: true`（第 21 行已存在）。Sentinel 会拦截 Feign 调用并触发 Fallback。

---

## Step 6：保留 @EnableFeignClients（添加说明注释）

**目标：** 用户确认需要 OpenFeign，保留各服务的注解，为后续跨服务调用预留

### 6.1 添加注释

- `DeviceServiceApplication.java` — `@EnableFeignClients` 行上方加注释 `// 预留：后续可能调用 user-service 获取用户昵称等`
- `CouponServiceApplication.java` — 同理

无代码逻辑变更。

---

## Step 7：确认 .gitignore 生效（untrack dist/）

**目标：** 确保 `frontend/dist/` 不被 Git 追踪

### 7.1 检查 & 执行

`.gitignore` 已包含 `frontend/dist/` 和 `dist/`（第 42-43 行）。

如果该目录已被提交过，需执行：
```bash
git rm -r --cached frontend/dist/
git commit -m "chore: untrack frontend/dist/"
```

---

## Step 8：引入 VO 层（隐藏内部字段）

**目标：** Controller 返回 VO 而非实体，避免泄露 `deleted`、`phone`、内部 ID 等字段

### 8.1 新增 VO 类

| 服务 | VO 类 | 暴露字段 | 隐藏字段 |
|------|-------|---------|---------|
| user-service | `UserVO.java` | id, nickname, avatar, address, role, createdAt | password, phone, status, updatedAt |
| device-service | `DeviceVO.java` | id, userId, title, brand, model, description, imagePaths, rentPrice, deposit, address, auditStatus, status, createdAt | deleted, batchId, updatedAt |
| order-service | `OrderVO.java` | id, orderNo, userId, deviceId, lessorId, startTime, endTime, totalRent, deposit, couponId, discountAmount, payAmount, virtualOrderNo, status, depositStatus, createdAt | updatedAt |

### 8.2 转换方式

手动映射（无需引入 MapStruct，保持简洁）：
- 在每个 VO 类中提供 `public static XxxVO fromEntity(Xxx entity)` 静态工厂方法
- Controller 中调用 `.map(XxxVO::fromEntity)` 转换列表

### 8.3 修改 Controller

- `UserController.getUserById()` → 返回 `Result<UserVO>`
- `DeviceController.getById()` / `search()` / `adminListDevices()` → 返回 `DeviceVO`
- `OrderController.getDetail()` / `list()` → 返回 `OrderVO`

**风险：** 前端可能依赖某些被隐藏的字段。`OrderVO` 保留 `userId` / `lessorId` 因为前端 OrderDetail.vue 用它判断 `isLessor`。

---

## Step 9：补全订单接口权限校验（防 IDOR）

**目标：** 所有订单操作接口校验 `X-User-Id`，防止越权

### 9.1 OrderController.java 修改

每个操作端点增加 `@RequestHeader("X-User-Id") String currentUserId` 参数：

| 端点 | 校验规则 |
|------|---------|
| `POST /{orderId}/pay` | `currentUserId == order.userId`（租用者操作） |
| `POST /{orderId}/cancel` | `currentUserId == order.userId`（租用者操作） |
| `POST /{orderId}/ship` | `currentUserId == order.lessorId`（出租者操作） |
| `POST /{orderId}/confirm-receive` | `currentUserId == order.userId`（租用者操作） |
| `POST /{orderId}/return` | `currentUserId == order.userId`（租用者操作） |
| `POST /{orderId}/confirm` | `currentUserId == order.lessorId`（出租者操作） |

### 9.2 通用校验方法

在 `OrderService` 中提取：
```java
private void verifyOrderOwner(Order order, Long currentUserId, boolean mustBeLessor) {
    Long expected = mustBeLessor ? order.getLessorId() : order.getUserId();
    if (!expected.equals(currentUserId)) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此订单");
    }
}
```

---

## Step 10：统一前端 API 导出风格

**目标：** 所有 API 文件统一为 `xxxApi` 对象式导出

### 10.1 重构文件

| 文件 | 改动 |
|------|------|
| `api/user.js` | 包裹为 `export const userApi = { register, login, ... }` |
| `api/device.js` | 移除顶层独立 `export const` 函数，仅保留 `export const deviceApi` |
| `api/coupon.js` | 移除顶层独立 `export const` 函数，仅保留 `export const couponApi` |
| `api/order.js` | 移除底部兼容导出（`createOrder`, `payOrder`, `getOrderList`） |

### 10.2 更新消费者

- `stores/user.js` — `import { userApi } from '@/api/user'` → `userApi.login(...)`, `userApi.register(...)`
- `views/Home.vue` — `import { deviceApi } from '@/api/device'` → `deviceApi.search(...)`
- `views/device/*.vue` — 统一改为 `deviceApi.xxx`
- `views/admin/UserManage.vue` — `userApi.adminListUsers`
- `views/admin/DeviceAudit.vue` — 纯 `deviceApi.xxx`
- `views/coupon/*.vue` — 纯 `couponApi.xxx`
- `views/user/ProfileEdit.vue` — `userApi.updateProfile`


## 执行优先级

| 优先级 | Step | 理由 |
|--------|------|------|
| 🔴 P0 | Step 1 | 业务流程缺陷，影响核心功能 |
| 🔴 P0 | Step 3 | 图片链接 7 天后失效，影响线上数据 |
| 🟡 P1 | Step 2 | 消除魔法字符串，提升可维护性 |
| 🟡 P1 | Step 5 | 服务降级保障，影响系统稳定性 |
| 🟡 P1 | Step 9 | 安全漏洞（IDOR 越权） |
| 🟢 P2 | Step 4 | 审计需求，不阻塞业务 |
| 🟢 P2 | Step 8 | 数据安全，不紧急 |
| 🟢 P2 | Step 10 | 代码规范，不影响功能 |
| ⚪ P3 | Step 6 | 仅添加注释 |
| ⚪ P3 | Step 7 | 仅 Git 操作 |
| ⚪ P3 | Step 11 | 纯文档输出 |
