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
public class BiMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * @param message 要发送的消息
     */
    public void sendMessage(String message) {
        // 使用 RabbitTemplate 的 convertAndSend 方法将消息发送到指定的交换机和队列
        rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY, message);
    }
}
