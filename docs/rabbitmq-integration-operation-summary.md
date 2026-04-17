# RabbitMQ 接入与依赖治理操作总结

> 更新时间：2026-04-15
> 范围：`Camera-rental` 仓库本轮改造（网关依赖治理 + 订单通知异步化 + Outbox + 消费落库幂等）

## 1. 本次改造目标

1. 将订单状态通知从同步 Feign 调用迁移为 RabbitMQ 异步事件。
2. 减少模块间不必要依赖传递，推进“按服务按需引入核心依赖”。
3. 增强消息可靠性：引入 Outbox（生产侧）与 `mq_consume_log`（消费侧幂等）。
4. 补齐运行与运维配置（Nacos、Docker Compose、日志清理任务）。

## 2. 已完成的主要工作

### 2.1 RabbitMQ 规划文档与落地方案

- 重写 `docs/plan-rabbitmqIntegration.prompt.md`，形成可执行的业务化接入计划。
- 明确了事件分级、命名规范、DLQ/重试、幂等、一致性、阶段实施路径。

### 2.2 订单通知链路：同步 Feign -> 异步 MQ

- `order-service` 的通知发送改造为消息发布（发货/确认收货/申请归还/确认归还）。
- 删除旧同步通知客户端：
  - 已删除 `service/camera-rental-order-service/src/main/java/com/example/camerarental/order/feign/MessageFeignClient.java`

### 2.3 可靠投递：Outbox 本地消息表

- 生产侧新增 Outbox 模型与发布任务：
  - `MqOutboxMessage` 实体
  - `MqOutboxMessageMapper`
  - `OutboxStatus`
  - `OutboxPublishJob`
- `OrderNotificationProducer` 改为“写 Outbox”，再由发布任务异步投递 RabbitMQ。

### 2.4 消费幂等升级：Redis -> 数据库

- `user-service` 新增：
  - `MqConsumeLog` 实体
  - `MqConsumeLogMapper`
- `OrderNotificationConsumer` 幂等改为落库唯一键（`event_id + consumer`）判断重复消费。
- 新增消费日志清理任务 `MqConsumeLogCleanupJob`，支持长期运行的表容量治理。

### 2.5 依赖治理（按服务按需）

- `camera-rental-common` 当前仍保留 Feign/LoadBalancer 依赖传递，后续可按“按服务按需”目标继续收敛。
- `order-service` 显式引入：
  - `spring-cloud-starter-openfeign`
  - `spring-cloud-starter-loadbalancer`
- `device-service`、`coupon-service` 移除未使用 `@EnableFeignClients`。
- `device-service`、`coupon-service` 已补充 `spring-boot-starter-amqp`，可独立编译通过 MQ 相关代码。
- `gateway` 保留 `spring-cloud-starter-loadbalancer`（`lb://` 路由需要）。

### 2.6 配置与基础设施

- 新增共享配置：`nacos_config/SHARED_GROUP/rabbitmq-config.yaml`
- 将 RabbitMQ 共享配置接入：
  - `service/camera-rental-order-service/src/main/resources/bootstrap.yml`
  - `service/camera-rental-user-service/src/main/resources/bootstrap.yml`
  - `service/camera-rental-device-service/src/main/resources/bootstrap.yml`
  - `service/camera-rental-coupon-service/src/main/resources/bootstrap.yml`
- 更新 `nacos_config/DEFAULT_GROUP/camera-rental-user-service.yaml`：
  - 增加消费日志参数（consumer 名称、保留天数、清理 cron）
- 更新 `service/docker-compose.yml`：
  - 新增 RabbitMQ 服务与数据卷
  - 四个业务服务均注入 RabbitMQ 连接环境变量并依赖 RabbitMQ 健康启动

## 3. 数据库变更

`sql/schema.sql` 已新增：

1. `mq_outbox`
   - 用于生产侧本地消息持久化、重试、状态追踪（`NEW/SENT/FAILED`）
2. `mq_consume_log`
   - 用于消费侧幂等去重与审计
   - 唯一索引：`(event_id, consumer)`

## 4. 新增/修改文件概览（按模块）

### 文档

