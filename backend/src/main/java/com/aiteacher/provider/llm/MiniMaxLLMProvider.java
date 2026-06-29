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
 * MiniMax LLM Provider implementation
 * API Docs: https://www.minimaxi.com/document/Guides/Quickstart
 */
public class MiniMaxLLMProvider extends AbstractLLMProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public MiniMaxLLMProvider(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "https://api.minimax.chat/v1";
        this.model = model != null ? model : "MiniMax-Text-01";
        this.enabled = true;
        this.priority = 12;
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
        return ProviderType.MINIMAX;
    }

    @Override
    protected ChatResponse doChat(ChatRequest request) {
        try {
            Map<String, Object> requestBody = buildRequestBody(request);

            JsonNode responseNode = webClient.post()
                    .uri("/text/chatcompletion_v2")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return parseResponse(responseNode);
        } catch (Exception e) {
            throw new RuntimeException("MiniMax chat request failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected List<String> doStream(ChatRequest request) {
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
        // Always set tokens_to_generate to avoid MiniMax default truncation (default ~8192 tokens
        // is often insufficient for 27+ slide JSON responses). Use maxTokens from request, or 32000.
        int tokens = request.getMaxTokens() != null ? request.getMaxTokens() : 32000;
        body.put("tokens_to_generate", tokens);

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