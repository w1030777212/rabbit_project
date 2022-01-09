package com.dxl.food.config;

import com.dxl.food.service.OrderMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author : dxl
 * @version: 2022/1/7  18:04
 */
@Configuration
public class RabbitConfig {
    @Autowired
    OrderMessageService orderMessageService;

    @Autowired
    public void startListenMessage() throws InterruptedException, IOException, TimeoutException {
        orderMessageService.handleMessage();
    }
}
