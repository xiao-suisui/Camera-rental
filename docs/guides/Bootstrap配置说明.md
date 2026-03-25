# Bootstrap.yml 配置说明

## 📋 概述

本项目已将配置文件从 `application.yml` 迁移到 `bootstrap.yml`，以支持 Nacos 配置中心的使用。

## 🎯 配置文件层次结构

### 1. **bootstrap.yml** （优先级最高）
- **用途**：配置 Nacos 配置中心连接信息
- **加载时机**：Spring Boot 启动的最早期，在 application.yml 之前加载
- **包含内容**：
  - 应用名称
  - Nacos 服务器地址
  - Nacos 命名空间
  - 共享配置引用
  - 基础日志配置

### 2. **application-{profile}.yml** （可选）
- **用途**：本地开发环境的特定配置
- **加载时机**：根据激活的 profile 加载
- **包含内容**：
  - 本地端口配置
  - 本地数据库连接（用于覆盖 Nacos 配置）

### 3. **Nacos 配置中心** （主配置）
- **用途**：存储所有业务配置
- **优势**：
  - 集中管理
  - 动态刷新
  - 多环境隔离
  - 配置共享

## 📁 文件清单

### 各服务的 bootstrap.yml
```
service/
├── camera-rental-user-service/src/main/resources/
│   ├── bootstrap.yml          # User Service 启动配置
│   └── application-dev.yml    # User Service 本地开发配置（可选）
├── camera-rental-device-service/src/main/resources/
│   ├── bootstrap.yml          # Device Service 启动配置
│   └── application-dev.yml    # Device Service 本地开发配置（可选）
├── camera-rental-order-service/src/main/resources/
│   ├── bootstrap.yml          # Order Service 启动配置
│   └── application-dev.yml    # Order Service 本地开发配置（可选）
├── camera-rental-coupon-service/src/main/resources/
│   ├── bootstrap.yml          # Coupon Service 启动配置
│   └── application-dev.yml    # Coupon Service 本地开发配置（可选）
└── camera-rental-gateway/src/main/resources/
    ├── bootstrap.yml          # Gateway 启动配置
    └── application-dev.yml    # Gateway 本地开发配置（可选）
```

## ⚙️ Bootstrap.yml 核心配置

### 通用结构
```yaml
spring:
  application:
    name: camera-rental-xxx-service  # 服务名称
  profiles:
    active: dev                       # 激活的环境：dev/test/prod
  cloud:
    nacos:
      config:
        server-addr: localhost:8848   # Nacos 地址
        namespace: dev                 # 命名空间
        group: DEFAULT_GROUP          # 配置组
        file-extension: yaml          # 配置文件格式
        import-check:
          enabled: false               # 禁用强制导入检查
        shared-configs:                # 共享配置
          - data-id: common-config.yaml
            group: SHARED_GROUP
            refresh: true
      discovery:
        server-addr: localhost:8848   # 服务注册地址
        namespace: dev                 # 命名空间
```

## 🔧 环境变量

支持通过环境变量覆盖配置：

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `NACOS_ADDR` | localhost:8848 | Nacos 服务器地址 |
| `NACOS_NAMESPACE` | dev | Nacos 命名空间 |
| `CONFIG_GROUP` | DEFAULT_GROUP | 配置组名称 |
| `SPRING_PROFILES_ACTIVE` | dev | 激活的环境 |

### 使用示例

#### Windows PowerShell
```powershell
$env:NACOS_ADDR="192.168.1.100:8848"
$env:NACOS_NAMESPACE="prod"
java -jar camera-rental-user-service.jar
```

#### Linux/Mac
```bash
export NACOS_ADDR=192.168.1.100:8848
export NACOS_NAMESPACE=prod
java -jar camera-rental-user-service.jar
```

#### IDEA 配置
在 Run Configuration 中添加环境变量：
```
NACOS_ADDR=192.168.1.100:8848;NACOS_NAMESPACE=prod
```

