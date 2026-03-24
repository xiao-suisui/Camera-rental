package com.example.camerarental.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.camerarental.device.entity.Device;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    @Insert("<script>" +
            "INSERT INTO device (user_id, title, brand, model, description, image_paths, rent_price, deposit, address, audit_status, status, batch_id, deleted, created_at, updated_at) VALUES " +
            "<foreach collection='devices' item='item' separator=','>" +
            "(#{item.userId}, #{item.title}, #{item.brand}, #{item.model}, #{item.description}, #{item.imagePaths}, #{item.rentPrice}, #{item.deposit}, #{item.address}, #{item.auditStatus}, #{item.status}, #{item.batchId}, #{item.deleted}, #{item.createdAt}, #{item.updatedAt})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("devices") java.util.List<Device> devices);

    @Select("<script>" +
            "SELECT * FROM device " +
            "WHERE deleted = 0 AND audit_status = 'APPROVED' AND status = 'ACTIVE' " +
            "<if test='keyword != null and keyword != &quot;&quot;'>" +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR model LIKE CONCAT('%', #{keyword}, '%') OR brand LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "<if test='brand != null and brand != &quot;&quot;'>" +
            "AND brand = #{brand}" +
            "</if>" +
            "<if test='minPrice != null'>" +
            "AND rent_price &gt;= #{minPrice}" +
            "</if>" +
            "<if test='maxPrice != null'>" +
            "AND rent_price &lt;= #{maxPrice}" +
            "</if>" +
            "</script>")
    IPage<Device> searchDevices(Page<?> page,
                                @Param("keyword") String keyword,
                                @Param("brand") String brand,
                                @Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice);
}

