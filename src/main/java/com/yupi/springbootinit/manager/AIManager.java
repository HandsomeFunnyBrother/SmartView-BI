package com.yupi.springbootinit.manager;

import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于对接 AI 平台
 */
@Service
@Slf4j
public class AIManager {

    @Resource
    private SparkClient sparkClient;

    /**
     * 向 AI 发送请求
     *
     * @param isNeedTemplate 是否使用模板，进行 AI 生成； true 使用 、false 不使用 ，false 的情况是只想用 AI 不只是生成前端代码
     * @param content        内容
     *                       分析需求：
     *                       分析网站用户的增长情况
     *                       原始数据：
     *                       日期,用户数
     *                       1号,10
     *                       2号,20
     *                       3号,30
     * @return AI 返回的内容
     * '【【【【【'
     * <p>
     * '【【【【【'
     */
    public String sendMsgToXingHuo(boolean isNeedTemplate, String content) {
        if (isNeedTemplate) {
            // AI 生成问题的预设条件
            String predefinedInformation ="请根据以上内容，完成以下两部分任务：\n" +
                    "    1.\t生成一个严格符合 JSON 语法规范的 Echarts V5 的 option 配置对象，用于前端数据可视化。请注意：\n" +
                    "    •\t确保属性名称和字符串值都用双引号括起来。\n" +
                    "    •\t确保 JSON 结构完整，无多余属性或格式错误。\n" +
                    "    2.\t\t根据数据和需求，提供数据分析结论，越详细越好，仅需生成内容，不要包括多余的标注性文字。\n" +
                    "输出格式如下（严格按照格式要求）：\n" +
                    "【【【【【\n" +
                    "{以严格JSON格式生成的Echarts V5的option对象}\n" +
                    "【【【【【\n" +
                    "{分析结论，用自然语言清晰描述}\n" +
                    "【【【【【";
            content = predefinedInformation + "\n" + content;
        }
        List<SparkMessage> messages = new ArrayList<>();
        messages.add(SparkMessage.userContent(content));
        // 构造请求
        SparkRequest sparkRequest = SparkRequest.builder()
                // 消息列表
                .messages(messages)
                // 模型回答的tokens的最大长度,非必传,取值为[1,4096],默认为2048
                .maxTokens(2048)
                // 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
                .temperature(0.2)
                // 指定请求版本，默认使用最新2.0版本
                .apiVersion(SparkApiVersion.V4_0)
                .build();
        // 同步调用
        SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
        String responseContent = chatResponse.getContent();
        log.info("星火 AI 返回的结果 {}", responseContent);
        return responseContent;
    }
}
