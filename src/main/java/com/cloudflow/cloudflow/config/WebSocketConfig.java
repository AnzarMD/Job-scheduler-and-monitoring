package com.cloudflow.cloudflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
// @EnableWebSocketMessageBroker activates the full STOMP message broker infrastructure.
// Without this, the WebSocket config is ignored.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for topics prefixed with /topic
        // When we call convertAndSend("/topic/executions/tenantId", payload),
        // the broker routes it to all subscribers of that topic
        config.enableSimpleBroker("/topic");

        // Prefix for messages sent FROM clients TO the server (not used in Day 9
        // since we only push server→client, but needed for full STOMP setup)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the WebSocket endpoint at /ws
        // Clients connect to: ws://localhost:8080/ws
        // withSockJS() adds SockJS fallback for browsers that don't support
        // native WebSocket (older browsers, some corporate proxies).
        // SockJS tries WebSocket first, then falls back to HTTP long-polling.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow all origins in dev
                .withSockJS();
    }
}