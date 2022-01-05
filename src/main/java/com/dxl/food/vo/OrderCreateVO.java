package com.dxl.food.vo;

import lombok.Data;

/**
 * @author : dxl
 * @version: 2022/1/5  14:15
 */
@Data
public class OrderCreateVO {
    //用户id
    private Integer accountId;
    //地址
    private String address;
    //产品id
    private Integer productId;
}
