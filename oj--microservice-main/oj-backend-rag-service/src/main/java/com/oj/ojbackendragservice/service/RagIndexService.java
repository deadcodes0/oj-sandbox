package com.oj.ojbackendragservice.service;

import com.oj.ojbackendmodel.model.dto.rag.RagIndexRequest;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendragservice.client.LlmClient;
import com.oj.ojbackendragservice.client.QdrantClient;
import com.oj.ojbackendserviceclient.service.QuestionFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 索引维护（写入 / 删除 / 全量重建）
 */
@Slf4j
@Service
public class RagIndexService {

    @Resource
    private LlmClient llmClient;

    @Resource
    private QdrantClient qdrantClient;

    @Resource
    private QuestionFeignClient questionFeignClient;

    /**
     * 写入/更新单个题目的向量索引
     */
    public boolean upsert(RagIndexRequest request) {
        if (request == null || request.getQuestionId() == null) {
            return false;
        }
        String chunk = buildChunk(request);
        List<Float> vector = llmClient.embed(chunk);
        Map<String, Object> payload = new HashMap<>();
        payload.put("questionId", request.getQuestionId());
        payload.put("title", request.getTitle());
        payload.put("content", request.getContent());
        payload.put("tags", request.getTags());
        payload.put("answer", request.getAnswer());
        qdrantClient.upsertPoint(request.getQuestionId(), vector, payload);
        return true;
    }

    /**
     * 删除单个题目的向量索引
     */
    public boolean delete(long questionId) {
        qdrantClient.deletePoint(questionId);
        return true;
    }

    /**
     * 全量重建索引：清空集合后从题目服务拉取全量题目重建
     */
    public boolean rebuild() {
        List<Question> questions = questionFeignClient.listAllQuestions();
        qdrantClient.deleteCollection();
        qdrantClient.ensureCollection();
        if (questions == null || questions.isEmpty()) {
            return true;
        }
        for (Question question : questions) {
            RagIndexRequest request = new RagIndexRequest();
            request.setQuestionId(question.getId());
            request.setTitle(question.getTitle());
            request.setContent(question.getContent());
            request.setTags(question.getTags());
            request.setAnswer(question.getAnswer());
            upsert(request);
        }
        log.info("RAG 索引重建完成，共写入 {} 个题目", questions.size());
        return true;
    }

    /**
     * 拼装单条题目的检索块文本
     */
    public static String buildChunk(RagIndexRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getTitle() != null) {
            sb.append("标题：").append(request.getTitle()).append('\n');
        }
        if (request.getTags() != null) {
            sb.append("标签：").append(request.getTags()).append('\n');
        }
        if (request.getContent() != null) {
            sb.append("题目内容：\n").append(request.getContent()).append('\n');
        }
        if (request.getAnswer() != null) {
            sb.append("答案：\n").append(request.getAnswer()).append('\n');
        }
        return sb.toString();
    }
}
