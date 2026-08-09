package com.example.camerarental.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxMetricsResponse {

    /** 待投递（NEW）消息数 */
    private Long pendingCount;

    /** 投递失败（FAILED）消息数 */
    private Long failedCount;

    /** 累计重试次数（SUM(retry_count)） */
    private Long retryCount;
}
