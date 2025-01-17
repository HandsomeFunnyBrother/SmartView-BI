package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.util.Scanner;

public class MultiProducer {
    // 定义队列名称，当前使用的队列名称为 "multi_queue"
    private static final String TASK_QUEUE_NAME = "multi_queue";

    public static void main(String[] argv) throws Exception {
        // 创建连接工厂，用于建立与RabbitMQ的连接
        ConnectionFactory factory = new ConnectionFactory();

        // 设置RabbitMQ服务的主机名为 "localhost"，即本地服务
        factory.setHost("localhost");

        // 创建一个新的连接到RabbitMQ服务
        try (Connection connection = factory.newConnection();
             // 创建一个新的频道，用于发送和接收消息
             Channel channel = connection.createChannel()) {

            // 声明一个队列，若队列不存在，则创建该队列
            // 参数说明：
            // - TASK_QUEUE_NAME: 队列名称
            // - true: 队列持久化，确保消息在RabbitMQ重启后仍然存在
            // - false: 排他性队列（此队列仅在本连接中使用，关闭连接后队列消失）
            // - false: 自动删除（消息消费者断开后，队列自动删除）
            // - null: 额外的队列参数，当前没有设置
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);

            // 创建输入扫描器，用于读取用户在控制台的输入
            Scanner scanner = new Scanner(System.in);

            // 使用循环持续读取用户输入的每一行文本
            while (scanner.hasNext()) {
                // 获取用户输入的一行文本消息
                String message = scanner.nextLine();

                // 将消息发送到队列
                // 参数说明：
                // - "": 交换机名称，空字符串表示使用默认交换机
                // - TASK_QUEUE_NAME: 消息投递的目标队列名称
                // - MessageProperties.PERSISTENT_TEXT_PLAIN: 消息持久化设置，确保消息不会丢失
                // - message.getBytes("UTF-8"): 将消息字符串转换为字节数组，并指定字符编码为UTF-8
                channel.basicPublish("", TASK_QUEUE_NAME,
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes("UTF-8"));

                // 控制台输出，表示消息已发送到队列
                System.out.println(" [x] Sent '" + message + "'");
            }
        }
    }
}

