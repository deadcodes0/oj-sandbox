package com.oj.ojbackendmodel.model.dto.rag;

import lombok.Data;

import java.util.List;

/**
 * AI 智能答疑请求
 */
@Data
public class RagChatRequest {

    /**
     * 用户提问内容（最新一问）
     */
    private String message;

    /**
     * 锁定题目 id（可选，携带时优先基于该题资料作答）
     */
    private Long questionId;

    /**
     * 多轮对话历史（可选，按时间顺序 role∈{user,assistant}）；携带时与当前 message 一起作为上下文提交模型
     */
    private List<ChatMessage> messages;
}
