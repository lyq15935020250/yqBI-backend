package com.yq.yqBI.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;

/**
 * @author lyq15935020250
 * @Description: topic 交换机，可以发送一组消息到多个队列，routingKey 使用模糊匹配
 * 有两中匹配的通配符：#（匹配0个或多个单词/字符串）  *（匹配单个单词/字符串）
 */
public class TopicProducer {

    private static final String EXCHANGE_NAME = "topic_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, "topic");

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