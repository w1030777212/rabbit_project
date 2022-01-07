package com.dxl.food.service;

import com.dxl.food.dao.OrderDetailMapper;
import com.dxl.food.dto.OrderMessageDTO;
import com.dxl.food.enumoperation.OrderStatus;
import com.dxl.food.po.OrderDetailPO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author : dxl
 * @version: 2022/1/5  15:13
 */
@Service
public class OrderMessageService {
    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Async
    public void handlerMessage() throws InterruptedException {
        //构造连接，连接rabbitmq server
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        DeliverCallback deliverCallback = null;
        //创建连接，以及创建channel
        try {
            try(Connection connection = connectionFactory.newConnection();
                Channel channel = connection.createChannel()){
                //通过channel声明对应的exchange、queue
                //1.durable：是否持久化(非持久化的交换机在rabbitmq server重启后会丢失)
                //2.autoDelete：是否自动删除(当交换机没有关联的队列时会被自动删除)
                channel.exchangeDeclare(
                        "exchange.order.resturant",
                        BuiltinExchangeType.DIRECT,
                        true,
                        false,
                        null
                );
                //1.durable：是否持久化(非持久化的队列在rabbitmq server重启后会丢失)
                //2.exclusive：该队列是否被当前连接独占
                //3.autoDelete：是否自动删除(当队列没有关联的交换机时会被自动删除)
                channel.queueDeclare(
                        "queue.order",
                        true,
                        false,
                        false,
                        null
                );
                //构造binding
                channel.queueBind(
                        "queue.order",
                        "exchange.order.resturant",
                        "key.order"
                        );

                channel.exchangeDeclare(
                        "exchange.order.deliveryman",
                        BuiltinExchangeType.DIRECT,
                        true,
                        false,
                        null
                );
                channel.queueBind(
                        "queue.order",
                        "exchange.order.deliveryman",
                        "key.order"
                );
                channel.basicConsume(
                        "queue.order",
                        true,
                        deliverCallback,
                        consumerTag -> {}
                        );
            }
        } catch (IOException e) {

        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        deliverCallback = ((consumerTag, message) -> {
            //消息体
            byte[] body = message.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            OrderMessageDTO orderMessageDTO = objectMapper.readValue(body, OrderMessageDTO.class);
            final OrderDetailPO orderDetailPO = orderDetailMapper.selectOrder(orderMessageDTO.getOrderId());
            OrderStatus status = orderMessageDTO.getOrderStatus();
            switch (status){
                case ORDER_CREATING:
                    if (orderMessageDTO.getConfirmed() && orderMessageDTO.getPrice() != null){
                        orderDetailPO.setPrice(orderMessageDTO.getPrice());
                        orderDetailPO.setStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        orderDetailMapper.update(orderDetailPO);
                        try(Connection connection = connectionFactory.newConnection();
                            Channel channel = connection.createChannel())
                        {
                            String messageToSend = objectMapper.writeValueAsString(orderMessageDTO);
                            channel.basicPublish(
                                    "exchange.order.deliveryman",
                                    "key.deliverman",
                                    null,
                                    messageToSend.getBytes()
                                    );
                        }catch (Exception e){

                        }
                    }else {
                        orderDetailPO.setStatus(OrderStatus.FAILED);
                        orderDetailMapper.update(orderDetailPO);
                    }
                    break;
                case RESTAURANT_CONFIRMED:
                    break;
                case DELIVERYMAN_CONFIRMED:
                    break;
                case SETTLEMENT_CONFIRMED:
                    break;
                case ORDER_CREATED:
                    break;
                case FAILED:
                    break;
            }
        });
        while (true){
            Thread.sleep(1000000);
        }
    }
}
