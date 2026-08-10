package com.oj.ojbackendserviceclient.service;

import com.oj.ojbackendmodel.model.dto.rag.RagIndexRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * rag 服务（索引同步）
 */
@FeignClient(name = "oj-backend-rag-service", path = "/api/rag/inner")
public interface RagFeignClient {

    /**
     * 写入/更新题目索引
     *
     * @param request 题目信息
     * @return 是否成功
     */
    @PostMapping("/index/upsert")
    boolean upsertIndex(@RequestBody RagIndexRequest request);

    /**
     * 删除题目索引
     *
     * @param questionId 题目 id
     * @return 是否成功
     */
    @PostMapping("/index/delete")
    boolean deleteIndex(@RequestParam("questionId") long questionId);
}
