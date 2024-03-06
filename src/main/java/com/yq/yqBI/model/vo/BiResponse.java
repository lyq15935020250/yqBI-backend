package com.yq.yqBI.model.vo;

import lombok.Data;

/**
 * @author lyq
 * @description: BI 返回结果
 * @date 2024/3/6 0:10
 */

@Data
public class BiResponse {

    private String genChart;
    private String genResult;
    private Long chartId;
}

