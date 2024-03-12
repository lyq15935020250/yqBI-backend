package com.yq.yqBI.controller;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/queue")
@Profile({"dev","local"})
@Slf4j
public class ThreadController {

    /**
     * 注入线程池实例
     */
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("/add")
    public void add(String name){
        CompletableFuture.runAsync(() -> {
           // 打印日志包括任务名称和执行线程名称
           log.info("任务名称：{}，执行线程名称：{}", name, Thread.currentThread().getName());
            try {
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 异步任务在 threadPoolExecutor 中执行
        }, threadPoolExecutor);
    }

    @GetMapping("/get")
    public String get(){
        Map<String, Object> map = new HashMap<>();
        // 获取队列长度
        int size = threadPoolExecutor.getQueue().size();
        map.put("线程池队列长度：", size);
        // 获取任务总数
        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("任务总数：", taskCount);
        // 获取已完成任务数量
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        map.put("已完成任务数量：", completedTaskCount);
        // 获取线程池中正在执行的任务数量
        int activeCount = threadPoolExecutor.getActiveCount();
        map.put("正在执行任务数量：", activeCount);
        return JSONUtil.toJsonStr(map);
    }

}
