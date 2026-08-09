# Camera Rental — 后端微服务

> 基于 Spring Cloud Alibaba 的微服务后端，包含 1 个公共模块 + 1 个网关 + 4 个业务服务。

## 模块总览

```
service/
├── camera-rental-common/          # 公共模块（不独立运行）
├── camera-rental-gateway/         # API 网关         → Port 8080
├── camera-rental-user-service/    # 用户服务         → Port 8081
├── camera-rental-device-service/  # 设备服务         → Port 8082
├── camera-rental-order-service/   # 订单服务         → Port 8083
├── camera-rental-coupon-service/  # 优惠券服务       → Port 8084
├── docker-compose.yml             # Docker 一键部署
└── pom.xml                        # Maven 父 POM
```

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.2.4 |
| 微服务 | Spring Cloud Alibaba | 2023.0.1.0 |
| 网关 | Spring Cloud Gateway (WebFlux) | — |
| 注册/配置中心 | Nacos | 2.2+ |
| 流控/熔断 | Sentinel | 1.8.x |
| 消息队列 | RabbitMQ | 3.x |
| 服务调用 | OpenFeign + Fallback | — |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis (Lettuce) | 7.x |
| 连接池 | Druid | 1.2.20 |
| 对象存储 | MinIO | 8.5.x |
| API 文档 | Knife4j (OpenAPI 3) | 4.4.0 |
| JSON | Jackson + Hutool | — |
| 密码加密 | Spring Security Crypto (BCrypt) | — |
| JWT | jjwt | 0.11.x |

## 各模块详细说明

### camera-rental-common（公共模块）

**不可独立运行**，被所有业务服务依赖。

| 包 | 内容 |
|----|------|
| `annotation` | `@AdminOnly`（管理员权限）、`@OperationLog`（操作日志）、`@RepeatSubmit`（防重提交） |
| `aspect` | `OperationLogAspect`（日志切面）、`RepeatSubmitAspect`（Redis 分布式锁防重） |
| `config` | `RedisConfig`（缓存管理器 + 多级 TTL）、`MinioConfig`、`OpenApiConfig`、`WebMvcConfig` |
| `enums` | `ErrorCode`（统一错误码）、`OrderStatus`、`DepositStatus`、`DisputeType`、`DisputeStatus` 等 |
| `exception` | `BusinessException`（业务异常）、`GlobalExceptionHandler`（全局异常捕获） |
| `interceptor` | `AdminInterceptor`（拦截 `@AdminOnly` 注解方法，校验 `X-User-Role` 请求头） |
| `mq` | RabbitMQ 常量定义（Exchange、Queue、RoutingKey） |
| `result` | `Result<T>`（统一响应格式：code + message + data） |
| `service` | `MinioService`（文件上传/下载/删除） |
| `feign` | 跨服务 Feign 客户端接口定义 |

### camera-rental-gateway（API 网关）

| 组件 | 说明 |
|------|------|
| `JwtAuthFilter` | 全局 JWT 鉴权过滤器，解析 Token 后将 `X-User-Id` / `X-User-Role` 写入请求头传递给下游服务 |
| 公开接口 | 仅 `POST /user/register`、`POST /user/login`、`POST /user/refresh-token`、`GET /device/search`、`GET /device/{id}`、`GET /actuator/health` |
| 管理面访问 | OpenAPI/Knife4j、Druid、`/actuator/info`、`/actuator/metrics`、`/admin/**` 仅允许内网/IP 白名单 |
| JWT 过期语义 | 过期 Token 返回 `{"code":2005,"message":"TOKEN_EXPIRED","data":null}` |
| 路由 | 按 `Path` 前缀分发至各服务，支持 OpenAPI / Druid / WebSocket 子路由 |
| Knife4j | 网关层聚合所有服务的 API 文档，访问 `/doc.html`（需内网/IP 白名单） |
| Sentinel | 网关流控 + 降级响应（503） |

> Gateway 基于 WebFlux，**不能**引入 `spring-boot-starter-web`。pom.xml 中对 common 模块做了大量 exclusion。

### camera-rental-user-service（用户服务）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/user/register` | POST | 手机号注册（BCrypt 加密） |
| `/user/login` | POST | 登录，返回 JWT Token |
| `/user/refresh-token` | POST | Token 静默刷新 |
| `/user/logout` | POST | 退出登录，清除 RefreshToken |
| `/user/{userId}` | GET | 获取用户信息（含 avatar/address） |
| `/user/{userId}/profile` | PUT | 更新个人资料（昵称/地址/头像） |
| `/user/{userId}/change-password` | POST | 修改密码 |
| `/user/admin/list` | GET | 管理员：用户列表 |
| `/user/admin/{userId}/disable` | PUT | 管理员：禁用用户 |
| `/user/admin/{userId}/enable` | PUT | 管理员：启用用户 |
| `/message/list` | GET | 消息列表 |
| `/message/unread-count` | GET | 未读消息数 |
| `/message/read` | PUT | 标记已读 |
| `/message/read-all` | PUT | 全部已读 |
| `/message/send` | POST | 发送站内信（供 Feign 调用） |
| `/chat/conversations` | GET | 聊天会话列表 |
| `/chat/messages/{conversationId}` | GET | 聊天消息记录 |
| `/chat/send` | POST | 发送聊天消息 |
| `/chat/unread-total` | GET | 聊天未读总数 |

**WebSocket**：`/ws/chat` — 实时聊天消息推送，基于 STOMP 协议。

