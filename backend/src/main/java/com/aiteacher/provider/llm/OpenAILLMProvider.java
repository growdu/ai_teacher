package com.aiteacher.provider.llm;

import com.aiteacher.provider.ai.ChatRequest;
import com.aiteacher.provider.ai.ChatResponse;
import com.aiteacher.provider.ai.model.ChatChoice;
import com.aiteacher.provider.ai.model.ChatMessage;
import com.aiteacher.provider.ai.model.ProviderType;
import com.aiteacher.provider.ai.model.UsageInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * OpenAI LLM Provider implementation
 */
public class OpenAILLMProvider extends AbstractLLMProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OpenAILLMProvider(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.openai.com";
        this.model = model != null ? model : "gpt-4o";
        this.enabled = true;
        this.priority = 10;
        this.webClient = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("Authorization", "Bearer " + this.apiKey);
                })
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.OPENAI;
    }

    @Override
    protected ChatResponse doChat(ChatRequest request) {
        try {
            Map<String, Object> requestBody = buildRequestBody(request);
            
            JsonNode responseNode = webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return parseResponse(responseNode);
        } catch (Exception e) {
            throw new RuntimeException("OpenAI chat request failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected List<String> doStream(ChatRequest request) {
        // Simplified - return empty list for now
        return Collections.emptyList();
    }

    private Map<String, Object> buildRequestBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : this.model);
        
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getMessages() != null) {
            for (ChatMessage msg : request.getMessages()) {
                Map<String, String> message = new HashMap<>();
                message.put("role", msg.getRole());
                message.put("content", msg.getContent());
                messages.add(message);
            }
        }
        body.put("messages", messages);
        
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        
        return body;
    }

    private ChatResponse parseResponse(JsonNode responseNode) {
        ChatResponse response = new ChatResponse();
        response.setId(responseNode.has("id") ? responseNode.get("id").asText() : "");
        response.setObject(responseNode.has("object") ? responseNode.get("object").asText() : "");
        response.setCreated(responseNode.has("created") ? responseNode.get("created").asLong() : 0);
        response.setModel(responseNode.has("model") ? responseNode.get("model").asText() : "");
        
        if (responseNode.has("choices") && responseNode.get("choices").isArray()) {
            ChatChoice[] choices = new ChatChoice[responseNode.get("choices").size()];
            for (int i = 0; i < responseNode.get("choices").size(); i++) {
                JsonNode choiceNode = responseNode.get("choices").get(i);
                ChatChoice choice = new ChatChoice();
                choice.setIndex(choiceNode.has("index") ? choiceNode.get("index").asInt() : 0);
                choice.setFinishReason(choiceNode.has("finish_reason") ? choiceNode.get("finish_reason").asText() : "");
                
                if (choiceNode.has("message")) {
                    ChatMessage message = new ChatMessage();
                    message.setRole(choiceNode.get("message").has("role") ? choiceNode.get("message").get("role").asText() : "");
                    message.setContent(choiceNode.get("message").has("content") ? choiceNode.get("message").get("content").asText() : "");
                    choice.setMessage(message);
                }
                choices[i] = choice;
            }
            response.setChoices(choices);
        }
        
        return response;
    }
}
