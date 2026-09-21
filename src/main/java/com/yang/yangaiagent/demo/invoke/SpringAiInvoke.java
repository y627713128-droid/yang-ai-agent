package com.yang.yangaiagent.demo.invoke;


import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SpringAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashScopeChatModel;

    @Override
    public void run(String... args) throws Exception {
/*        AssistantMessage output = dashScopeChatModel.call(new Prompt("你好 我是Yang, 现在我正在做超级智能体项目！"))
                .getResult()
                .getOutput();
        System.out.println(output.getText());*/

    }
}
