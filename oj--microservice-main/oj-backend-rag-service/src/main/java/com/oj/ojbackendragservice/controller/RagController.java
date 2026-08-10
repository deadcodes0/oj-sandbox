package com.oj.ojbackendragservice.controller;

import com.oj.ojbackendcommon.annotation.AuthCheck;
import com.oj.ojbackendcommon.common.BaseResponse;
import com.oj.ojbackendcommon.common.ResultUtils;
import com.oj.ojbackendcommon.constant.UserConstant;
import com.oj.ojbackendmodel.model.dto.rag.RagChatRequest;
import com.oj.ojbackendmodel.model.dto.rag.RagRecommendRequest;
import com.oj.ojbackendmodel.model.dto.rag.RagSearchRequest;
import com.oj.ojbackendmodel.model.vo.RagChatResponse;
import com.oj.ojbackendmodel.model.vo.RagSearchItem;
import com.oj.ojbackendragservice.service.RagChatService;
import com.oj.ojbackendragservice.service.RagIndexService;
import com.oj.ojbackendragservice.service.RagSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * RAG 对外接口
 */
@RestController
@RequestMapping("/")
@Slf4j
public class RagController {

    @Resource
    private RagChatService ragChatService;

    @Resource
    private RagSearchService ragSearchService;

    @Resource
    private RagIndexService ragIndexService;

    /**
     * AI 智能答疑
     */
    @PostMapping("/chat")
    public BaseResponse<RagChatResponse> chat(@RequestBody RagChatRequest request) {
        return ResultUtils.success(ragChatService.chat(request));
    }

    /**
     * 题目语义检索
     */
    @PostMapping("/question/search")
    public BaseResponse<List<RagSearchItem>> search(@RequestBody RagSearchRequest request) {
        return ResultUtils.success(ragSearchService.search(request));
    }

    /**
     * 相似题目推荐
     */
    @PostMapping("/question/recommend")
    public BaseResponse<List<RagSearchItem>> recommend(@RequestBody RagRecommendRequest request) {
        return ResultUtils.success(ragSearchService.recommend(request));
    }

    /**
     * 全量重建索引（管理员）
     */
    @PostMapping("/index/rebuild")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> rebuild() {
        return ResultUtils.success(ragIndexService.rebuild());
    }
}
