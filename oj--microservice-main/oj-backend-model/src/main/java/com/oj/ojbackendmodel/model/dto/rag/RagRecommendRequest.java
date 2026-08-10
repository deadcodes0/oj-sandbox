package com.oj.ojbackendmodel.model.dto.rag;

import lombok.Data;

/**
 * 相似题目推荐请求
 */
@Data
public class RagRecommendRequest {

    /**
     * 目标题目 id
     */
    private Long questionId;

    /**
     * 返回条数（可选，缺省用配置 top-k）
     */
    private Integer topN;
}
