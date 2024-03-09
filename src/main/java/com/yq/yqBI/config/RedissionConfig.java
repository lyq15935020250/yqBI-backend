package com.yq.yqBI.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lyq
 * @description:
 * @date 2024/3/9 17:03
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissionConfig {

    private Integer database;

    private String host;

    private Integer port;

    @Bean
    public RedissonClient getRedission() {
        Config config = new Config();
        // 添加 Redission 单机配置
        config.useSingleServer()
                // 设置 Redis 数据库
                .setDatabase(database)
                // 设置 Redis 地址
                .setAddress("redis://" + host + ":" + port);
        return Redisson.create(config);
    }

}
