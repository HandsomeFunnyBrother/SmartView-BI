package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class SingleProducer {
    // 定义一个常量字符串，表示要发送到的队列名称
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) throws Exception {
        // 创建一个连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        // 设置连接工厂的主机名为 localhost
        factory.setHost("localhost");
        try (
                // 使用连接工厂创建一个新的连接
                Connection connection = factory.newConnection();
                // 使用连接创建一个新的通道
                Channel channel = connection.createChannel()) {
            // 声明一个队列，名称为 QUEUE_NAME，不持久化，不独占，不自动删除，没有额外参数
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            // 创建要发送的消息内容
            String message = "Hello World!";
            // 将消息发布到默认交换机，路由键为 QUEUE_NAME，消息体为 message 的字节数组，使用 UTF-8 编码
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            // 打印发送的消息内容
            System.out.println(" [x] Sent '" + message + "'");
        }

    }
}
