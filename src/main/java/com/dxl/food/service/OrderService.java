package com.dxl.food.service;

import com.dxl.food.dao.OrderDetailMapper;
import com.dxl.food.dto.OrderMessageDTO;
import com.dxl.food.enumoperation.OrderStatus;
import com.dxl.food.po.OrderDetailPO;
import com.dxl.food.vo.OrderCreateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * @author : dxl
 * @version: 2022/1/5  15:40
 */
@Slf4j
public class OrderService {
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    private ObjectMapper objectMapper;

    public void createOrder(OrderCreateVO orderCreateVO) throws IOException, TimeoutException {
        log.info("createOrder:orderCreateVO:{}", orderCreateVO);
        OrderDetailPO orderPO = new OrderDetailPO();
        orderPO.setAddress(orderCreateVO.getAddress());
        orderPO.setAccountId(orderCreateVO.getAccountId());
        orderPO.setProductId(orderCreateVO.getProductId());
        orderPO.setStatus(OrderStatus.ORDER_CREATING);
        orderPO.setDate(new Date());
        orderDetailMapper.insert(orderPO);

        OrderMessageDTO orderMessageDTO = new OrderMessageDTO();
        orderMessageDTO.setOrderId(orderPO.getId());
        orderMessageDTO.setProductId(orderPO.getProductId());
        orderMessageDTO.setAccountId(orderCreateVO.getAccountId());

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            channel.basicPublish("exchange.order.restaurant", "key.restaurant", null, messageToSend.getBytes());
        }
    }
}
