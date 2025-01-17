package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MultiConsumer {
    private static final String TASK_QUEUE_NAME = "multi_queue";

    public static void main(String[] argv) throws Exception {
        // 创建连接工厂，设置连接RabbitMQ的参数
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");  // 设置RabbitMQ服务器地址为localhost
        final Connection connection = factory.newConnection();  // 创建新的连接

        // 创建两个消费者
        for (int i = 0; i < 2; i++) {
            final Channel channel = connection.createChannel();  // 为每个消费者创建一个通道

            // 声明队列，确保队列存在（如果不存在会创建）
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            // 设置预取计数为1，这样RabbitMQ在给消费者新消息之前，会等待先前的消息被确认
            channel.basicQos(1);

            // 记录当前消费者的编号
            int finalI = i;

            // 创建消息处理回调，处理接收到的消息
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                // 获取消息内容并转换为字符串
                String message = new String(delivery.getBody(), "UTF-8");

                try {
                    // 打印接收到的消息，编号与消息内容
                    System.out.println(" [x] Received '" + "编号:" + finalI + ":" + message + "'");

                    // 模拟消息处理的时间（这里设置为20秒）
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    // 处理完消息后，打印完成的提示
                    System.out.println(" [x] Done");

                    // 手动确认消息已经处理完毕，RabbitMQ才会移除该消息
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };

            // 启动消费者，等待接收消息并处理
            channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> { });
        }
    }
}
