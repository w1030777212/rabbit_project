package com.dxl.food.service;

import com.dxl.food.dao.OrderDetailMapper;
import com.dxl.food.dto.OrderMessageDTO;
import com.dxl.food.enumoperation.OrderStatus;
import com.dxl.food.po.OrderDetailPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author : dxl
 * @version: 2022/1/5  15:13
 */
@Slf4j
@Service
public class OrderMessageService {




    @Autowired
    private OrderDetailMapper orderDetailMapper;
    ObjectMapper objectMapper = new ObjectMapper();


    @Async
    public void handleMessage() throws IOException, TimeoutException, InterruptedException {
        log.info("start linstening message");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setHost("localhost");
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            /*---------------------restaurant---------------------*/
            channel.exchangeDeclare(
                    "exchange.order.restaurant",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);

            channel.queueDeclare(
                    "queue.order",
                    true,
                    false,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.order.restaurant",
                    "key.order");


            /*---------------------deliveryman---------------------*/
            channel.exchangeDeclare(
                    "exchange.order.deliveryman",
                    BuiltinExchangeType.DIRECT,
                    true,
                    false,
                    null);


            channel.queueBind(
                    "queue.order",
                    "exchange.order.deliveryman",
                    "key.order");

            /*---------------------settlement---------------------*/

            channel.exchangeDeclare(
                    "exchange.settlement.order",
                    BuiltinExchangeType.FANOUT,
                    true,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.settlement.order",
                    "key.order");

            /*---------------------reward---------------------*/

            channel.exchangeDeclare(
                    "exchange.order.reward",
                    BuiltinExchangeType.TOPIC,
                    true,
                    false,
                    null);

            channel.queueBind(
                    "queue.order",
                    "exchange.order.reward",
                    "key.order");

            channel.basicConsume("queue.order", true, deliverCallback, consumerTag -> {
            });

            while (true) {
                Thread.sleep(100000);
            }
        }
    }


    DeliverCallback deliverCallback = (consumerTag, message) -> {
        String messageBody = new String(message.getBody());
        log.info("deliverCallback:messageBody:{}", messageBody);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        try {
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(messageBody,
                    OrderMessageDTO.class);
            OrderDetailPO orderPO = orderDetailMapper.selectOrder(orderMessageDTO.getOrderId());
            switch (orderPO.getStatus()) {
                case ORDER_CREATING:
                    if (orderMessageDTO.getConfirmed() && null != orderMessageDTO.getPrice()) {
                        orderPO.setStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        orderPO.setPrice(orderMessageDTO.getPrice());
                        orderDetailMapper.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()) {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.deliveryman", "key.deliveryman", null,
                                    messageToSend.getBytes());
                        }
                    } else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailMapper.update(orderPO);
                    }
                    break;
                case RESTAURANT_CONFIRMED:
                    if (null != orderMessageDTO.getDeliverymanId()){
                        orderPO.setStatus(OrderStatus.DELIVERYMAN_CONFIRMED);
                        orderPO.setDeliverymanId(orderMessageDTO.getDeliverymanId());
                        orderDetailMapper.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()){
                            final String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.settlement",
                                    "key.settlement",
                                    null,
                                    messageToSend.getBytes());
                        }
                    }else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailMapper.update(orderPO);
                    }
                    break;
                case DELIVERYMAN_CONFIRMED:
                    if (null != orderMessageDTO.getSettlementId()){
                        orderPO.setStatus(OrderStatus.SETTLEMENT_CONFIRMED);
                        orderPO.setSettlementId(orderMessageDTO.getSettlementId());
                        orderDetailMapper.update(orderPO);
                        try (Connection connection = connectionFactory.newConnection();
                             Channel channel = connection.createChannel()){
                            final String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish("exchange.order.reward",
                                    "key.reward",
                                    null,
                                    messageToSend.getBytes());
                        }
                    }else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailMapper.update(orderPO);
                    }
                    break;
                case SETTLEMENT_CONFIRMED:
                    if (null != orderMessageDTO.getRewardId()){
                        orderPO.setStatus(OrderStatus.ORDER_CREATED);
                        orderPO.setRewardId(orderMessageDTO.getRewardId());
                        orderDetailMapper.update(orderPO);
                    }else {
                        orderPO.setStatus(OrderStatus.FAILED);
                        orderDetailMapper.update(orderPO);
                    }
                    break;
                case ORDER_CREATED:
                    break;
                case FAILED:
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    };
}
