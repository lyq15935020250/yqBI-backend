package com.yq.yqBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.util.Scanner;

/**
 * @author lyq15935020250
 * @Description: direct 交换机，发送消息到指定 routingKey 的队列
 */
public class DirectProducer {

    private static final String EXCHANGE_NAME = "direct_queue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 声明交换机，指定交换机类型为 direct
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

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
                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
                System.out.println(" [x] Sent '" + message + "with routingKey:" + routingKey + "' ");
            }
        }
    }
    //..
}