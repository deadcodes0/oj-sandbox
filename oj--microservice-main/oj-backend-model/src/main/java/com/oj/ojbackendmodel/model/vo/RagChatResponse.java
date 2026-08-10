package com.oj.ojbackendmodel.model.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 智能答疑响应
 */
@Data
public class RagChatResponse {

    /**
     * 生成的回答内容
     */
    private String content;

    /**
     * 参考的题目列表
     */
    private List<RagSearchItem> references;
}
