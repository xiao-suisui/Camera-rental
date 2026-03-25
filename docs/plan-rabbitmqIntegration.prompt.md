## Plan: 引入 RabbitMQ 实现异步消息投递

### 现状分析

当前 `order-service` 通过 `MessageFeignClient` **同步** HTTP 调用 `user-service` 的 `POST /message/send` 接口发送站内信。
该调用出现在 `OrderService.java` 的 3 个位置：

| 方法 | 行号 | 通知对象 | 通知内容 |
|------|------|---------|---------|
| `ship()` | ~132 | 租用者 (userId) | 订单已发货 + 物流单号 |
| `applyReturn()` | ~166 | 出租者 (lessorId) | 租用者已申请归还 |
| `confirmReturn()` | ~195 | 租用者 (userId) | 订单已确认归还，押金已退还 |

每处调用都包裹在 `try-catch` 中做静默降级，存在以下问题：

1. **阻塞主流程** — Feign 同步调用（默认 read-timeout 10s）阻塞订单状态变更的响应
2. **消息丢失** — user-service 不可用时消息直接丢失，无重试
3. **强耦合** — order-service 强依赖 user-service 的 `/message/send` HTTP 接口
4. **无 Fallback** — `MessageFeignClient` 未配置 fallback 类（仅在 docs 中提及计划）
5. **扩展困难** — 新增通知场景需要在每个生产者服务重复写 Feign 调用

引入 RabbitMQ 后，生产者只需投递消息到 Exchange，由 `user-service` 统一消费入库，实现 **解耦、削峰、可靠投递**。

### Steps

#### Step 1 — 添加 RabbitMQ 基础设施

**docker-compose.yml** — 在 `minio` 服务之后、`frontend` 之前插入 RabbitMQ 服务：

修改文件：[service/docker-compose.yml](../service/docker-compose.yml)

```yaml
rabbitmq:
  image: rabbitmq:3.12-management
  container_name: camera-rental-rabbitmq
  restart: unless-stopped
  environment:
    RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-guest}
    RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-guest}
    RABBITMQ_DEFAULT_VHOST: /camera-rental
    TZ: Asia/Shanghai
  ports:
    - "${RABBITMQ_PORT:-5672}:5672"
    - "${RABBITMQ_MGMT_PORT:-15672}:15672"
  volumes:
    - rabbitmq-data:/var/lib/rabbitmq
  healthcheck:
    test: ["CMD", "rabbitmq-diagnostics", "check_port_connectivity"]
    interval: 15s
    timeout: 10s
    retries: 5
  networks:
    - camera-rental-network
```

同时在 `volumes` 区域追加 `rabbitmq-data:`。

在 `order-service` 和 `user-service` 的 `depends_on` 中追加：
```yaml
rabbitmq:
  condition: service_healthy
```

在 `order-service` 和 `user-service` 的 `environment` 中追加：
```yaml
- RABBITMQ_HOST=rabbitmq
- RABBITMQ_PORT=5672
- RABBITMQ_USER=${RABBITMQ_USER:-guest}
- RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest}
```

**start-all-middleware.bat** — 已包含 RabbitMQ 启动步骤（第 87-102 行），无需改动。

#### Step 2 — 新增 Nacos 共享配置 `rabbitmq-config.yaml`

新建文件：[nacos_config/SHARED_GROUP/rabbitmq-config.yaml](../nacos_config/SHARED_GROUP/rabbitmq-config.yaml)

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: ${RABBITMQ_VHOST:/camera-rental}
    # ===== 生产者可靠投递 =====
    publisher-confirm-type: correlated   # 异步确认模式
    publisher-returns: true              # 路由失败回调
    template:
      mandatory: true                    # 消息无法路由时触发 returnsCallback
    # ===== 消费者可靠消费 =====
    listener:
      simple:
        acknowledge-mode: manual         # 手动 ACK
        prefetch: 10                     # 每次预取 10 条
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000         # 首次重试间隔 1s
          multiplier: 2                  # 指数退避系数
          max-interval: 10000            # 最大间隔 10s
