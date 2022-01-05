package com.dxl.food.dto;

import com.dxl.food.enumoperation.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author : dxl
 * @version: 2022/1/5  14:08
 */
@Data
public class OrderMessageDTO {
    //订单id
    private Integer orderId;
    //订单状态
    private OrderStatus orderStatus;
    //价格
    private BigDecimal price;
    //骑手id
    private Integer deliverymanId;
    //产品id
    private Integer productId;
    //用户id
    private Integer accountId;
    //结算id
    private Integer settlementId;
    //积分结算Id
    private Integer rewardId;
    //积分奖励数量
    private Integer rewardAmount;
    //确认
    private Boolean confirmed;
}
