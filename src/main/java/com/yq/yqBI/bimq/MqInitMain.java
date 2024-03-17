package com.yq.yqBI.bimq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * @author lyq
 * @description:
 * @date 2024/3/17 21:03
 */
public class MqInitMain {
    public static void main(String[] args) {

        try {
            // 创建连接
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            // 声明交换机
            String EXCHANGE_NAME = "code_exchange";
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            // 声明队列
            String QUEUE_NAME = "code_queue";
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            // 绑定交换机和队列
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "my_routingKey");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
