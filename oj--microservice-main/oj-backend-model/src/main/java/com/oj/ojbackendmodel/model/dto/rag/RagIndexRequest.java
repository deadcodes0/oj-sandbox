package com.oj.ojbackendmodel.model.dto.rag;

import lombok.Data;

/**
 * RAG 索引同步请求（题目服务 → rag 服务内网调用）
 */
@Data
public class RagIndexRequest {

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表（json 数组字符串）
     */
    private String tags;

    /**
     * 题目答案
     */
    private String answer;
}
