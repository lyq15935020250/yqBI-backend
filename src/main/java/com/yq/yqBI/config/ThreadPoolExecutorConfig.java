package com.yq.yqBI.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author lyq
 * @description:
 * @date 2024/3/11 23:49
 */
@Configuration
public class ThreadPoolExecutorConfig {

    /**
     * 线程工厂
     */
    ThreadFactory threadFactory = new ThreadFactory() {
        // 初始化线程为1
        int count = 1;

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("线程_" + count);
            count++;
            return thread;
        }
    };

    /**
     * 线程池
     * 参数：
     *      1、核心线程数 2
     *      2、线程池最大线程数 4
     *      3、空闲线程的过期时间 100s
     *      4、过期时间单位 s
     *      5、工作队列：数组阻塞队列
     *      6、线程工厂
     *      7、拒绝策略：默认拒绝
     * @return
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2, 4, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(4), threadFactory);
        return threadPoolExecutor;
    }

}
