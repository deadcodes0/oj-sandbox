package com.oj.ojbackendragservice.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QdrantClientTest {

    @Test
    void parseSearchResult_extractsPoints() {
        String json = "{\"result\":[{\"id\":101,\"score\":0.87,\"payload\":{\"title\":\"二分查找\",\"questionId\":101}}],\"status\":\"ok\"}";
        List<QdrantClient.SearchResult> results = QdrantClient.parseSearchResult(json);
        assertEquals(1, results.size());
        assertEquals(101L, results.get(0).getId());
        assertEquals(0.87, results.get(0).getScore());
        assertEquals("二分查找", results.get(0).getPayload().get("title").getAsString());
    }

    @Test
    void parseSearchResult_emptyResult() {
        String json = "{\"result\":[],\"status\":\"ok\"}";
        assertTrue(QdrantClient.parseSearchResult(json).isEmpty());
    }
}
