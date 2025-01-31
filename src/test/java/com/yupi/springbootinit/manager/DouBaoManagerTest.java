package com.yupi.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class DouBaoManagerTest {

    @Autowired
    private DouBaoManager arkUtil;

    @Test
    public void testPerformStandardChatCompletion() {
        String content = "分析内容某处24小时用电情况，请使用折线图原始数据：时间,0:00,1:15,2:30,3:45,5:00,6:15,7:30,8:45,10:00,11:15,12:30,13:45,15:00,16:15,17:30,18:45,20:00,21:15,22:30,23:45,400 瓦/W,300,280,250,260,270,300,550,500,400,390,380,390,400,500,600,750,800,700,600,400";
        arkUtil.performStandardChatCompletion(content);
        // 这里可以添加更多测试逻辑，例如对结果进行断言
    }

    @Test
    public void testPerformStreamingChatCompletion() {
        arkUtil.performStreamingChatCompletion();
        // 这里可以添加更多测试逻辑，例如对结果进行断言
    }

}