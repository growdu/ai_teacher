package com.aiteacher.common;

import lombok.Data;

@Data
public class PageRequest {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String orderBy;
    private String sortDirection = "asc";

    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }
}