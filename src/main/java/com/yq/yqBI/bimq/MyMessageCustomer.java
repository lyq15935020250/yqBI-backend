package com.yq.yqBI.bimq;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * @author lyq
 * @description:
 * @date 2024/3/17 17:24
 */
@Component
@Slf4j
public class MyMessageCustomer {

    /**
     *
     * @param message 接受到的消息
     * @param channel 消息所在的通道，可以通过通道与 RabbitMQ 进行交互
     * @param deliveryTag 消息投递的标签，消息的唯一标识
     * @Description:
     *              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag 是一个方法注解，用于获取消息的唯一投递标签，赋值给 long deliveryTag
     *              RabbitMQ 中的每一条消息都会被分配一个唯一的投递标签，用于标识该消息在通道中的投递状态和顺序，
     */
    @SneakyThrows
    @RabbitListener(queues = {"code_queue"},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        log.info("receive message：{}", message);
        // 投递标签是一个数字表示，它在消费者接受到消息后向 RabbitMQ 返回消息的确认状态，告知 RabbitMQ 是否删除消息
        // 手动确认消息
        channel.basicAck(deliveryTag, false);
    }

}
