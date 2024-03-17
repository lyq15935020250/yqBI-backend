package com.yq.yqBI.bimq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author lyq
 * @description:
 * @date 2024/3/17 17:24
 */
@Component
public class MyMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * @param exchange 交换机名称，指定发送到那个交换机
     * @param routingKey 队列的路由键，指定消息要根据什么规则发送到相应的队列
     * @param message 要发送的消息
     */
    public void sendMessage(String exchange, String routingKey, String message) {
        // 使用 RabbitTemplate 的 convertAndSend 方法将消息发送到指定的交换机和队列
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
