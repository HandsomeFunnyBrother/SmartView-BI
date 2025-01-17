package com.yupi.springbootinit.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 队列测试
 */

@RestController
@RequestMapping("/queue")
@Slf4j
//被标注的 Bean 只会在dev（开发）或者local（本地）环境被激活时，
// 才会被 Spring 容器实例化并管理。
@Profile({"dev","local"})
public class QueueController {
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("/add")
    public void add(String name) {
        //使用ComletableFuture运行一个异步任务
        CompletableFuture.runAsync(() -> {
            // 日志
            log.info("任务执行中：" + name + ",执行人，" + Thread.currentThread());

            try {
                //模拟长时间运行任务
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //异步任务在threadPoolExecutor中进行
        }, threadPoolExecutor);
    }

    /**
     * 得到线程的状态信息
     */
    @GetMapping("/get")
    public String get() {
        //存储线程池状态信息
        HashMap<Object, Object> map = new HashMap<>();
        //获取线程池队列长度
        int size = threadPoolExecutor.getQueue().size();
        // 将队列长度放入map中
        map.put("队列长度", size);
        // 获取线程池已接收的任务总数
        long taskCount = threadPoolExecutor.getTaskCount();
        // 将任务总数放入map中
        map.put("任务总数", taskCount);
        // 获取线程池已完成的任务数
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        // 将已完成的任务数放入map中
        map.put("已完成任务数", completedTaskCount);
        // 获取线程池中正在执行任务的线程数
        int activeCount = threadPoolExecutor.getActiveCount();
        // 将正在工作的线程数放入map中
        map.put("正在工作的线程数", activeCount);
        // 将map转换为JSON字符串并返回
        return JSONUtil.toJsonStr(map);
    }
}
