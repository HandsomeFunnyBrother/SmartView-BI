package com.yupi.springbootinit.bizmq;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AIManager;
import com.yupi.springbootinit.manager.DouBaoManager;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.yupi.springbootinit.constant.ChartStatusConstant.RUNNING;
import static com.yupi.springbootinit.constant.ChartStatusConstant.SUCCEED;

// 使用@Component注解标记该类为一个组件，让Spring框架能够扫描并将其纳入管理
@Component
// 使用@Slf4j注解生成日志记录器
@Slf4j
public class BiMessageConsumer {
    @Resource
    private ChartService chartService;
    @Resource
    private AIManager aiManager;
    @Resource
    private DouBaoManager douBaoManager;


    /**
     * 接收消息的方法
     *
     * @param message     接收到的消息内容，是一个字符串类型
     * @param channel     消息所在的通道，可以通过该通道与 RabbitMQ 进行交互，例如手动确认消息、拒绝消息等
     * @param deliveryTag 消息的投递标签，用于唯一标识一条消息
     */
    // 使用@SneakyThrows注解简化异常处理
    @SneakyThrows
    // 使用@RabbitListener注解指定要监听的队列名称为，并设置消息的确认机制为手动确认
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    // @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag是一个方法参数注解,用于从消息头中获取投递标签(deliveryTag),
    // 在RabbitMQ中,每条消息都会被分配一个唯一的投递标签，用于标识该消息在通道中的投递状态和顺序。通过使用@Header(AmqpHeaders.DELIVERY_TAG)注解,可以从消息头中提取出该投递标签,并将其赋值给long deliveryTag参数。
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("receiveMessage message ={}",message);
        if (StringUtils.isBlank(message)){
            //更新失败，拒绝当前消息，不重新入队
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        //消息id类型转换
        long chartId = Long.parseLong(message);
        //根据id查找
        Chart chart = chartService.getById(chartId);
        //状态改为执行中
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(RUNNING);
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            handleChartUpdateError(chart.getId(), "更新图表执行状态失败");
            return;
        }
        // 调用
        String result = douBaoManager.performStandardChatCompletion(buildUserInput(chart));
        //String result = aiManager.sendMsgToXingHuo(true, buildUserInput(chart));
        // 对返回结果做拆分,按照5个中括号进行拆分
        String[] splits = result.split("【【【【【");
        // 拆分之后还要进行校验
        if (splits.length < 3) {
            handleChartUpdateError(chart.getId(), "AI 生成错误");
            return;
        }
        // 从返回的结果中提取生成的图表，去除前后的空格
        String genChart = splits[1].trim();
        // 从返回的结果中提取生成的结果，去除前后的空格
        String genResult = splits[2].trim();
        //得到结果后，再次更新
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus(SUCCEED);
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            // 确认消息的接收，即使更新图表状态失败
            channel.basicAck(deliveryTag, false);
        }


        // 使用日志记录器打印接收到的消息内容
        log.info("receiveMessage message = {}", message);
        // 投递标签是一个数字标识,它在消息消费者接收到消息后用于向RabbitMQ确认消息的处理状态。通过将投递标签传递给channel.basicAck(deliveryTag, false)方法,可以告知RabbitMQ该消息已经成功处理,可以进行确认和从队列中删除。
        // 手动确认消息的接收，向RabbitMQ发送确认消息
        channel.basicAck(deliveryTag, false);
    }

    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage("execMessage");
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }

    }

    /**
     * 构建用户输入
     * @param chart 图表对象
     * @return 用户输入字符串
     */
    private String buildUserInput(Chart chart) {
        // 获取图表的目标、类型和数据
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        // 将StringBuilder转换为String并返回
        return userInput.toString();
    }


}
