package com.oj.ojbackendragservice.service;

import com.oj.ojbackendmodel.model.dto.rag.RagIndexRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagIndexServiceTest {

    @Test
    void buildChunk_containsAllSections() {
        RagIndexRequest request = new RagIndexRequest();
        request.setQuestionId(1L);
        request.setTitle("二分查找");
        request.setTags("[\"数组\",\"二分\"]");
        request.setContent("给定一个有序数组，查找目标值。");
        request.setAnswer("使用二分查找。");
        String chunk = RagIndexService.buildChunk(request);
        assertTrue(chunk.contains("标题：二分查找"));
        assertTrue(chunk.contains("标签："));
        assertTrue(chunk.contains("题目内容："));
        assertTrue(chunk.contains("答案："));
    }

    @Test
    void buildChunk_nullFieldsSafe() {
        RagIndexRequest request = new RagIndexRequest();
        request.setQuestionId(1L);
        request.setTitle("标题");
        assertEquals("标题：标题\n", RagIndexService.buildChunk(request));
    }
}
