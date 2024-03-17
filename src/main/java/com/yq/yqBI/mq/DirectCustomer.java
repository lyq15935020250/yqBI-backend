package com.yq.yqBI.mq;

import com.rabbitmq.client.*;

public class DirectCustomer {

    private static final String EXCHANGE_NAME = "direct_queue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "direct");

        String queueName = "yy";
        channel.queueDeclare(queueName, true, false, false, null);
        // 绑定队列和交换器，设置路由键为 “yy”
        channel.queueBind(queueName, EXCHANGE_NAME, "yy");

        String queueName2 = "qq";
        channel.queueDeclare(queueName2, true, false, false, null);
        // 绑定队列和交换器，设置路由键为 “qq”
        channel.queueBind(queueName2, EXCHANGE_NAME, "qq");


        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
        });
        channel.basicConsume(queueName2, true, deliverCallback2, consumerTag -> {
        });
    }
}