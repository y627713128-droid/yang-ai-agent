package com.yang.yangaiagent.app;

import com.yang.yangaiagent.entity.LoveReport;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void doChat() {

        String chatId = UUID.randomUUID().toString();

        //第一轮对话
        String message = "你好, 我是Yang, 我现在有一些感情问题想要咨询你";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

/*        //第二轮对话
        message = "我现在正在追求一个女孩儿, 但是她的回应比较冷淡, 我应该怎么办";
        answer  = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

        //第三轮对话
        message = "你还记得我上一个问题是什么吗？";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);*/

    }


    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好, 我是Yang, 我想要我的另一半更加爱我, 我需要怎么做?";
        LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好, 我是Yang, 我和我的女朋友恋爱了恋爱期间需要找对方一直聊天吗";
        String answer = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }
}