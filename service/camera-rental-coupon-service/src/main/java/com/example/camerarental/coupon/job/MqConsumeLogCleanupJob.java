package com.example.camerarental.coupon.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.camerarental.coupon.entity.MqConsumeLog;
import com.example.camerarental.coupon.mapper.MqConsumeLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqConsumeLogCleanupJob {

    private final MqConsumeLogMapper mqConsumeLogMapper;

    @Value("${mq.consume-log.retention-days:180}")
    private int retentionDays;

    @Scheduled(cron = "${mq.consume-log.cleanup-cron:0 30 3 * * ?}")
    public void cleanupExpiredLogs() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusDays(Math.max(retentionDays, 1));
        int deleted = mqConsumeLogMapper.delete(new LambdaQueryWrapper<MqConsumeLog>()
                .lt(MqConsumeLog::getConsumedAt, expiredBefore));

        if (deleted > 0) {
            log.info("Cleaned mq_consume_log rows: {}, expiredBefore={}", deleted, expiredBefore);
        }
    }
}
