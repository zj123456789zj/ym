package com.yumian.service.impl;

import com.yumian.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
public class OllamaAiService implements AiService {

    private final WebClient webClient;
    private final String model;

    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(180);

    public OllamaAiService(
            @Value("${ollama.url}") String url,
            @Value("${ollama.model}") String model) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(RESPONSE_TIMEOUT);
        this.webClient = WebClient.builder()
                .baseUrl(url)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.model = model;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "stream", false
        );
        try {
            Map response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map<String, Object> message = (Map<String, Object>) response.get("message");
            if (message != null) {
                return (String) message.get("content");
            }
            return "AI 返回为空";
        } catch (Exception e) {
            return "AI 调用失败: " + e.getMessage();
        }
    }
}