```

在 **order-service** 的 `bootstrap.yml` 的 `shared-configs` 列表末尾追加：

修改文件：[service/camera-rental-order-service/src/main/resources/bootstrap.yml](../service/camera-rental-order-service/src/main/resources/bootstrap.yml)

```yaml
          - data-id: rabbitmq-config.yaml
            group: SHARED_GROUP
            refresh: true
```

在 **user-service** 的 `bootstrap.yml` 的 `shared-configs` 列表末尾追加同样配置：

修改文件：[service/camera-rental-user-service/src/main/resources/bootstrap.yml](../service/camera-rental-user-service/src/main/resources/bootstrap.yml)

```yaml
          - data-id: rabbitmq-config.yaml
            group: SHARED_GROUP
            refresh: true
```

> **不要**在 device-service / coupon-service / gateway 中引入——它们暂不需要 MQ。

#### Step 3 — 在 `common` 模块添加 AMQP 可选依赖和公共常量

修改文件：[service/camera-rental-common/pom.xml](../service/camera-rental-common/pom.xml)

在 `<!-- ===== AOP ===== -->` 之前插入：

```xml
<!-- ===== 消息队列（可选，按需引入） ===== -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
    <optional>true</optional>
</dependency>
```

`<optional>true</optional>` 确保只有显式声明 AMQP 的子模块才会真正引入运行时依赖。

新建文件：`service/camera-rental-common/src/main/java/com/example/camerarental/common/mq/MqConstants.java`

```java
package com.example.camerarental.common.mq;

/**
 * RabbitMQ 交换机 / 队列 / 路由键 常量
 * 生产者和消费者共享，保持命名一致
 */
public final class MqConstants {

    private MqConstants() {}

    // ===== 通知消息 =====
    public static final String TOPIC_EXCHANGE        = "camera.rental.topic";
    public static final String NOTIFICATION_QUEUE     = "message.notification.queue";
    public static final String NOTIFICATION_KEY       = "order.notification";

    // ===== 死信 =====
    public static final String DLX_EXCHANGE           = "camera.rental.dlx";
    public static final String DLQ_NOTIFICATION       = "message.notification.dlq";
    public static final String DLQ_NOTIFICATION_KEY   = "message.notification.dead";
}
```

新建文件：`service/camera-rental-common/src/main/java/com/example/camerarental/common/mq/NotificationMessage.java`

```java
package com.example.camerarental.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 跨服务通知消息体（统一契约）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    /** 消息唯一 ID（UUID，用于幂等判断） */
    private String messageId;

    /** 目标用户 ID */
    private Long userId;

    /** 消息内容 */
    private String content;

    /** 消息类型（ORDER_STATUS / DEVICE_AUDIT / COUPON_EXPIRE 等） */
    private String type;

    /** 投递时间戳 */
    private LocalDateTime timestamp;
}
```

#### Step 4 — 在 `order-service` 添加 AMQP 依赖并实现生产者

修改文件：[service/camera-rental-order-service/pom.xml](../service/camera-rental-order-service/pom.xml)

在 `camera-rental-common` 依赖之后插入：

```xml
<!-- 消息队列 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

新建文件：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/mq/RabbitMQConfig.java`

```java
package com.example.camerarental.order.mq;

import com.example.camerarental.common.mq.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ===== 消息转换器（JSON 序列化） =====
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ===== 死信交换机 & 队列 =====
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(MqConstants.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlqNotification() {
        return QueueBuilder.durable(MqConstants.DLQ_NOTIFICATION).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqNotification())
                .to(dlxExchange())
                .with(MqConstants.DLQ_NOTIFICATION_KEY);
    }

    // ===== 业务交换机 & 队列 =====
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(MqConstants.TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(MqConstants.NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", MqConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.DLQ_NOTIFICATION_KEY)
                .build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(topicExchange())
                .with(MqConstants.NOTIFICATION_KEY);
    }
}
```

新建文件：`service/camera-rental-order-service/src/main/java/com/example/camerarental/order/mq/MessageProducer.java`

```java
package com.example.camerarental.order.mq;

