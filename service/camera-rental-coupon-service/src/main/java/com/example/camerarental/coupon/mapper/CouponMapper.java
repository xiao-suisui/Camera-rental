package com.example.camerarental.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.camerarental.coupon.dto.CouponAvailableDTO;
import com.example.camerarental.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    @Select("SELECT c.id, c.type, c.denomination, c.threshold, c.total_count, c.used_count, " +
            "c.valid_days, c.status, c.created_at, " +
            "CASE WHEN uc.id IS NOT NULL THEN true ELSE false END AS received " +
            "FROM coupon c " +
            "LEFT JOIN user_coupon uc ON c.id = uc.coupon_id AND uc.user_id = #{userId} " +
            "WHERE c.status = 'PUBLISHED' AND c.used_count < c.total_count " +
            "ORDER BY c.created_at DESC")
    List<CouponAvailableDTO> selectAvailableWithReceived(@Param("userId") Long userId);

    @Select("SELECT c.id, c.type, c.denomination, c.threshold, c.total_count, c.used_count, " +
            "c.valid_days, c.status, c.created_at, false AS received " +
            "FROM coupon c " +
            "WHERE c.status = 'PUBLISHED' AND c.used_count < c.total_count " +
            "ORDER BY c.created_at DESC")
    List<CouponAvailableDTO> selectAvailable();
}

