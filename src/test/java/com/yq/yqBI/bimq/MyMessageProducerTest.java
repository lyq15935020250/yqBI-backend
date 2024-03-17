package com.yq.yqBI.bimq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author lyq
 * @description:
 * @date 2024/3/17 21:13
 */
@SpringBootTest
class MyMessageProducerTest {

    @Resource
    private MyMessageProducer myMessageProducer;

    @Test
    void sendMessage() {

        myMessageProducer.sendMessage("code_exchange", "my_routingKey", "hello lyqq");
    }
}