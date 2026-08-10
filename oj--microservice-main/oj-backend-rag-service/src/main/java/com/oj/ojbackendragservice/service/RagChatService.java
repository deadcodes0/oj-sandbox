package com.oj.ojbackendragservice.service;

import com.oj.ojbackendcommon.common.ErrorCode;
import com.oj.ojbackendcommon.exception.BusinessException;
import com.oj.ojbackendmodel.model.dto.rag.ChatMessage;
import com.oj.ojbackendmodel.model.dto.rag.RagChatRequest;
import com.oj.ojbackendmodel.model.dto.rag.RagIndexRequest;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.vo.RagChatResponse;
import com.oj.ojbackendmodel.model.vo.RagSearchItem;
import com.oj.ojbackendragservice.client.LlmClient;
import com.oj.ojbackendragservice.client.QdrantClient;
import com.oj.ojbackendragservice.config.RagConfig;
import com.oj.ojbackendserviceclient.service.QuestionFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能答疑：检索相关题目资料 + 云端模型（智谱 GLM）生成
 */
@Slf4j
@Service
public class RagChatService {

    @Resource
    private LlmClient llmClient;

    @Resource
    private QdrantClient qdrantClient;

    @Resource
    private RagConfig ragConfig;

    @Resource
    private QuestionFeignClient questionFeignClient;

    public RagChatResponse chat(RagChatRequest request) {
        if (request == null || StringUtils.isBlank(request.getMessage())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提问内容不能为空");
        }
        // 1）语义检索相关题目资料
        List<Float> vector = llmClient.embed(request.getMessage());
        List<QdrantClient.SearchResult> hits = qdrantClient.search(vector, ragConfig.getSearch().getTopK());
        List<RagSearchItem> references = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        double threshold = ragConfig.getSearch().getSimilarityThreshold();
        for (QdrantClient.SearchResult hit : hits) {
            if (hit.getScore() < threshold) {
                continue;
            }
            references.add(toItem(hit));
            context.append(hit.getPayload().toString()).append("\n\n");
        }
        // 2）若指定了题目，把该题资料单独附加（失败仅降级，不阻断整个对话）
        if (request.getQuestionId() != null) {
            try {
                Question question = questionFeignClient.getQuestionById(request.getQuestionId());
                if (question != null) {
                    RagIndexRequest idx = new RagIndexRequest();
                    idx.setQuestionId(question.getId());
                    idx.setTitle(question.getTitle());
                    idx.setContent(question.getContent());
                    idx.setTags(question.getTags());
                    idx.setAnswer(question.getAnswer());
                    context.append(RagIndexService.buildChunk(idx)).append("\n\n");
                }
            } catch (Exception e) {
                log.warn("拉取题目详情用于 AI 上下文失败，questionId = {}，降级为仅检索上下文。{}",
                        request.getQuestionId(), e.toString());
            }
        }
        // 3）组装消息并调用云端模型（智谱 GLM）
        String systemPrompt = "你是 OJ 在线评测平台的智能解题助手。请基于下面提供的题目资料回答用户的问题，"
                + "给出思路讲解或代码提示；如果资料不足以回答，请如实说明，不要编造。\n\n"
                + "[题目资料]\n"
                + (context.length() == 0 ? "（无相关题目资料）" : context.toString());
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new HashMap<>();
        system.put("role", "system");
        system.put("content", systemPrompt);
        messages.add(system);
        // 多轮对话历史（只保留最近 12 条，避免 messages 无限膨胀），原样透传给模型
        List<ChatMessage> history = request.getMessages();
        if (history != null && !history.isEmpty()) {
            int from = Math.max(0, history.size() - 12);
            for (int i = from; i < history.size(); i++) {
                ChatMessage turn = history.get(i);
                if (turn == null || StringUtils.isBlank(turn.getRole()) || turn.getContent() == null) {
                    continue;
                }
                Map<String, String> msg = new HashMap<>();
                msg.put("role", turn.getRole());
                msg.put("content", turn.getContent());
                messages.add(msg);
            }
        }
        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", request.getMessage());
        messages.add(user);

        RagChatResponse response = new RagChatResponse();
        response.setContent(llmClient.chat(messages));
        response.setReferences(references);
        return response;
    }

    private RagSearchItem toItem(QdrantClient.SearchResult hit) {
        RagSearchItem item = new RagSearchItem();
        item.setQuestionId(hit.getId());
        item.setTitle(hit.getPayload().has("title") ? hit.getPayload().get("title").getAsString() : null);
        item.setScore(hit.getScore());
        return item;
    }
}
