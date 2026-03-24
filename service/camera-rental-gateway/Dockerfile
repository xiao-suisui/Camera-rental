# ============================================
# Camera Rental Gateway — Production Dockerfile
# ============================================
# 多阶段构建：仅复制 jar，不包含源码和构建工具

FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="camera-rental-team"
LABEL description="Camera Rental API Gateway"

# 安全：创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 复制构建产物
COPY target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /var/logs/camera-rental && chown -R appuser:appgroup /var/logs/camera-rental

# 切换到非 root 用户
USER appuser

EXPOSE 8080

# JVM 生产调优参数
ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/logs/camera-rental/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai"

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
