package com.dxl.food.service;

import com.dxl.food.dao.OrderDetailMapper;
import com.dxl.food.dto.OrderMessageDTO;
import com.dxl.food.enumoperation.OrderStatus;
import com.dxl.food.po.OrderDetailPO;
import com.dxl.food.vo.OrderCreateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * @author : dxl
 * @version: 2022/1/5  15:40
 */
@Slf4j
@Service
public class OrderService {
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    public void createOrder(OrderCreateVO orderCreateVO) throws IOException, TimeoutException, InterruptedException {
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
        //创建物理连接、逻辑连接
        //使用channel发布消息到exchange
        try (Connection connection = connectionFactory.newConnection();
             //传送的消息为对象，先将其序列化为字节流传输
             Channel channel = connection.createChannel()) {
            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
            //exchange:要发布到的exchange
            //routingkey:消息键

            channel.basicPublish("exchange.order.resturant", "key.resturant", null, messageToSend.getBytes());

        }
    }
}
