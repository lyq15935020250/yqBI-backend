package com.yq.yqBI.manager;

import com.yq.yqBI.common.ErrorCode;
import com.yq.yqBI.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lyq
 * @description: Redission限流基础服务
 * @date 2024/3/9 17:23
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 实施基于键的速率限制。
     * 这个方法用于对特定的资源或操作应用速率限制，以控制在一定时间内可以执行的次数。
     * 它通过键来区分不同的限流器，允许对不同的用户或其他区分维度进行单独的速率限制。
     *
     * @param key 用于区分不同限流器的键。例如，可以按照不同的用户ID分开统计，以实现对每个用户请求速率的独立控制。
     *
     * 注意：此方法不返回任何值，它只是尝试获取令牌，如果获取失败（即请求超出限制），则抛出业务异常。
     */
    public void doRateLimit(String key){
        // 通过键获取限流器，并设置速率限制为每秒最多2个请求
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);

        // 尝试获取令牌，如果获取失败（即请求超出限制），则返回false
        boolean option = rateLimiter.tryAcquire(1);
        if (!option){
            // 请求超出限制，抛出业务异常
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }


}
