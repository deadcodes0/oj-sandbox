package com.oj.ojbackendragservice.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmClientTest {

    @Test
    void parseEmbedding_extractsVector() {
        String json = "{\"data\":[{\"embedding\":[0.1,0.2,0.3],\"index\":0,\"object\":\"embedding\"}],\"model\":\"embedding-2\"}";
        List<Float> vector = LlmClient.parseEmbedding(json);
        assertEquals(3, vector.size());
        assertEquals(0.1f, vector.get(0));
        assertEquals(0.3f, vector.get(2));
    }

    @Test
    void parseChatContent_extractsContent() {
        String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"你好，这是解答\"},\"index\":0}]}";
        assertEquals("你好，这是解答", LlmClient.parseChatContent(json));
    }
}
