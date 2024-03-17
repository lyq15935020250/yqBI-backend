package com.yq.yqBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DlxDirectCustomer {

    private static final String WORK_EXCHANGE_NAME = "work_direct_exchange";

    private static final String DLX_EXCHANGE_NAME = "dlx_direct_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        // 工作交换机
        channel.exchangeDeclare(WORK_EXCHANGE_NAME, "direct");

        // 给需要容错的队列设置死信队列，配置参数：死信交换机和死信队列的路由键
        Map<String, Object> args1 = new HashMap<>();
        args1.put("x-dead-letter-exchange", DLX_EXCHANGE_NAME);
        args1.put("x-dead-letter-routing-key", "laoban");

        // 工作队列：lyqq
        String queueName = "lyqq-queue";
        channel.queueDeclare(queueName, true, false, false, args1);
        // 绑定队列和交换器
        channel.queueBind(queueName, WORK_EXCHANGE_NAME, "lyqq");

        Map<String, Object> args2 = new HashMap<>();
        args2.put("x-dead-letter-exchange", DLX_EXCHANGE_NAME);
        args2.put("x-dead-letter-routing-key", "waibao");

        // 工作队列：lyxx
        String queueName2 = "lyxx-queue";
        channel.queueDeclare(queueName2, true, false, false, args2);
        // 绑定队列和交换器
        channel.queueBind(queueName2, WORK_EXCHANGE_NAME, "lyxx");


        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            // 拒绝消息并且不要把消息重新放回到队列，只拒绝当前消息
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            System.out.println(" [lyqq] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            // 拒绝消息并且不要把消息重新放回到队列，只拒绝当前消息
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            System.out.println(" [lyxx] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });

        channel.basicConsume(queueName2, false, deliverCallback2, consumerTag -> {
        });
    }
}