## 📝 Nacos 配置中心配置清单

### 服务专用配置（DEFAULT_GROUP）
每个服务在 Nacos 中都有自己的配置文件：

| Data ID | 说明 | 示例内容 |
|---------|------|---------|
| camera-rental-user-service.yaml | User Service 配置 | 端口、数据库连接等 |
| camera-rental-device-service.yaml | Device Service 配置 | 端口、MinIO配置等 |
| camera-rental-order-service.yaml | Order Service 配置 | 端口、Seata配置等 |
| camera-rental-coupon-service.yaml | Coupon Service 配置 | 端口、Redis配置等 |
| camera-rental-gateway.yaml | Gateway 配置 | 端口、路由配置等 |

### 共享配置（SHARED_GROUP）
多个服务共用的配置：

| Data ID | 说明 | 引用服务 |
|---------|------|---------|
| common-config.yaml | 通用配置 | 所有服务 |
| datasource-config.yaml | 数据库配置 | User/Device/Order/Coupon |
| cipher-config.yaml | 加密配置 | 所有服务 |
| redis-config.yaml | Redis配置 | User/Device/Order/Coupon/Gateway |
| minio-config.yaml | MinIO配置 | Device |
| seata-config.yaml | Seata分布式事务 | Order（可选） |

## 🚀 启动流程

1. **加载 bootstrap.yml**
   - 读取应用名称
   - 连接 Nacos 配置中心

2. **从 Nacos 拉取配置**
   - 拉取服务专用配置（如 `camera-rental-user-service.yaml`）
   - 拉取共享配置（如 `datasource-config.yaml`）

3. **加载 application-{profile}.yml**（如果存在）
   - 本地配置覆盖 Nacos 配置

4. **合并配置**
   - 优先级：application-{profile}.yml > Nacos > bootstrap.yml

5. **启动服务**

## 🔄 配置刷新

### 自动刷新
Nacos 配置支持动态刷新，修改后无需重启服务。

在需要动态刷新的类上添加注解：
```java
@RefreshScope
@RestController
public class ConfigController {
    @Value("${custom.property}")
    private String property;
}
```

### 手动刷新
如果配置未自动刷新，可以：
1. 重启服务
2. 调用 `/actuator/refresh` 端点（需开启 Actuator）

## ⚠️ 注意事项

### 1. **依赖要求**
所有服务必须添加 `spring-cloud-starter-bootstrap` 依赖：
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

### 2. **命名空间隔离**
- **dev**：开发环境
- **test**：测试环境
- **prod**：生产环境

确保不同环境使用不同的命名空间。

### 3. **配置优先级**
本地配置 > Nacos 配置 > bootstrap.yml 默认值

### 4. **敏感信息**
敏感配置（如数据库密码）存储在 `cipher-config.yaml` 中，使用加密存储。

## 🛠️ 故障排查

### 问题：启动时提示 "No spring.config.import property"
**解决方案**：
1. 确保 `bootstrap.yml` 存在
2. 确保添加了 `spring-cloud-starter-bootstrap` 依赖
3. 在 bootstrap.yml 中设置 `import-check.enabled: false`

### 问题：无法连接 Nacos
**解决方案**：
1. 检查 Nacos 服务是否启动
2. 检查 `NACOS_ADDR` 环境变量
3. 检查网络连接和防火墙

### 问题：配置未生效
**解决方案**：
1. 检查 Nacos 中配置的 Data ID 和 Group 是否正确
2. 检查命名空间是否匹配
3. 查看启动日志，确认配置已加载

## 📚 相关文档

- [Nacos 配置管理规范](./配置管理规范-本地vs-Nacos分离方案.md)
- [配置分离快速参考表](./配置分离快速参考表.md)
- [微服务架构说明](../service/微服务架构说明.md)

---

**最后更新时间**：2026-02-23  
**维护人员**：Camera Rental Team

