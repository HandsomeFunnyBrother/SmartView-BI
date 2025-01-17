package com.yupi.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;

@SpringBootTest
class RedisLimiterManagerTest {
    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Test
    void doRateLimit() throws InterruptedException {
        //模拟操作
        String userId = "1";
        //瞬间执行两次，成功打印一次
        for (int i = 0; i < 2; i++) {
            redisLimiterManager.doRateLimit(userId);
            System.out.println("成功");
        }
        //sleep 1s
        Thread.sleep(1000);
        //瞬间执行5次，成功打印
        for (int i = 0; i < 5; i++) {
            redisLimiterManager.doRateLimit(userId);
            System.out.println("成功");
        }
    }
}