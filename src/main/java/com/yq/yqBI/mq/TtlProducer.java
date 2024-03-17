package com.yq.yqBI.mq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lyq15935020250
 * @Description: 消息过期机制
 * 1、对消息队列设置过期时间，队列中的所有消息都会遵循这个过期时间
 * 2、给某条消息指定过期时间
 */
public class TtlProducer {

    private final static String QUEUE_NAME = "ttl-queue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             // 通过连接创建通道，相当于操作RabbitMQ的client
             Channel channel = connection.createChannel()) {

            // 设置队列参数
            Map<String, Object> args = new HashMap<>();
            args.put("x-message-ttl", 60000);
            // 设置队列过期时间
            channel.queueDeclare(QUEUE_NAME, false, false, false, args);

            String message = "Hello World!";
            // 给消息设置过期时间
//            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
//                    // 设置过期时间为 1s
//                    .expiration("1000")
//                    .build();
//            channel.basicPublish("", QUEUE_NAME, properties, message.getBytes(StandardCharsets.UTF_8));

            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
}