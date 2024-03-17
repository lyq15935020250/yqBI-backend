package com.yq.yqBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.Scanner;

/**
 * @author lyq15935020250
 * @Description: direct 交换机，发送消息到指定 routingKey 的队列
 */
public class DlxDirectProducer {

    private static final String WORK_EXCHANGE_NAME = "work_direct_exchange";

    private static final String DLX_EXCHANGE_NAME = "dlx_direct_exchange";


    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 声明死信交换机
            channel.exchangeDeclare(DLX_EXCHANGE_NAME, "direct");

            // 声明死信队列 laoban
            String queueName1 = "laoban-queue";
            channel.queueDeclare(queueName1, true, false, false, null);
            // 绑定队列和交换器
            channel.queueBind(queueName1, DLX_EXCHANGE_NAME, "laoban");

            // 声明死信队列 waibao
            String queueName2 = "waibao-queue";
            channel.queueDeclare(queueName2, true, false, false, null);
            // 绑定队列和交换器
            channel.queueBind(queueName2, DLX_EXCHANGE_NAME, "waibao");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                // 拒绝消息并且不要把消息重新放回到队列，只拒绝当前消息
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                System.out.println(" [死信队列：laoban] Received '" +
                        delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
            };

            DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                // 拒绝消息并且不要把消息重新放回到队列，只拒绝当前消息
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                System.out.println(" [死信队列：waibao] Received '" +
                        delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
            };
            channel.basicConsume(queueName1, false, deliverCallback, consumerTag -> {
            });
            channel.basicConsume(queueName2, false, deliverCallback2, consumerTag -> {
            });

            Scanner scanner = new Scanner(System.in);

            while (scanner.hasNext()) {
                String userInput = scanner.nextLine();
                String[] s = userInput.split(" ");
                if (s.length < 1) {
                    continue;
                }
                String message = s[0];
                String routingKey = s[1];
                // 第二个属性为消息的路由键
                channel.basicPublish(WORK_EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
                System.out.println(" [x] Sent '" + message + " with routingKey:" + routingKey + "' ");
            }
        }
    }
    //..
}