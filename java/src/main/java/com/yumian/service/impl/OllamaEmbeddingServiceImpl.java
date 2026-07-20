package com.yumian.service.impl;

import com.yumian.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OllamaEmbeddingServiceImpl implements EmbeddingService {

    private final WebClient webClient;

    public OllamaEmbeddingServiceImpl(@Value("${ollama.url}") String ollamaUrl) {
        String baseUrl = ollamaUrl.replace("/api/chat", "");
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public float[] embed(String text) {
        try {
            Map<String, Object> request = Map.of(
                    "model", "bge-m3:latest",
                    "prompt", text
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/api/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Embedding 返回为空");
            }

            @SuppressWarnings("unchecked")
            List<Double> embeddingList = (List<Double>) response.get("embedding");
            if (embeddingList == null || embeddingList.isEmpty()) {
                throw new RuntimeException("Embedding 内容为空");
            }

            float[] result = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                result[i] = embeddingList.get(i).floatValue();
            }
            return result;
        } catch (Exception e) {
            log.error("Embedding 调用失败: {}", e.getMessage());
            throw new RuntimeException("Embedding 调用失败: " + e.getMessage());
        }
    }
}
