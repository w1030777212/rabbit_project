package com.dxl.food.po;

import com.dxl.food.enumoperation.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author : dxl
 * @version: 2022/1/5  14:16
 */
@Data
public class OrderDetailPO {
    private Integer id;
    private OrderStatus status;
    private String address;
    private Integer accountId;
    private Integer productId;
    private Integer deliverymanId;
    private Integer settlementId;
    private Integer rewardId;
    private BigDecimal price;
    private Date date;
}
