# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Camera Rental (LensGo) — a Spring Cloud microservices platform for camera equipment rental. Java 17 backend with Vue 3 frontend.

## Build & Run

### Backend (Maven multi-module)

```bash
cd service

# Full build (skip tests)
mvn clean install -DskipTests

# Run a single test class
cd camera-rental-user-service
mvn test -Dtest=UserServiceTest

# Run all tests
mvn test

# Test reports: target/surefire-reports/
# Coverage (if JaCoCo configured): mvn clean test jacoco:report → target/site/jacoco/index.html
```

### Frontend (Vite)

```bash
cd frontend
npm install
npm run dev        # Dev server on port 3000
npm run build      # Production build → dist/
```

### Middleware (Windows)

```bash
# Start MySQL, Redis, Nacos, MinIO, Sentinel, RabbitMQ
./start-all-middleware.bat
./stop-middleware.bat

# Verify middleware connectivity (PowerShell)
./service/scripts/check-middleware.ps1
./service/scripts/check-middleware.ps1 -Strict   # CI mode

# Verify RabbitMQ topology
./service/scripts/check-mq-topology.ps1
./service/scripts/check-mq-topology.ps1 -DryRun  # Show expected without API calls
```

### Docker Compose (alternative)

```bash
cd service
docker-compose up -d    # Starts all 10 services (MySQL, Redis, Nacos, etc. + 4 app services + Nginx)
```

## Architecture

### Microservices (Maven modules under `service/`)

| Module | Port | Responsibility |
|--------|------|----------------|
| `camera-rental-common` | — | Shared library: entities, DTOs, exceptions, utils, Feign clients, MQ APIs, AOP |
| `camera-rental-gateway` | 8080 | Spring Cloud Gateway: JWT auth, routing, CORS, Sentinel rate limiting |
| `camera-rental-user-service` | 8081 | Auth (register/login/JWT), profile, admin user mgmt, in-app messaging |
| `camera-rental-device-service` | 8082 | Device CRUD, search, MinIO image upload, favorites, admin audit |
| `camera-rental-order-service` | 8083 | Order lifecycle, evaluations, Outbox pattern, timeout job |
| `camera-rental-coupon-service` | 8084 | Coupon CRUD, Redis atomic stock claim, user coupon mgmt |

### Cross-Service Communication

- **Synchronous**: OpenFeign clients defined in `common` module, called by order-service → device-service and order-service → coupon-service
- **Asynchronous**: RabbitMQ topic exchange `camera.rental.order.topic.v1`
  - Order Service publishes via Outbox pattern (`mq_outbox` table + `OutboxPublishJob` scheduled task)
  - Consumers: user-service (`user.notify.q`), device-service (`device.sync.q`), coupon-service (`coupon.sync.q`)
  - Each consumer has a DLQ for failed messages
  - Idempotency via `mq_consume_log` table

### Configuration Layers (bottom → top)

1. `bootstrap.yml` per service → Nacos server address, namespace `b87db654-8323-4c9f-ba55-66ce6d0622be`
2. `application-dev.yml` / `application-prod.yml` → profile overrides
3. Nacos `SHARED_GROUP` → common-config, datasource, redis, rabbitmq, minio, secrets
4. Nacos `DEFAULT_GROUP` per service → full service config (port, Sentinel, JWT, logging)

Secrets reference env vars (`${CAMERA_RENTAL_DB_PASSWORD}`) in prod; `secret-config-dev.yaml` has hardcoded dev values.

### Security Model

- Gateway `JwtAuthFilter` validates JWT, forwards `X-User-Id` / `X-User-Role` headers downstream
- Admin endpoints: `@AdminOnly` annotation + `AdminInterceptor` checks `X-User-Role` header
- Dual-token: accessToken + refreshToken; expired tokens return code `2005` for silent refresh
- Public endpoints: `/user/register`, `/user/login`, `/user/refresh-token`, `/device/search`, `/device/{id}`, `/actuator/health`

### Order State Machine

`PENDING_PAY → PAID → TO_SHIP → TO_RETURN → RETURNED/OVERDUE → FINISHED` (with `CANCELED` from `PENDING_PAY` only)

### Database

14 tables in `sql/schema.sql`, seed data in `sql/data.sql`. Key tables: `user`, `device`, `order`, `coupon`, `user_coupon`, `evaluation`, `message`, `device_favorite`, `mq_outbox`, `mq_consume_log`, `operation_log`, `chat_conversation`, `chat_message`, `dispute`.

## Code Conventions

### Backend

- All business exceptions use `BusinessException(ErrorCode.*)` — error codes defined in `common` module
- Controllers add Swagger/Knife4j annotations (`@Operation`, `@Parameter`)
- MyBatis-Plus for data access; mappers extend `BaseMapper<T>`
- Entities use Lombok `@Data` / `@Builder`
- Admin-only endpoints require `@AdminOnly` annotation

### Frontend

- Vue 3 Composition API with `<script setup>` syntax
- Element Plus component library; styles use `scoped`
- API calls in `src/api/` modules; Axios instance in `src/utils/request.js` handles token refresh
- State management: Pinia store in `src/stores/user.js`
- Constants for status enums in `src/constants/index.js`

### Commits

Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`

### Branches

`feature/xxx`, `fix/xxx`, `docs/xxx`, `refactor/xxx`

## Key File Locations

- Gateway route config: `nacos_config/DEFAULT_GROUP/camera-rental-gateway.yaml`
- Shared Nacos configs: `nacos_config/SHARED_GROUP/`
- Feign clients: `service/camera-rental-common/src/main/java/.../feign/`
- Error codes: `service/camera-rental-common/src/main/java/.../exception/ErrorCode.java`
- MQ routing: `service/camera-rental-common/src/main/java/.../mq/`
- Frontend routes & guards: `frontend/src/router/index.js`
