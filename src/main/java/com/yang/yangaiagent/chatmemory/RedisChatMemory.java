package com.yang.yangaiagent.chatmemory;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis 实现AI持久化记忆
 */
@Component
public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "chat:memory:";

    private static final long TTL_HOURS = 24 * 7;

    public RedisChatMemory (StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = stringRedisTemplate;
    }

    /**
     * 添加消息
     *
     * @param conversationId 会话 ID
     * @param messages       消息列表
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        //获取历史消息
        List<Message> messageList = getOrCreateConversation(conversationId);
        messageList.addAll(messages);
        //保存消息
        saveConversation(conversationId, messageList);
    }

    /**
     * 获取最后几条消息
     * @param conversationId 会话 ID
     * @param lastN  最后几条
     * @return 消息列表
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        //获取历史消息
        List<Message> messageList = getOrCreateConversation(conversationId);

        if (messageList.isEmpty()) {
            return new ArrayList<>();
        }

        return messageList.stream()
                .skip(Math.max(0, messageList.size()) - lastN)
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        String key = getConversationKey(conversationId);
        redisTemplate.delete(key);
    }


    /**
     * 获取会话消息
     */
    private List<Message> getOrCreateConversation(String conversationId) {
        String conversationKey = getConversationKey(conversationId);
        //从Redis里获取 message json
        String messageJson = redisTemplate.opsForValue().get(conversationKey);
        if (StrUtil.isBlank(messageJson)){
            return new ArrayList<>();
        }
        //反序列化
        return deserializeMessages(messageJson);
    }


    /**
     * 保存会话消息
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        String conversationKey = getConversationKey(conversationId);
        // 将消息列表转换为 JSON
        String json = serializeMessages(messages);
        // 保存到 Redis
        redisTemplate.opsForValue().set(conversationKey, json);
        //对于不经常访问的消息 设置七天后自动删除
        redisTemplate.expire(conversationKey, TTL_HOURS, TimeUnit.HOURS);
    }

    private String getConversationKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    /**
     * 将消息列表序列化为 JSON
     */
    private String serializeMessages(List<Message> messages) {
        JSONArray array = JSONUtil.createArray();

        for (Message message : messages) {
            JSONObject json = JSONUtil.createObj()
                    .set("messageType", message.getMessageType().name())
                    .set("text", message.getText());

            // 保存 metadata
            if (message.getMetadata() != null
                    && !message.getMetadata().isEmpty()) {
                json.set("metadata", message.getMetadata());
            }

            array.add(json);
        }

        return array.toString();
    }

    /**
     * 将 JSON 反序列化为 Message 列表
     */
    private List<Message> deserializeMessages(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }

        JSONArray array = JSONUtil.parseArray(json);
        List<Message> messages = new ArrayList<>();

        for (Object item : array) {
            Message message = deserializeMessage(item.toString());

            if (message != null) {
                messages.add(message);
            }
        }

        return messages;
    }

    /**
     * 将 JSON 反序列化为 Message
     */
    private Message deserializeMessage(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }

        JSONObject jsonObject = JSONUtil.parseObj(json);

        String messageType = jsonObject.getStr("messageType");
        String text = jsonObject.getStr("text");

        if (messageType == null || text == null) {
            return null;
        }

        // 根据消息类型创建具体的 Message 对象
        return switch (messageType) {
            case "USER" -> new UserMessage(text);
            case "ASSISTANT" -> new AssistantMessage(text);
            case "SYSTEM" -> new SystemMessage(text);
            default -> null;
        };
    }

}


