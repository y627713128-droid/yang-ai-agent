package com.yang.yangaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 限制敏感词 Advisor
 */
@Slf4j
public class SensitiveWordAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private final List<String> sensitiveWords;
    private static final String BLOCK_MESSAGE = "抱歉，您的内容包含不适当的信息，无法继续处理，请修改后重新提问。";

    public SensitiveWordAdvisor() {
        this.sensitiveWords = loadSensitiveWords();
        log.info("敏感词加载完成，共加载 {} 个敏感词", sensitiveWords.size());
    }

    /**
     * 从 resources/sensitiveword.txt 加载敏感词
     */
    private List<String> loadSensitiveWords() {

        ClassPathResource resource = new ClassPathResource("/word/sensitiveword.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("#"))
                    .distinct()
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("加载敏感词文件失败，file=sensitiveword.txt", e);
            throw new RuntimeException("加载敏感词文件失败", e);
        }
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {

        // 1. 检查用户输入
        String userMessage = advisedRequest.userText();
        String detectedWord = findSensitiveWord(userMessage);
        if (detectedWord != null) {
            log.warn("检测到用户输入包含敏感词，advisor={}, detectedWord={}", getName(), detectedWord);
            AdvisedResponse response = createBlockedResponse("input", detectedWord);
            log.warn("Advisor上下文：{}", response.adviseContext());
            log.warn("AI响应：{}", response.response()
                    .getResult()
                    .getOutput()
                    .getText());
            // 用户输入违规，直接拦截
            return response;
        }

        // 2. 正常调用大模型
        log.info("敏感词检查通过，正常调用大模型，advisor={}", getName());
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);

        // 3. 检查 AI 输出
        String responseText = null;
        if (advisedResponse.response() != null && advisedResponse.response().getResult() != null && advisedResponse.response().getResult().getOutput() != null) {
            responseText = advisedResponse.response()
                    .getResult()
                    .getOutput()
                    .getText();
        }

        detectedWord = findSensitiveWord(responseText);

        if (detectedWord != null) {
            log.warn("检测到AI响应包含敏感词，advisor={}, detectedWord={}", getName(), detectedWord);
            // AI 输出违规，替换成统一提示
            return createBlockedResponse("output", detectedWord);
        }

        // 4. 正常返回
        log.info("AI响应敏感词检查通过，正常返回，advisor={}", getName());
        return advisedResponse;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {

        // 1. 检查用户输入
        String userMessage = advisedRequest.userText();
        AtomicReference<String> detectedWord = new AtomicReference<>(findSensitiveWord(userMessage));
        if (detectedWord.get() != null) {
            log.warn("检测到流式请求包含敏感词，advisor={}, detectedWord={}", getName(), detectedWord);
            // 不调用大模型，直接返回统一提示
            return Flux.just(createBlockedResponse("input", detectedWord.get()));
        }
        log.info("流式请求敏感词检查通过，开始调用大模型，advisor={}", getName());

        // 2. 获取 AI 流式响应

        return chain.nextAroundStream(advisedRequest)
                .collectList()
                .flatMapMany(responses -> {
                    // 3. 拼接完整 AI 响应
                    StringBuilder fullText = new StringBuilder();
                    for (AdvisedResponse response : responses) {
                        if (response.response() != null
                                && response.response().getResult() != null
                                && response.response().getResult().getOutput() != null) {
                            String text = response.response()
                                    .getResult()
                                    .getOutput()
                                    .getText();
                            if (text != null) {
                                fullText.append(text);
                            }
                        }
                    }

                    // 4. 检查完整 AI 响应
                    String responseText = fullText.toString();
                    detectedWord.set(findSensitiveWord(responseText));
                    if (detectedWord.get() != null) {
                        log.warn("检测到流式AI响应包含敏感词，advisor={}, detectedWord={}", getName(), detectedWord);
                        // AI 输出违规
                        return Flux.just(createBlockedResponse("output", detectedWord.get()));
                    }

                    // 5. 检查通过，正常返回
                    log.info("流式AI响应敏感词检查通过，正常返回，advisor={}", getName());
                    return Flux.fromIterable(responses);
                });
    }


    /**
     * 查找敏感词
     */
    private String findSensitiveWord(String text) {

        if (text == null || text.isEmpty()) {
            return null;
        }

        for (String sensitiveWord : sensitiveWords) {

            if (text.contains(sensitiveWord)) {
                return sensitiveWord;
            }
        }

        return null;
    }


    /**
     * 创建敏感词拦截响应
     */
    private AdvisedResponse createBlockedResponse(String action, String detectedWord) {

        // 1. 构造统一返回内容
        AssistantMessage assistantMessage = new AssistantMessage(BLOCK_MESSAGE);

        Generation generation = new Generation(assistantMessage);

        ChatResponse chatResponse = new ChatResponse(List.of(generation));

        // 2. 添加 Advisor 上下文
        Map<String, Object> adviseContext = new HashMap<>();
        adviseContext.put("advisor.name", getName());
        adviseContext.put("advisor.action", "blocked");
        adviseContext.put("advisor.reason", "Forbidden Word Detected");
        adviseContext.put("advisor.detectedWord", detectedWord);
        adviseContext.put("advisor.blockPosition", action);
        // 3. 返回统一响应
        return new AdvisedResponse(chatResponse, adviseContext);
    }


    @Override
    public String getName() {
        return "SensitiveWordAdvisor";
    }


    @Override
    public int getOrder() {
        return 0;
    }
}