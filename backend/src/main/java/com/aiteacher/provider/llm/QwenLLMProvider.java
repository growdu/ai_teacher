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
 * Qwen LLM Provider implementation (Aliyun)
 */
public class QwenLLMProvider extends AbstractLLMProvider {

    private final WebClient webClient;

    public QwenLLMProvider(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://dashscope.aliyuncs.com";
        this.model = model != null ? model : "qwen-max";
        this.enabled = true;
        this.priority = 8;
        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("Authorization", "Bearer " + this.apiKey);
                })
                .build();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.QWEN;
    }

    @Override
    protected ChatResponse doChat(ChatRequest request) {
        try {
            Map<String, Object> requestBody = buildRequestBody(request);
            
            JsonNode responseNode = webClient.post()
                    .uri("/compatible-mode/v1/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return parseResponse(responseNode);
        } catch (Exception e) {
            throw new RuntimeException("Qwen chat request failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected Flux<String> doStream(ChatRequest request) {
        Map<String, Object> requestBody = buildRequestBody(request);
        requestBody.put("stream", true);

        return webClient.post()
                .uri("/compatible-mode/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class);
    }

    private Map<String, Object> buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : model);
        
        List<Map<String, String>> messages = request.getMessages().stream()
                .map(msg -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("role", msg.getRole());
                    m.put("content", msg.getContent());
                    return m;
                })
                .toList();
        body.put("messages", messages);
        
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        
        return body;
    }

    private ChatResponse parseResponse(JsonNode response) {
        ChatResponse.ChatResponseBuilder builder = ChatResponse.builder();
        builder.id(response.path("id").asText());
        builder.object(response.path("object").asText());
        builder.created(response.path("created").asLong());
        builder.model(response.path("model").asText());

        if (response.has("choices")) {
            ChatChoice[] choices = new ChatChoice[response.path("choices").size()];
            int i = 0;
            for (JsonNode choiceNode : response.path("choices")) {
                ChatChoice choice = new ChatChoice();
                choice.setIndex(choiceNode.path("index").asInt());
                
                ChatMessage message = new ChatMessage();
                message.setRole(choiceNode.path("message").path("role").asText());
                message.setContent(choiceNode.path("message").path("content").asText());
                choice.setMessage(message);
                
                choice.setFinishReason(choiceNode.path("finish_reason").asText());
                choices[i++] = choice;
            }
            builder.choices(choices);
        }

        if (response.has("usage")) {
            UsageInfo usage = new UsageInfo();
            usage.setPromptTokens(response.path("usage").path("prompt_tokens").asInt());
            usage.setCompletionTokens(response.path("usage").path("completion_tokens").asInt());
            usage.setTotalTokens(response.path("usage").path("total_tokens").asInt());
            builder.usage(usage);
        }

        return builder.build();
    }
}