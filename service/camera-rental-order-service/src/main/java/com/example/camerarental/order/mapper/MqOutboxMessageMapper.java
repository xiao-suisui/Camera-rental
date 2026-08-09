package com.example.camerarental.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.camerarental.order.entity.MqOutboxMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MqOutboxMessageMapper extends BaseMapper<MqOutboxMessage> {
}

