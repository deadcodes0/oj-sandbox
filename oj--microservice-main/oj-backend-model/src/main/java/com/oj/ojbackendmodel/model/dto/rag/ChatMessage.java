package com.oj.ojbackendmodel.model.dto.rag;

import lombok.Data;

/**
 * 多轮对话消息（role: user / assistant）
 */
@Data
public class ChatMessage {

    /**
     * 角色："user" | "assistant"
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
}
