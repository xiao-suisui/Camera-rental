package com.example.camerarental.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.camerarental.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * Conditional status update: only updates if current status matches expectedStatus.
     * Returns number of affected rows (0 = status already changed by another request).
     */
    @Update("UPDATE `order` SET status = #{newStatus}, updated_at = NOW() " +
            "WHERE id = #{orderId} AND status = #{expectedStatus}")
    int updateStatusIf(@Param("orderId") Long orderId,
                       @Param("expectedStatus") String expectedStatus,
                       @Param("newStatus") String newStatus);

    /**
     * Conditional status + depositStatus update.
     */
    @Update("UPDATE `order` SET status = #{newStatus}, deposit_status = #{newDepositStatus}, " +
            "updated_at = NOW() WHERE id = #{orderId} AND status = #{expectedStatus}")
    int updateStatusAndDepositIf(@Param("orderId") Long orderId,
                                 @Param("expectedStatus") String expectedStatus,
                                 @Param("newStatus") String newStatus,
                                 @Param("newDepositStatus") String newDepositStatus);
}

