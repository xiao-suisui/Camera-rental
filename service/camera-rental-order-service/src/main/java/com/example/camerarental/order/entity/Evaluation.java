package com.example.camerarental.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("evaluation")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long evaluatorId; // 评价人ID

    private Long evaluatedId; // 被评价人ID

    private Long deviceId;

    private Integer score; // 评分 1-5星

    private String content; // 评价内容

    private String imagePaths; // 评价图片（逗号分隔）

    private String reply; // 商家回复

    private LocalDateTime repliedAt;

    private Long repliedBy;

    private LocalDateTime createdAt;
}

