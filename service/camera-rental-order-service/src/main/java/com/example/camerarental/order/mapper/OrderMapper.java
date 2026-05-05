package com.example.camerarental.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.camerarental.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

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

    /**
     * 查询最近 N 天每天的订单数量
     */
    @Select("SELECT DATE(created_at) AS date, COUNT(*) AS count FROM `order` " +
            "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> countOrdersByDay(@Param("days") int days);
}

