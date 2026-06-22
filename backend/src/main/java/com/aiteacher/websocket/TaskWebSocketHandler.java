package com.aiteacher.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TaskWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> taskSubscriptions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message: {}", payload);

        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");

            if ("subscribe".equals(type)) {
                String taskId = (String) data.get("taskId");
                taskSubscriptions.put(session.getId(), taskId);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        Map.of("type", "subscribed", "taskId", taskId)
                )));
            } else if ("unsubscribe".equals(type)) {
                taskSubscriptions.remove(session.getId());
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        taskSubscriptions.remove(session.getId());
        log.info("WebSocket connection closed: {}", session.getId());
    }

    public void sendTaskUpdate(String taskId, String status, Integer progress, String message) {
        Map<String, Object> update = new ConcurrentHashMap<>();
        update.put("type", "taskUpdate");
        update.put("taskId", taskId);
        update.put("status", status);
        if (progress != null) update.put("progress", progress);
        if (message != null) update.put("message", message);

        sessions.forEach((sessionId, session) -> {
            String subscribedTaskId = taskSubscriptions.get(sessionId);
            if (taskId.equals(subscribedTaskId)) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(update)));
                } catch (IOException e) {
                    log.error("Error sending message to session {}", sessionId, e);
                }
            }
        });
    }

    public void broadcast(String message) {
        sessions.forEach((sessionId, session) -> {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("Error broadcasting message", e);
            }
        });
    }
}