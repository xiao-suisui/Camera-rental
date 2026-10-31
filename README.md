# LensGo — 相机租赁平台

> 基于 Spring Cloud Alibaba 微服务架构的相机租赁平台。前后端分离，覆盖从设备发布、下单租赁到归还评价的完整业务闭环，内置 IM 聊天、纠纷处理与管理后台。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.1.0-267DFF?logo=alibabacloud&logoColor=white)](https://github.com/alibaba/spring-cloud-alibaba)
[![Vue 3](https://img.shields.io/badge/Vue-3.3-4FC08D?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 目录

- [系统架构](#-系统架构)
- [功能概览](#-功能概览)
- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [快速开始](#-快速开始)
- [环境配置](#-环境配置)
- [Docker 部署](#-docker-部署)
- [数据库设计](#-数据库设计)
- [API 文档](#-api-文档)
- [项目文档](#-项目文档)
- [贡献指南](#-贡献指南)
- [开源协议](#-开源协议)

---

## 系统架构

```
                          ┌─────────────────────┐
                          │  Vue 3 + Vite        │
                          │  (localhost:3000)     │
                          └──────────┬───────────┘
                                     │ HTTP / WebSocket
                          ┌──────────▼───────────┐
                          │  Spring Cloud Gateway │
                          │  (8080)               │
                          │  JWT 鉴权 · 路由聚合   │
                          │  Sentinel 限流         │
                          │  Knife4j 文档聚合      │
                          └───┬────┬────┬────┬───┘
                              │    │    │    │
          ┌───────────────────┘    │    │    └───────────────────┐
          ▼                        ▼    ▼                        ▼
 ┌─────────────────┐  ┌────────────────┐  ┌─────────────────┐  ┌────────────────┐
 │  User Service   │  │ Device Service │  │ Order Service   │  │ Coupon Service │
 │  (8081)         │  │ (8082)         │  │ (8083)          │  │ (8084)         │
 │                 │  │                │  │                 │  │                │
 │ · 注册 / 登录   │  │ · 设备 CRUD    │  │ · 订单全流程    │  │ · 优惠券创建   │
 │ · JWT 签发      │  │ · 多条件搜索   │  │ · 评价系统      │  │ · Redis 防超发 │
 │ · IM 聊天       │  │ · 图片上传     │  │ · 纠纷处理      │  │ · 领取 / 核销  │
 │ · 站内消息      │  │ · 收藏夹       │  │ · OpenFeign 调用│  │ · 过期自动更新 │
 │ · 用户管理      │  │ · WebSocket    │  │ · Outbox 模式   │  │                │
 └───────┬─────────┘  └───────┬────────┘  └───────┬─────────┘  └───────┬────────┘
         │                    │                    │                    │
 ┌───────▼────────────────────▼────────────────────▼────────────────────▼────────┐
 │                          基础设施层                                            │
 │  MySQL 8.0  ·  Redis 7  ·  MinIO  ·  Nacos 2.2+  ·  RabbitMQ               │
 │  Sentinel Dashboard  ·  Druid 监控                                            │
 └──────────────────────────────────────────────────────────────────────────────-┘
```

## 功能概览

### 用户端（前台）

| 模块 | 功能 |
|------|------|
| **认证** | 手机号注册 / 登录，JWT 双 Token 自动刷新 |
| **设备浏览** | 关键词 + 品牌 + 价格区间多条件搜索，分页排序 |
| **设备发布** | 单台 / 批量发布，MinIO 图片上传（最多 9 张，单张 10MB） |
| **收藏夹** | 设备收藏 / 取消收藏（上限 50） |
| **租赁下单** | 选择租期 → 应用优惠券 → 计算金额 → 下单 |
| **订单流程** | 待支付 → 已支付 → 待发货 → 待归还 → 已归还/逾期 → 完成 |
| **订单评价** | 完成后可评分 + 文字 + 图片评价 |
| **纠纷处理** | 订单纠纷提交，支持设备损坏/押金/逾期等多种类型 |
| **IM 聊天** | 租用者与出租者实时聊天，WebSocket 消息推送 |
| **优惠券** | 领取中心，无门槛券 / 满减券，过期自动状态更新 |
| **站内消息** | 订单状态变更自动推送，未读红点提示 |
| **个人中心** | 头像上传、修改昵称/地址、修改密码 |

### 管理后台

| 模块 | 功能 |
|------|------|
| **数据看板** | 用户数、设备数、订单数实时统计 + 订单趋势图 |
| **设备审核** | 查看待审核设备，通过 / 驳回 |
| **用户管理** | 用户列表、搜索、启用 / 禁用 |
| **订单管理** | 全量订单查询，状态 + 订单号筛选 |
| **优惠券管理** | 创建优惠券、下架管理 |
| **纠纷管理** | 纠纷列表、处理、备注，支持按类型/状态筛选 |

### 订单状态机

```
PENDING_PAY ──支付──▶ PAID ──出租者发货──▶ TO_SHIP ──确认──▶ TO_RETURN
                                                               │
     CANCELED ◀──取消（仅待支付）                               │
                                                   租用者申请归还 ▼
                                              ┌──── RETURNED（未逾期）
                                              │
                                              └──── OVERDUE（已逾期）
                                                       │
                           FINISHED ◀──出租者确认归还────
```

## 技术栈

| 层 | 技术 | 版本 | 说明 |
|----|------|------|------|
| **后端框架** | Spring Boot | 3.2.4 | |
| **微服务** | Spring Cloud Alibaba | 2023.0.1.0 | |
| **网关** | Spring Cloud Gateway | — | WebFlux 响应式 |
| **注册 / 配置中心** | Nacos | 2.2+ | 配置热更新 |
| **流控 / 熔断** | Sentinel | 1.8.x | Gateway 限流 + Feign 降级 |
| **消息队列** | RabbitMQ | 3.x | Topic Exchange + Outbox 模式 |
| **服务调用** | OpenFeign | — | 声明式 HTTP + Fallback |
| **ORM** | MyBatis-Plus | 3.5.5 | 自动分页 / 逻辑删除 |
| **数据库** | MySQL | 8.0+ | InnoDB，utf8mb4 |
| **缓存** | Redis (Lettuce) | 7.x | 多级 TTL + 防重提交 + 原子库存 |
| **连接池** | Druid | 1.2.20 | SQL 监控 + 防火墙 |
| **对象存储** | MinIO | 8.5.x | S3 兼容，公开读 |
| **API 文档** | Knife4j (OpenAPI 3) | 4.4.0 | Gateway 聚合 |
| **前端框架** | Vue 3 (Composition API) | 3.3.4 | `<script setup>` |
| **UI** | Element Plus | 2.4.0 | 中文化 |
| **构建** | Vite | 4.4.9 | 代码分割优化 |
| **状态管理** | Pinia | 2.1.6 | |
| **HTTP** | Axios | 1.5.0 | 拦截器 + Token 刷新 |
| **图表** | ECharts | 5.x | 管理后台数据可视化 |

## 项目结构

```
Camera-rental/
├── service/                              # ===== 后端微服务 =====
│   ├── pom.xml                           # Maven 父 POM（统一依赖版本）
│   ├── docker-compose.yml                # Docker 编排（一键部署）
│   ├── camera-rental-common/             # 公共模块
│   │   └── src/main/java/.../common/
│   │       ├── annotation/               #   @AdminOnly, @OperationLog, @RepeatSubmit
│   │       ├── aspect/                   #   操作日志切面, 防重提交切面
│   │       ├── config/                   #   Redis, MinIO, MybatisPlus, OpenAPI, WebMvc
│   │       ├── enums/                    #   ErrorCode, OrderStatus, DepositStatus, DisputeType...
│   │       ├── exception/                #   BusinessException, GlobalExceptionHandler
│   │       ├── interceptor/              #   AdminInterceptor
│   │       ├── mq/                       #   RabbitMQ 常量与路由
│   │       ├── result/                   #   Result<T> 统一响应
│   │       ├── service/                  #   MinioService
│   │       └── util/                     #   BusinessUtils, ValidationUtils
│   ├── camera-rental-gateway/            # API 网关
│   │   └── src/main/java/.../gateway/
│   │       ├── filter/                   #   JwtAuthFilter（全局 JWT 鉴权）
│   │       └── config/                   #   WebFluxConfig
│   ├── camera-rental-user-service/       # 用户服务
│   │   └── src/main/java/.../user/
│   │       ├── controller/               #   UserController, MessageController, ChatController
│   │       ├── service/                  #   UserService, MessageService, ChatService
│   │       ├── config/                   #   JwtService, SecurityConfig, WebSocketConfig
│   │       ├── entity/                   #   User, Message, ChatConversation, ChatMessage
│   │       └── dto/                      #   LoginRequest/Response, RegisterRequest...
│   ├── camera-rental-device-service/     # 设备服务
│   │   └── src/main/java/.../device/
│   │       ├── controller/               #   DeviceController, FavoriteController, UploadController
│   │       ├── service/                  #   DeviceService, FavoriteService
│   │       ├── mq/                       #   RabbitMQ 消费者
│   │       └── entity/                   #   Device, DeviceFavorite
│   ├── camera-rental-order-service/      # 订单服务
│   │   └── src/main/java/.../order/
│   │       ├── controller/               #   OrderController, EvaluationController, DisputeController
│   │       ├── service/                  #   OrderService, EvaluationService, DisputeService
│   │       ├── feign/                    #   DeviceFeignClient, CouponFeignClient, MessageFeignClient
│   │       ├── fallback/                 #   DeviceFeignClientFallback, MessageFeignClientFallback
│   │       ├── job/                      #   OrderTimeoutJob, OutboxPublishJob, CouponExpireJob
│   │       ├── entity/                   #   Order, Evaluation, Dispute, MqOutbox
│   │       └── mapper/                   #   OrderMapper, EvaluationMapper, DisputeMapper, MqOutboxMapper
│   └── camera-rental-coupon-service/     # 优惠券服务
│       └── src/main/java/.../coupon/
│           ├── controller/               #   CouponController
│           ├── service/                  #   CouponService（Redis 库存防超发）
│           └── mq/                       #   RabbitMQ 消费者
│
├── frontend/                             # ===== Vue 3 前端 =====
│   ├── src/
│   │   ├── api/                          # API 接口层
│   │   │   ├── user.js                   #   用户：注册/登录/资料/管理
│   │   │   ├── device.js                 #   设备：CRUD/搜索/上下架
│   │   │   ├── order.js                  #   订单：创建/支付/发货/归还
│   │   │   ├── coupon.js                 #   优惠券：领取/查询/管理
│   │   │   ├── evaluation.js             #   评价：创建/查询/删除
│   │   │   ├── favorite.js               #   收藏：添加/取消/列表
│   │   │   ├── message.js                #   消息：列表/已读/删除
│   │   │   ├── chat.js                   #   IM 聊天：会话/消息/未读
│   │   │   ├── dispute.js                #   纠纷：创建/查询/管理
│   │   │   └── upload.js                 #   图片上传
│   │   ├── views/
│   │   │   ├── Home.vue                  # 首页
│   │   │   ├── auth/                     #   Login, Register
│   │   │   ├── device/                   #   DeviceList, DeviceDetail, DevicePublish, DeviceEdit, MyDevices
│   │   │   ├── order/                    #   OrderList, OrderDetail, OrderCreate, OrderEvaluate, LessorOrderList
│   │   │   ├── coupon/                   #   CouponList, CouponCenter
│   │   │   ├── chat/                     #   ChatList, ChatRoom
│   │   │   ├── user/                     #   UserProfile, ProfileEdit, ChangePassword, MessageCenter, FavoriteList
│   │   │   ├── admin/                    #   Dashboard, DeviceAudit, UserManage, CouponManage, AdminOrderList, DisputeManage
│   │   │   └── NotFound.vue
│   │   ├── layouts/
│   │   │   ├── MainLayout.vue            # 前台布局（导航 + 页脚）
│   │   │   └── AdminLayout.vue           # 管理后台布局（侧边栏 + 顶栏）
│   │   ├── router/index.js               # 路由配置 + 导航守卫
│   │   ├── stores/user.js                # Pinia 用户状态
│   │   ├── utils/
│   │   │   ├── request.js                # Axios 封装（拦截器/Token刷新/401处理）
│   │   │   ├── storage.js                # localStorage / sessionStorage 工具
│   │   │   ├── validate.js               # 表单验证规则
│   │   │   ├── date.js                   # 日期格式化
│   │   │   ├── format.js                 # 数据格式化
│   │   │   ├── common.js                 # 通用工具
│   │   │   ├── eventBus.js               # 事件总线
│   │   │   └── message.js                # 消息提示封装
│   │   ├── constants/index.js            # 业务常量（状态枚举映射/品牌列表）
│   │   └── components/
│   │       └── ImageUpload.vue           # 图片上传组件（拖拽 + 重试）
│   ├── index.html
│   ├── vite.config.js                    # Vite 配置（代理 + 代码分割）
│   └── package.json
│
├── nacos_config/                         # ===== Nacos 配置文件 =====
│   ├── DEFAULT_GROUP/                    # 各服务独立配置
│   │   ├── camera-rental-gateway.yaml
│   │   ├── camera-rental-user-service.yaml
│   │   ├── camera-rental-device-service.yaml
│   │   ├── camera-rental-order-service.yaml
│   │   └── camera-rental-coupon-service.yaml
│   └── SHARED_GROUP/                     # 共享配置
│       ├── common-config.yaml            #   日志、Jackson、Actuator
│       ├── datasource-config.yaml        #   MySQL + Druid 连接池
│       ├── redis-config.yaml             #   Redis 连接
│       ├── rabbitmq-config.yaml          #   RabbitMQ 连接
│       ├── minio-config.yaml             #   MinIO 连接
│       ├── secret-config.yaml.example    #   敏感配置模板（密码/密钥）
│       └── seata-config.yaml             #   Seata 配置（预留）
│
├── sql/                                  # ===== 数据库脚本 =====
│   ├── schema.sql                        # 建表语句（15 张表）
│   └── data.sql                          # 初始数据
│
├── docs/                                 # ===== 项目文档 =====
│   ├── guides/                           # 配置指南
│   ├── learn/                            # 学习/简历说明
│   └── archived/                         # 归档文档
│
├── start-all-middleware.bat              # Windows 一键启动中间件
├── stop-middleware.bat                   # Windows 一键停止中间件
├── CONTRIBUTING.md                       # 贡献指南
└── LICENSE                               # MIT 协议
```

## 快速开始

### 环境要求

| 依赖 | 版本 | 必需 |
|------|------|------|
| JDK | 17+ | ✅ |
| Maven | 3.8+ | ✅ |
| Node.js | 18+ | ✅ |
| MySQL | 8.0+ | ✅ |
| Redis | 7.0+ | ✅ |
| Nacos | 2.2+ | ✅ |
| RabbitMQ | 3.x | ✅（异步消息） |
| MinIO | — | 可选（不配则图片上传不可用） |
| Sentinel Dashboard | 1.8.6 | 可选（不配则无流控面板） |

### 1. 克隆项目

```bash
git clone https://github.com/your-username/Camera-rental.git
cd Camera-rental
```

### 2. 初始化数据库

```bash
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/data.sql
```

`schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`，可安全重复执行，自动跳过已存在表。

### 3. 启动中间件

**方式一：Windows 一键脚本**

```bash
start-all-middleware.bat
```

**方式二：手动启动**

```bash
# MySQL、Redis、RabbitMQ（使用系统安装版本即可）

# Nacos（单机模式）
cd nacos/bin && startup.cmd -m standalone

# Sentinel Dashboard（可选）
java -Dserver.port=8858 -jar sentinel-dashboard.jar

# MinIO（可选）
minio server /data --console-address ":9001"
```

### 4. 配置 Nacos

1. 访问 Nacos 控制台 http://localhost:8848/nacos （默认 nacos/nacos）
2. 创建命名空间（ID 与 `bootstrap.yml` 中 `NACOS_NAMESPACE` 一致）
3. 导入 `nacos_config/` 下所有 `.yaml` 文件，注意分组：
   - `DEFAULT_GROUP`：各服务独立配置
   - `SHARED_GROUP`：共享配置
4. **重要**：复制 `secret-config.yaml.example` → `secret-config.yaml`，填入实际密码后导入

### 5. 启动后端

```bash
cd service
mvn clean install -DskipTests
```

在 IDE 中按顺序启动（或直接运行各模块的 `Application.java`）：

1. `GatewayApplication` (8080)
2. `UserServiceApplication` (8081)
3. `DeviceServiceApplication` (8082)
4. `OrderServiceApplication` (8083)
5. `CouponServiceApplication` (8084)

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 7. 访问服务

| 服务 | 地址 |
|------|------|
| 前端首页 | http://localhost:3000 |
| 管理后台 | http://localhost:3000/admin/dashboard |
| API 文档 (Knife4j) | http://localhost:8080/doc.html（仅内网/IP 白名单可访问） |
| Nacos 控制台 | http://localhost:8848/nacos |
| Sentinel 控制台 | http://localhost:8858 |
| Druid 监控 (user-service) | 默认关闭，仅维护开窗期可访问 |

> **管理后台**：使用 `role = ADMIN` 的账号登录前端后，点击头像下拉菜单 → "管理后台"

## 环境配置

所有敏感配置通过 Nacos `secret-config.yaml` 集中管理，**不入 Git**。

需要配置的环境变量（或直接在 secret-config.yaml 中填写）：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DATASOURCE_URL` | MySQL 连接地址 | `jdbc:mysql://localhost:3306/camera_rental...` |
| `DATASOURCE_USERNAME` | 数据库用户名 | `root` |
| `DATASOURCE_PASSWORD` | 数据库密码 | — |
| `JWT_SECRET` | JWT 签名密钥（>=32 字符） | — |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PASSWORD` | Redis 密码 | 空 |
| `RABBITMQ_HOST` | RabbitMQ 地址 | `localhost` |
| `RABBITMQ_USERNAME` | RabbitMQ 用户名 | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | `guest` |
| `MINIO_ENDPOINT` | MinIO 地址 | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO Access Key | — |
| `MINIO_SECRET_KEY` | MinIO Secret Key | — |
| `DRUID_PASSWORD` | Druid 监控密码（仅开窗期生效） | — |

## 网关安全基线

- 公开接口最小化：`POST /user/register`、`POST /user/login`、`POST /user/refresh-token`、`GET /device/search`、`GET /device/{id}`、`GET /actuator/health`。
- 管理面（文档、Druid、`/actuator/info`、`/actuator/metrics`、`/admin/**`）仅允许内网/IP 白名单访问。
- Druid 默认关闭，通过 Nacos 临时开窗后访问，维护完成需立即回收。
- 过期 AccessToken 返回：`{"code":2005,"message":"TOKEN_EXPIRED","data":null}`，用于前端触发刷新流程。
- 详细 SOP 见：[`docs/guides/网关安全与运维开窗指南.md`](docs/guides/网关安全与运维开窗指南.md)

## Docker 部署

```bash
cd service

# 一键启动所有基础设施 + 微服务
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f gateway
```

Docker Compose 包含：MySQL 8.0、Redis 7、Nacos、RabbitMQ、Sentinel Dashboard、Gateway、四个业务服务。

## 数据库设计

共 **15 张表**，MySQL InnoDB，utf8mb4 编码：

| 分类 | 表 | 说明 |
|------|----|------|
| **核心业务** | `user` | 用户（phone UNIQUE, role, status, avatar, address） |
| | `device` | 设备（brand, rentPrice, auditStatus, 逻辑删除） |
| | `order` | 订单（orderNo UNIQUE, 8 种状态, 3 种押金状态） |
| | `coupon` | 优惠券（denomination, threshold, totalCount, 4 种状态） |
| | `user_coupon` | 领取记录（user+coupon UNIQUE, expireTime） |
| | `evaluation` | 评价（score, content, imagePaths, reply） |
| | `message` | 站内消息（type: 4 种, readStatus） |
| | `device_favorite` | 收藏（user+device UNIQUE） |
| **IM 聊天** | `chat_conversation` | 会话（user_id1+user_id2 UNIQUE, 未读计数） |
| | `chat_message` | 聊天消息（conversation_id, is_read） |
| **纠纷** | `dispute` | 纠纷（order_id, dispute_type, status, admin 处理） |
| **消息队列** | `mq_outbox` | Outbox 发件箱（event_id UNIQUE, 状态+重试） |
| | `mq_consume_log` | 消费幂等日志（event_id+consumer UNIQUE） |
| **运维** | `operation_log` | 操作日志（type, request/response, 耗时） |

> 完整 DDL 见 [`sql/schema.sql`](sql/schema.sql)

## 跨服务通信

- **同步调用**：OpenFeign 声明式客户端，order-service → device-service / coupon-service / user-service
- **异步消息**：RabbitMQ Topic Exchange `camera.rental.order.topic.v1`
  - 生产者：Order Service 通过 Outbox 模式发布（`mq_outbox` 表 + `OutboxPublishJob` 定时任务）
  - 消费者：user-service（`user.notify.q`）、device-service（`device.sync.q`）、coupon-service（`coupon.sync.q`）
  - 每个消费者配置死信队列（DLQ）处理失败消息
  - 消费幂等通过 `mq_consume_log` 表保障

## API 文档

启动 Gateway 后访问 http://localhost:8080/doc.html，Knife4j 聚合展示所有服务的 OpenAPI 文档（仅内网/IP 白名单可访问）。

支持在线调试，需在 Authorize 中填入 `Bearer <token>`。

## 项目文档

| 文档 | 说明 |
|------|------|
| [`docs/guides/中间件配置指南.md`](docs/guides/中间件配置指南.md) | Nacos / Redis / MinIO / Sentinel / RabbitMQ 关键配置 |
| [`docs/guides/网关安全与运维开窗指南.md`](docs/guides/网关安全与运维开窗指南.md) | 网关白名单、Actuator 分级、Druid 临时开窗 SOP |
| [`docs/guides/单元测试快速开始指南.md`](docs/guides/单元测试快速开始指南.md) | 单元测试编写指南 |
| [`docs/guides/MinIO图片上传配置指南.md`](docs/guides/MinIO图片上传配置指南.md) | MinIO 配置与图片上传说明 |
| [`docs/guides/配置管理规范-本地vs-Nacos分离方案.md`](docs/guides/配置管理规范-本地vs-Nacos分离方案.md) | 配置分层管理规范 |
| [`docs/learn/Camera Rental 项目简历说明.md`](docs/learn/Camera%20Rental%20项目简历说明.md) | 简历/面试参考 |
| [`docs/learn/安全设计说明.md`](docs/learn/安全设计说明.md) | JWT 鉴权、权限控制、数据安全 |
| [`docs/生产部署检查清单.md`](docs/生产部署检查清单.md) | 上线前检查项 |
| [`service/README.md`](service/README.md) | 后端微服务开发指南 |
| [`frontend/README.md`](frontend/README.md) | 前端开发指南 |

## 贡献指南

欢迎 Issue 和 Pull Request！请参阅 [CONTRIBUTING.md](CONTRIBUTING.md)。

提交规范：

```
feat: 添加设备搜索功能
fix: 修复订单超时取消异常
docs: 更新 API 文档
refactor: 重构优惠券领取逻辑
chore: 升级依赖版本
```

## 开源协议

[MIT License](LICENSE) © 2026 LensGo
