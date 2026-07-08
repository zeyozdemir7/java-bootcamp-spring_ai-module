package com.accenture.springai_bootcamp_demo.client;

import com.accenture.springai_bootcamp_demo.config.OpenRouterProperties;
import com.accenture.springai_bootcamp_demo.entity.ChatMessage;
import com.accenture.springai_bootcamp_demo.entity.Role;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Thin client over the OpenRouter chat completions API, backed by Spring AI's
 * {@link ChatClient}. Keeps the public surface intentionally small: callers
 * hand over the conversation history and receive the assistant's reply text.
 */
@Slf4j
@Component
public class OpenRouterClient {

    private final ChatClient chatClient;
    private final String apiKey;
    private final OpenRouterProperties properties;

    public OpenRouterClient(ChatClient.Builder chatClientBuilder, @Value("${spring.ai.openai.api-key:}") String apiKey, OpenRouterProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.apiKey = apiKey;
        this.properties = properties;
    }


    public String complete(List<ChatMessage> history) {
        requireApiKey();
        String reply = call(history);
        return extractContent(reply);
    }

    private String call(List<ChatMessage> history) {
        try {
            return chatClient.prompt()
                    .messages(toSpringAiMessages(history))
                    .call()
                    .content();
        } catch (RuntimeException ex) {
            log.error("OpenRouter request failed", ex);
            throw new OpenRouterException("Failed to reach OpenRouter: " + ex.getMessage(), ex);
        }
    }


    private List<org.springframework.ai.chat.messages.Message> toSpringAiMessages(List<ChatMessage> history) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(properties.systemPrompt())) {
            messages.add(new SystemMessage(properties.systemPrompt()));
        }
        history.forEach(m -> messages.add(
                m.getRole() == Role.USER
                        ? new UserMessage(m.getContent())
                        : new AssistantMessage(m.getContent())));
        return messages;
    }


    private String extractContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new OpenRouterException("OpenRouter returned an empty response");
        }
        return content.trim();
    }

    private void requireApiKey() {

        if (!StringUtils.hasText(apiKey)){
            throw new OpenRouterException("OPENROUTER_API_KEY is not set.");
        }
    }
}
