package com.dxl.food.service;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author : dxl
 * @version: 2022/1/5  15:13
 */
public class OrderMessageService {
    public void handlerMessage(){
        //构造连接，连接rabbitmq server
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
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

            }
        } catch (IOException e) {

        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
