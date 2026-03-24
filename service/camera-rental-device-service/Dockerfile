# ============================================
# Camera Rental Device Service — Production Dockerfile
# ============================================

FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="camera-rental-team"
LABEL description="Camera Rental Device Service"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY target/*.jar app.jar

RUN mkdir -p /var/logs/camera-rental && chown -R appuser:appgroup /var/logs/camera-rental

USER appuser

EXPOSE 8082

ENV JAVA_OPTS="-Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/logs/camera-rental/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai"

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
