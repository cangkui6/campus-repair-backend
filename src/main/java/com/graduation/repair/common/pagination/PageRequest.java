package com.graduation.repair.common.pagination;

import lombok.Data;

@Data
public class PageRequest {

    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
