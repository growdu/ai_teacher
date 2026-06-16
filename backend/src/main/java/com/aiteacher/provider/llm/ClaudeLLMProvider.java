package com.aiteacher.provider.llm;

import com.aiteacher.provider.ai.ChatRequest;
import com.aiteacher.provider.ai.ChatResponse;
import com.aiteacher.provider.ai.model.ChatChoice;
import com.aiteacher.provider.ai.model.ChatMessage;
import com.aiteacher.provider.ai.model.ProviderType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Claude LLM Provider implementation using Anthropic API
 */
public class ClaudeLLMProvider extends AbstractLLMProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ClaudeLLMProvider(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.anthropic.com";
        this.model = model != null ? model : "claude-3-5-sonnet-20241022";
        this.enabled = true;
        this.priority = 8;
        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("x-api-key", this.apiKey);
                    headers.set("anthropic-version", "2023-06-01");
                })
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.CLAUDE;
    }

    @Override
    protected ChatResponse doChat(ChatRequest request) {
        try {
            Map<String, Object> requestBody = buildRequestBody(request);
            
            JsonNode responseNode = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return parseResponse(responseNode);
        } catch (Exception e) {
            throw new RuntimeException("Claude chat request failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected List<String> doStream(ChatRequest request) {
        return Collections.emptyList();
    }

    private Map<String, Object> buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : this.model);
        
        List<String> messages = new ArrayList<>();
        if (request.getMessages() != null) {
            for (ChatMessage msg : request.getMessages()) {
                Map<String, String> message = new HashMap<>();
                message.put("role", msg.getRole());
                message.put("content", msg.getContent());
                messages.add(msg.getContent());
            }
        }
        body.put("messages", messages);
        
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        } else {
            body.put("max_tokens", 1024);
        }
        
        return body;
    }

    private ChatResponse parseResponse(JsonNode responseNode) {
        ChatResponse response = new ChatResponse();
        response.setId(responseNode.has("id") ? responseNode.get("id").asText() : "");
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel(responseNode.has("model") ? responseNode.get("model").asText() : "");
        
        ChatChoice choice = new ChatChoice();
        choice.setIndex(0);
        choice.setFinishReason(responseNode.has("stop_reason") ? responseNode.get("stop_reason").asText() : "");
        
        ChatMessage message = new ChatMessage();
        message.setRole("assistant");
        if (responseNode.has("content") && responseNode.get("content").isArray()) {
            String content = responseNode.get("content").get(0).has("text") 
                ? responseNode.get("content").get(0).get("text").asText() 
                : "";
            message.setContent(content);
        }
        choice.setMessage(message);
        
        response.setChoices(new ChatChoice[]{choice});
        return response;
    }
}
