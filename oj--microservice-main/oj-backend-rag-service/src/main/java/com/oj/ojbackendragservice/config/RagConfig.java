package com.oj.ojbackendragservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "oj.rag")
public class RagConfig {

    /**
     * Qdrant 向量库配置
     */
    private QdrantConfig qdrant = new QdrantConfig();

    /**
     * 大模型服务配置（OpenAI 兼容）
     */
    private LlmConfig llm = new LlmConfig();

    /**
     * 检索配置
     */
    private SearchConfig search = new SearchConfig();

    @Data
    public static class QdrantConfig {
        private String host = "localhost";
        private int port = 6333;
        private String collectionName = "question_knowledge";
        private int dimension = 1024;
    }

    @Data
    public static class LlmConfig {
        private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";
        private String chatModel = "glm-4-flash";
        private String embeddingModel = "embedding-2";
        private String apiKey = "fae4fed4d8414ce0877b55b22a755de6.jgcK66iCb859Qe0M";
        private int timeout = 60000;
    }

    @Data
    public static class SearchConfig {
        private int topK = 5;
        private double similarityThreshold = 0.5;
    }
}
