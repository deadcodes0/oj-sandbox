package com.oj.ojbackendmodel.model.vo;

import lombok.Data;

/**
 * RAG 检索结果条目（不暴露题目答案等敏感字段）
 */
@Data
public class RagSearchItem {

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 标题
     */
    private String title;

    /**
     * 相似度得分
     */
    private Double score;
}
