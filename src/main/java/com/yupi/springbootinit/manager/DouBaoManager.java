package com.yupi.springbootinit.manager;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DouBaoManager {

    @Autowired
    private ArkService arkService;

    //标准式
    public String performStandardChatCompletion(String content) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content("请根据以下原始数据，完成两部分任务。第一，生成一个严格符合 JSON 语法规范的 Echarts V5 的 option 配置对象，用于前端数据可视化展示某处 24 小时用电情况，确保属性名称和字符串值都用双引号括起来，JSON 结构完整无多余属性或格式错误；第二，根据数据给出详细的数据分析结论。输出格式为：\n" +
                "【【【【【\n" +
                "{以严格 JSON 格式生成的 Echarts V5 的 option 对象}\n" +
                "【【【【【\n" +
                "{分析结论，用自然语言清晰描述}\n" +
                "【【【【【").build();
        ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(content).build();
        messages.add(systemMessage);
        messages.add(userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
               .model("ep-20250122184859-rcdzz")
               .messages(messages)
               .build();

        // 获取返回结果
//        String result = arkService.createChatCompletion(chatCompletionRequest).getChoices().stream()
//                .map(choice -> choice.getMessage().getContent())
//                .collect(Collectors.joining("\n"));

        String result = arkService.createChatCompletion(chatCompletionRequest).getChoices().stream()
                .<String>map(choice -> choice.getMessage().getContent().toString())
                .collect(Collectors.joining("\n"));
        log.info("豆包返回值:{}", result);
        return result;
    }


    //流式
    public void performStreamingChatCompletion() {
        List<ChatMessage> streamMessages = new ArrayList<>();
        ChatMessage streamSystemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content("你是豆包，是由字节跳动开发的 AI 人工智能助手").build();
        ChatMessage streamUserMessage = ChatMessage.builder().role(ChatMessageRole.USER).content("常见的十字花科植物有哪些？").build();
        streamMessages.add(streamSystemMessage);
        streamMessages.add(streamUserMessage);

        ChatCompletionRequest streamChatCompletionRequest = ChatCompletionRequest.builder()
               .model("ep-20250122184859-rcdzz")
               .messages(streamMessages)
               .build();

        arkService.streamChatCompletion(streamChatCompletionRequest)
               .doOnError(Throwable::printStackTrace)
               .blockingForEach(
                        choice -> {
                            if (choice.getChoices().size() > 0) {
                                System.out.print(choice.getChoices().get(0).getMessage().getContent());
                            }
                        }
                );
    }
}