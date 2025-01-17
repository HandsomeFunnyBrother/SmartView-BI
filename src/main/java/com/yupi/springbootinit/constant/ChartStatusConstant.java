package com.yupi.springbootinit.constant;

//表格状态常量
public interface ChartStatusConstant {

    //任务状态字段(排队中wait、执行中running、已完成succeed、失败failed)

    //排队中
    String WAIT = "wait";
    //执行中
    String RUNNING = "running";
    //已完成
    String SUCCEED = "succeed";
    //失败
    String FAILED = "failed";
}
