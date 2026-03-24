package com.example.camerarental.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求基类
 */
@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向 asc/desc
     */
    private String sortOrder = "desc";
}

