package com.example.camerarental.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.camerarental.coupon.dto.UserCouponDTO;
import com.example.camerarental.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
    
    @Select("SELECT uc.id, uc.user_id, uc.coupon_id, uc.receive_time, uc.expire_time, " +
            "uc.use_status, uc.order_id, c.denomination, c.threshold, c.type " +
            "FROM user_coupon uc " +
            "LEFT JOIN coupon c ON uc.coupon_id = c.id " +
            "WHERE uc.user_id = #{userId} " +
            "ORDER BY uc.receive_time DESC")
    List<UserCouponDTO> selectUserCouponsWithDetail(Long userId);
}

