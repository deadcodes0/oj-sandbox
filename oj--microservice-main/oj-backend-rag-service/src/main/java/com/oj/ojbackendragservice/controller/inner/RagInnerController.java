package com.oj.ojbackendragservice.controller.inner;

import com.oj.ojbackendmodel.model.dto.rag.RagIndexRequest;
import com.oj.ojbackendragservice.service.RagIndexService;
import com.oj.ojbackendserviceclient.service.RagFeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 该服务仅内部调用，不是给前端的
 */
@RestController
@RequestMapping("/inner")
public class RagInnerController implements RagFeignClient {

    @Resource
    private RagIndexService ragIndexService;
    /**
     * 索引更新
     */
    @PostMapping("/index/upsert")
    @Override
    public boolean upsertIndex(@RequestBody RagIndexRequest request) {
        return ragIndexService.upsert(request);
    }
    /**
     * 索引删除
     */
    @PostMapping("/index/delete")
    @Override
    public boolean deleteIndex(@RequestParam("questionId") long questionId) {
        return ragIndexService.delete(questionId);
    }
}