import com.example.camerarental.common.mq.MqConstants;
import com.example.camerarental.common.mq.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 投递通知消息到 RabbitMQ
     */
    public void sendNotification(Long userId, String content, String type) {
        String messageId = UUID.randomUUID().toString().replace("-", "");

        NotificationMessage msg = NotificationMessage.builder()
                .messageId(messageId)
                .userId(userId)
                .content(content)
                .type(type)
                .timestamp(LocalDateTime.now())
                .build();

        CorrelationData correlationData = new CorrelationData(messageId);
        correlationData.getFuture().whenComplete((confirm, throwable) -> {
            if (throwable != null) {
                log.error("[MQ] 消息投递异常 messageId={}", messageId, throwable);
            } else if (confirm != null && !confirm.isAck()) {
                log.error("[MQ] 消息被拒绝 messageId={}, reason={}", messageId, confirm.getReason());
            } else {
                log.debug("[MQ] 消息投递成功 messageId={}", messageId);
            }
        });

        rabbitTemplate.convertAndSend(
                MqConstants.TOPIC_EXCHANGE,
                MqConstants.NOTIFICATION_KEY,
                msg,
                correlationData
        );

        log.info("[MQ] 通知消息已投递: userId={}, type={}, messageId={}", userId, type, messageId);
    }
}
```

#### Step 5 — 重构 `OrderService` 中 3 处站内信调用

修改文件：[service/camera-rental-order-service/src/main/java/com/example/camerarental/order/service/OrderService.java](../service/camera-rental-order-service/src/main/java/com/example/camerarental/order/service/OrderService.java)

变更点：

1. **替换注入**：移除 `MessageFeignClient messageFeignClient`，改为注入 `MessageProducer messageProducer`
2. **`ship()` 方法**（~第 128-138 行）：
   ```java
   // Before:
   try {
       messageFeignClient.sendMessage(order.getUserId(), ..., "ORDER_STATUS");
   } catch (Exception e) { log.error(...); }

   // After:
   messageProducer.sendNotification(order.getUserId(), ..., "ORDER_STATUS");
   ```
3. **`applyReturn()` 方法**（~第 162-172 行）：同上模式替换
4. **`confirmReturn()` 方法**（~第 191-201 行）：同上模式替换

修改文件：[service/camera-rental-order-service/src/main/java/com/example/camerarental/order/feign/MessageFeignClient.java](../service/camera-rental-order-service/src/main/java/com/example/camerarental/order/feign/MessageFeignClient.java)

在类上添加 `@Deprecated` 注解，保留文件作为过渡参考，后续清理阶段删除。

#### Step 6 — 在 `user-service` 添加 AMQP 依赖并实现消费者

修改文件：[service/camera-rental-user-service/pom.xml](../service/camera-rental-user-service/pom.xml)

在 `spring-security-crypto` 依赖之后插入：

```xml
<!-- 消息队列 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

新建文件：`service/camera-rental-user-service/src/main/java/com/example/camerarental/user/mq/RabbitMQConfig.java`

消费端同样声明 Exchange / Queue / Binding（与生产端一致），确保消费端先于生产端启动时也能自动创建队列。同时注册 `Jackson2JsonMessageConverter`。结构与 Step 4 中的 `RabbitMQConfig` 完全相同。

新建文件：`service/camera-rental-user-service/src/main/java/com/example/camerarental/user/mq/MessageConsumer.java`

