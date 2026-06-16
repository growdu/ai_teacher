package com.aiteacher.provider.llm;

import com.aiteacher.provider.ai.ChatRequest;
import com.aiteacher.provider.ai.ChatResponse;
import com.aiteacher.provider.ai.model.ChatChoice;
import com.aiteacher.provider.ai.model.ChatMessage;
import com.aiteacher.provider.ai.model.ProviderType;
import com.aiteacher.provider.ai.model.UsageInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude LLM Provider implementation (Anthropic)
 */
public class ClaudeLLMProvider extends AbstractLLMProvider {

    private final WebClient webClient;

    public ClaudeLLMProvider(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.anthropic.com";
        this.model = model != null ? model : "claude-3-5-sonnet-20240620";
        this.enabled = true;
        this.priority = 9;
        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeaders(headers -> {
                    headers.setContentType("application/json");
                    headers.set("x-api-key", this.apiKey);
                    headers.set("anthropic-version", "2023-06-01");
                })
                .build();
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
    protected Flux<String> doStream(ChatRequest request) {
        Map<String, Object> requestBody = buildRequestBody(request);
        requestBody.put("stream", true);

        return webClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class);
    }

    private Map<String, Object> buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : model);
        
        // Claude uses a different message format
        List<Map<String, Object>> messages = request.getMessages().stream()
                .map(msg -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("role", msg.getRole());
                    m.put("content", msg.getContent());
                    return m;
                })
                .toList();
        body.put("messages", messages);
        
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        } else {
            body.put("max_tokens", 4096);
        }
        
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        
        return body;
    }

    private ChatResponse parseResponse(JsonNode response) {
        ChatResponse.ChatResponseBuilder builder = ChatResponse.builder();
        builder.id(response.path("id").asText());
        builder.model(response.path("model").asText());

        ChatChoice choice = new ChatChoice();
        choice.setIndex(0);
        
        ChatMessage message = new ChatMessage();
        message.setRole("assistant");
        message.setContent(response.path("content").path(0).path("text").asText());
        choice.setMessage(message);
        choice.setFinishReason(response.path("stop_reason").asText());
        
        builder.choices(new ChatChoice[]{choice});

        UsageInfo usage = new UsageInfo();
        usage.setPromptTokens(response.path("usage").path("input_tokens").asInt());
        usage.setCompletionTokens(response.path("usage").path("output_tokens").asInt());
        usage.setTotalTokens(
                response.path("usage").path("input_tokens").asInt() + 
                response.path("usage").path("output_tokens").asInt()
        );
        builder.usage(usage);

        return builder.build();
    }
}