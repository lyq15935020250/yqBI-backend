package com.yq.yqBI.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author lyq
 * @description:
 * @date 2024/3/9 21:48
 */
@SpringBootTest
class RedisLimiterManagerTest {

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Test
    void doRateLimit() {
        String userID = "1";
        for (int i = 0; i < 5; i++) {
            System.out.println("第" + i + "次请求");
            redisLimiterManager.doRateLimit("userID");
        }
    }
}