```java
package com.example.camerarental.user.mq;

import com.example.camerarental.common.mq.MqConstants;
import com.example.camerarental.common.mq.NotificationMessage;
import com.example.camerarental.user.service.MessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final MessageService messageService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String IDEMPOTENT_PREFIX = "mq:idempotent:";

    @RabbitListener(queues = MqConstants.NOTIFICATION_QUEUE)
    public void handleNotification(NotificationMessage msg,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        String messageId = msg.getMessageId();
        log.info("[MQ] 收到通知消息: messageId={}, userId={}, type={}",
                messageId, msg.getUserId(), msg.getType());

        try {
            // 幂等校验：防止重复消费
            String key = IDEMPOTENT_PREFIX + messageId;
            Boolean isNew = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "1", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isNew)) {
                log.warn("[MQ] 重复消息，跳过: messageId={}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 调用已有的 MessageService 入库
            messageService.sendMessage(msg.getUserId(), msg.getContent(), msg.getType());

            // 消费成功，手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("[MQ] 消息消费成功: messageId={}", messageId);

        } catch (Exception e) {
            log.error("[MQ] 消息消费失败: messageId={}", messageId, e);
            // 消费失败，不重回队列（交给 DLX 处理）
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

#### Step 7 — 配置死信队列（DLX）兜底

已在 Step 4 的 `RabbitMQConfig` 中完成：

- 主队列 `message.notification.queue` 绑定死信参数 `x-dead-letter-exchange` → `camera.rental.dlx`
- 死信交换机 `camera.rental.dlx`（Direct 类型）绑定死信队列 `message.notification.dlq`
- 消费失败且 Spring Retry 耗尽（3 次，指数退避 1s→2s→4s）的消息自动路由到 DLQ
- 消费端 `basicNack(requeue=false)` 确保不会无限循环

后续可在管理后台添加 DLQ 查询 / 重投接口。

### 改动文件清单

| 操作 | 文件 |
|------|------|
| 修改 | `service/docker-compose.yml` — 添加 rabbitmq 服务 + volume + depends_on |
| 新建 | `nacos_config/SHARED_GROUP/rabbitmq-config.yaml` |
| 修改 | `service/camera-rental-order-service/src/main/resources/bootstrap.yml` — shared-configs 追加 rabbitmq-config |
| 修改 | `service/camera-rental-user-service/src/main/resources/bootstrap.yml` — 同上 |
| 修改 | `service/camera-rental-common/pom.xml` — 添加 spring-boot-starter-amqp (optional) |
| 新建 | `common/src/.../common/mq/MqConstants.java` |
| 新建 | `common/src/.../common/mq/NotificationMessage.java` |
| 修改 | `service/camera-rental-order-service/pom.xml` — 添加 spring-boot-starter-amqp |
| 新建 | `order-service/src/.../order/mq/RabbitMQConfig.java` |
| 新建 | `order-service/src/.../order/mq/MessageProducer.java` |
| 修改 | `order-service/src/.../order/service/OrderService.java` — 3 处 Feign 调用改 MQ |
| 修改 | `order-service/src/.../order/feign/MessageFeignClient.java` — 添加 @Deprecated |
| 修改 | `service/camera-rental-user-service/pom.xml` — 添加 spring-boot-starter-amqp |
| 新建 | `user-service/src/.../user/mq/RabbitMQConfig.java` |
| 新建 | `user-service/src/.../user/mq/MessageConsumer.java` |

### Further Considerations

1. **为什么 common 用 `<optional>true</optional>`？** — `MqConstants` 和 `NotificationMessage` 放 common 保证生产者/消费者契约统一；但 `spring-boot-starter-amqp` 的运行时 Bean 不会被 gateway / device-service / coupon-service 加载。后续 device-service（审核通知）、coupon-service（到期提醒）需要投递消息时再显式加 AMQP 依赖即可。

2. **消息幂等性** — `NotificationMessage.messageId`（UUID）+ 消费端 Redis `SETNX` 24h 过期做去重。若数据量大可改为 message 表加 `message_id` UNIQUE INDEX，由数据库唯一约束兜底。

3. **后续可扩展场景**：
   - **订单超时自动取消** — RabbitMQ 延迟消息插件 `rabbitmq_delayed_message_exchange`（或 TTL + DLX 方案），订单创建时投递延迟消息，30 分钟后触发取消逻辑
   - **设备审核结果通知** — device-service 审核完成后投递 `device.audit.passed` / `device.audit.rejected` 到同一 Exchange
   - **优惠券即将过期提醒** — coupon-service 定时任务扫描 → 投递 `coupon.expiring` 消息
   - **多通道通知** — 同一消息绑定多个 Queue（站内信 / 邮件 / 短信 / WebSocket 推送），不同消费者独立消费

4. **监控** — RabbitMQ Management 控制台（15672）可查看队列深度、消费速率、DLQ 积压。生产环境建议接入 Prometheus + rabbitmq_prometheus 插件。

5. **架构文档更新** — 实施完成后更新 [docs/架构与性能设计.md](../docs/架构与性能设计.md) 中 §1.2「服务间通信」，将「异步通信（预留）」改为「异步通信（RabbitMQ）」并补充 Exchange / Queue 拓扑图。