- 修改：`docs/plan-rabbitmqIntegration.prompt.md`
- 新增：`docs/rabbitmq-integration-operation-summary.md`

### 公共模块 `camera-rental-common`

- 新增：`service/camera-rental-common/src/main/java/com/example/camerarental/common/mq/MqConstants.java`
- 新增：`service/camera-rental-common/src/main/java/com/example/camerarental/common/mq/OrderNotificationEvent.java`
- 修改：`service/camera-rental-common/pom.xml`

### 订单服务 `camera-rental-order-service`

- 修改：`service/camera-rental-order-service/pom.xml`
- 修改：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/service/OrderService.java`
- 新增：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/mq/OrderRabbitMqConfig.java`
- 新增：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/mq/OrderNotificationProducer.java`
- 新增：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/mq/RabbitProducerCallbackConfig.java`
- 新增：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/entity/MqOutboxMessage.java`
- 新增：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/mapper/MqOutboxMessageMapper.java`
- 新增：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/mq/OutboxStatus.java`
- 新增：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/job/OutboxPublishJob.java`
- 删除：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/feign/MessageFeignClient.java`
- 修改：`service/camera-rental-order-service/src/main/resources/bootstrap.yml`

### 用户服务 `camera-rental-user-service`

- 修改：`service/camera-rental-user-service/pom.xml`
- 修改：`service/camera-rental-user-service/src/main/java/com/example/camerarental/user/UserServiceApplication.java`
- 新增：`service/camera-rental-user-service/src/main/java/com/example/camerarental/user/mq/UserRabbitMqConfig.java`
- 修改：`service/camera-rental-user-service/src/main/java/com/example/camerarental/user/mq/OrderNotificationConsumer.java`
- 新增：`service/camera-rental-user-service/src/main/java/com/example/camerarental/user/entity/MqConsumeLog.java`
- 新增：`service/camera-rental-user-service/src/main/java/com/example/camerarental/user/mapper/MqConsumeLogMapper.java`
- 新增：`service/camera-rental-user-service/src/main/java/com/example/camerarental/user/job/MqConsumeLogCleanupJob.java`
- 修改：`service/camera-rental-user-service/src/main/resources/bootstrap.yml`

### 其他服务

- 修改：`service/camera-rental-device-service/src/main/java/com/example/camerarental/device/DeviceServiceApplication.java`
- 修改：`service/camera-rental-coupon-service/src/main/java/com/example/camerarental/coupon/CouponServiceApplication.java`

### 配置与部署

- 新增：`nacos_config/SHARED_GROUP/rabbitmq-config.yaml`
- 修改：`nacos_config/DEFAULT_GROUP/camera-rental-user-service.yaml`
- 修改：`service/docker-compose.yml`
- 修改：`sql/schema.sql`

## 5. 当前已知状态与说明

1. 代码层面已完成“通知异步化 + Outbox + DB 幂等”的主链路改造。
2. 已完成 Maven 编译验证：`mvn -pl camera-rental-coupon-service,camera-rental-device-service -am compile -DskipTests` 构建成功。
3. `device-service` 与 `coupon-service` 的 AMQP 依赖缺失问题已修复，`org.springframework.amqp.*` 相关报错已消除。

## 6. 建议的验证与上线前检查

```powershell
Set-Location "D:\Desktop\Project\JavaProject\Camera-rental\service"
mvn -pl camera-rental-order-service,camera-rental-user-service -am compile -DskipTests
```

```powershell
Set-Location "D:\Desktop\Project\JavaProject\Camera-rental\service"
docker-compose up -d rabbitmq
```

上线前重点确认：

1. `mq_outbox` 是否持续清空（避免堆积）。
2. `mq_consume_log` 唯一键是否生效（重复消息不重复入库）。
3. `camera.rental.user.notify.q.dlq` 是否有异常堆积。
4. `mq.consume-log.cleanup-cron` 与保留天数是否符合运维要求。

## 7. 下一步建议

1. 将 `mq_outbox` 发布任务改造成可观测指标（成功率、重试次数、积压量）。
2. 为 `OutboxPublishJob` 与 `OrderNotificationConsumer` 增加集成测试。
3. 根据业务扩展 `order.cancelled` 等事件，推进库存/优惠券联动异步化。

