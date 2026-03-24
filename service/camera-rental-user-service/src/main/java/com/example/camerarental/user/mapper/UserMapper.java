package com.example.camerarental.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.camerarental.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

