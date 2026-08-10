package com.oj.ojbackendragservice.service;

import com.oj.ojbackendcommon.common.ErrorCode;
import com.oj.ojbackendcommon.exception.BusinessException;
import com.oj.ojbackendmodel.model.dto.rag.RagIndexRequest;
import com.oj.ojbackendmodel.model.dto.rag.RagRecommendRequest;
import com.oj.ojbackendmodel.model.dto.rag.RagSearchRequest;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.vo.RagSearchItem;
import com.oj.ojbackendragservice.client.LlmClient;
import com.oj.ojbackendragservice.client.QdrantClient;
import com.oj.ojbackendragservice.config.RagConfig;
import com.oj.ojbackendserviceclient.service.QuestionFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 题目语义检索 / 相似推荐
 */
@Service
public class RagSearchService {

    @Resource
    private LlmClient llmClient;

    @Resource
    private QdrantClient qdrantClient;

    @Resource
    private RagConfig ragConfig;

    @Resource
    private QuestionFeignClient questionFeignClient;

    /**
     * 按关键词语义检索题目
     */
    public List<RagSearchItem> search(RagSearchRequest request) {
        if (request == null || StringUtils.isBlank(request.getKeyword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "检索关键词不能为空");
        }
        int topN = resolveTopN(request.getTopN());
        List<Float> vector = llmClient.embed(request.getKeyword());
        return toSearchItems(vector, topN, -1L);
    }

    /**
     * 相似题目推荐（排除自身）
     */
    public List<RagSearchItem> recommend(RagRecommendRequest request) {
        if (request == null || request.getQuestionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目 id 不能为空");
        }
        Question question = questionFeignClient.getQuestionById(request.getQuestionId());
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        RagIndexRequest idx = new RagIndexRequest();
        idx.setQuestionId(question.getId());
        idx.setTitle(question.getTitle());
        idx.setContent(question.getContent());
        idx.setTags(question.getTags());
        idx.setAnswer(question.getAnswer());
        int topN = resolveTopN(request.getTopN());
        List<Float> vector = llmClient.embed(RagIndexService.buildChunk(idx));
        return toSearchItems(vector, topN, request.getQuestionId());
    }

    private int resolveTopN(Integer topN) {
        return topN == null || topN <= 0 ? ragConfig.getSearch().getTopK() : topN;
    }

    private List<RagSearchItem> toSearchItems(List<Float> vector, int topN, long excludeId) {
        List<RagSearchItem> result = new ArrayList<>();
        List<QdrantClient.SearchResult> hits = qdrantClient.search(vector, topN + (excludeId > 0 ? 1 : 0));
        double threshold = ragConfig.getSearch().getSimilarityThreshold();
        for (QdrantClient.SearchResult hit : hits) {
            if (hit.getId() == excludeId) {
                continue;
            }
            if (hit.getScore() < threshold) {
                continue;
            }
            RagSearchItem item = new RagSearchItem();
            item.setQuestionId(hit.getId());
            item.setTitle(hit.getPayload().has("title") ? hit.getPayload().get("title").getAsString() : null);
            item.setScore(hit.getScore());
            result.add(item);
        }
        return result;
    }
}