### camera-rental-device-service（设备服务）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/device/publish` | POST | 发布设备 |
| `/device/batch-publish` | POST | 批量发布 |
| `/device/search` | GET | 多条件搜索（keyword/brand/price/sort） |
| `/device/{deviceId}` | GET | 设备详情（Redis 缓存） |
| `/device/{deviceId}` | PUT | 更新设备（重新审核） |
| `/device/{deviceId}` | DELETE | 删除设备（逻辑删除） |
| `/device/{deviceId}/online` | POST | 上架 |
| `/device/{deviceId}/offline` | POST | 下架 |
| `/device/my-devices` | GET | 我的设备列表 |
| `/device/audit` | POST | 管理员：审核设备 |
| `/device/admin/list` | GET | 管理员：待审核列表 |
| `/device/upload/image` | POST | 上传单张图片（最大 10MB） |
| `/device/upload/images` | POST | 批量上传图片 |
| `/favorite/add` | POST | 收藏设备 |
| `/favorite/remove` | DELETE | 取消收藏 |
| `/favorite/list` | GET | 收藏列表 |
| `/favorite/check` | GET | 检查是否已收藏 |

**MQ 消费者**：监听 `device.sync.q`，处理订单状态变更事件（如设备状态同步）。

### camera-rental-order-service（订单服务）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/order/create` | POST | 创建订单（Feign 调用设备 + 优惠券服务） |
| `/order/{orderId}/pay` | POST | 模拟支付 |
| `/order/{orderId}/ship` | POST | 出租者发货 |
| `/order/{orderId}/return` | POST | 租用者申请归还 |
| `/order/{orderId}/confirm` | POST | 出租者确认归还 |
| `/order/{orderId}/cancel` | POST | 取消订单（仅待支付） |
| `/order/list` | GET | 我的订单（分页 + 状态筛选） |
| `/order/lessor/list` | GET | 出租订单列表 |
| `/order/admin/list` | GET | 管理员：全量订单 |
| `/evaluation/create` | POST | 创建评价 |
| `/evaluation/device/{deviceId}` | GET | 设备评价列表 |
| `/evaluation/can-evaluate` | GET | 检查是否可评价 |
| `/dispute/create` | POST | 创建纠纷 |
| `/dispute/my` | GET | 我的纠纷列表 |
| `/dispute/admin/list` | GET | 管理员：全量纠纷 |
| `/dispute/admin/{id}` | PUT | 管理员：处理纠纷 |

**定时任务**：
- `OrderTimeoutJob` — 每分钟扫描，超过 30 分钟未支付的订单自动取消
- `OutboxPublishJob` — 定时扫描 `mq_outbox` 表，将 NEW 状态消息发送到 RabbitMQ
- `CouponExpireJob` — 定时更新过期优惠券状态

**MQ 拓扑**：
```
Exchange: camera.rental.order.topic.v1 (topic)
  ├── user.notify.q    → user-service    (订单状态通知 → 站内消息)
  ├── device.sync.q    → device-service  (设备状态同步)
  └── coupon.sync.q    → coupon-service  (优惠券状态同步)
```

### camera-rental-coupon-service（优惠券服务）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/coupon/create` | POST | 管理员：创建优惠券 |
| `/coupon/receive` | POST | 领取优惠券（Redis 原子扣减库存防超发） |
| `/coupon/available` | GET | 可领取优惠券列表 |
| `/coupon/user` | GET | 我的优惠券 |
| `/coupon/admin/list` | GET | 管理员：全量优惠券 |
| `/coupon/admin/{couponId}/offline` | PUT | 管理员：下架优惠券 |

**MQ 消费者**：监听 `coupon.sync.q`，处理订单取消时的优惠券退还。

## 配置管理

采用 **本地 bootstrap.yml + Nacos 远端配置** 分离模式：

- `bootstrap.yml`：Nacos 连接信息、命名空间、共享配置列表
- `application-dev.yml`：本地开发覆盖配置（如端口）
- **Nacos DEFAULT_GROUP**：各服务独立配置（端口、mybatis-plus、sentinel、springdoc）
- **Nacos SHARED_GROUP**：共享配置（datasource、redis、rabbitmq、minio、common）
- **Nacos cipher-config.yaml**：敏感配置（密码、密钥），不入 Git

## 编译与运行

```bash
# 编译
cd service
mvn clean install -DskipTests

# 启动顺序
1. Gateway        → 8080
2. UserService    → 8081
3. DeviceService  → 8082
4. OrderService   → 8083
5. CouponService  → 8084
```

## Docker 部署

```bash
cd service
docker-compose up -d
docker-compose ps
docker-compose logs -f gateway
```

### MQ 拓扑联调检查

RabbitMQ 启动后，可执行以下脚本核对 exchange/queue/binding：

```powershell
Set-Location service
.\scripts\check-mq-topology.ps1 -HostName localhost -ManagementPort 15672 -Username guest -Password guest -VirtualHost /camera-rental
```

若只查看预期资源清单而不发起 API 请求：

```powershell
Set-Location service
.\scripts\check-mq-topology.ps1 -DryRun
```

### 中间件连通性自检

用于快速检查 MySQL/Redis/Nacos/RabbitMQ 是否可访问：

```powershell
Set-Location service
.\scripts\check-middleware.ps1
```

若希望任一失败即返回非 0 退出码（便于 CI/任务脚本）：

```powershell
Set-Location service
.\scripts\check-middleware.ps1 -Strict
```

## 端口分配

| 服务 | 端口 | Sentinel 端口 |
|------|------|---------------|
| Gateway | 8080 | 8720 |
| User Service | 8081 | 8719 |
| Device Service | 8082 | 8721 |
| Order Service | 8083 | 8722 |
| Coupon Service | 8084 | 8723 |
| Nacos | 8848 | — |
| Sentinel Dashboard | 8858 | — |
| MySQL | 3306 | — |
| Redis | 6379 | — |
| RabbitMQ | 5672 (AMQP) / 15672 (管理) | — |
| MinIO | 9000 (API) / 9001 (Console) | — |
