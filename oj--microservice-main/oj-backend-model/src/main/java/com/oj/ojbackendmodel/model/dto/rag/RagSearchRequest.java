package com.oj.ojbackendmodel.model.dto.rag;

import lombok.Data;

/**
 * 题目语义检索请求
 */
@Data
public class RagSearchRequest {

    /**
     * 检索关键词
     */
    private String keyword;

    /**
     * 返回条数（可选，缺省用配置 top-k）
     */
    private Integer topN;
}
