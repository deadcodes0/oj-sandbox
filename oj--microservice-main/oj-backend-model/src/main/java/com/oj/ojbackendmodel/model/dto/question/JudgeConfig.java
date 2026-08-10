package com.oj.ojbackendmodel.model.dto.question;

import lombok.Data;

/**
 * 题目配置
 */
@Data
public class JudgeConfig {

    /**
     * 时间限制（ms）
     */
    private Long timeLimit;

    /**
     * 内存限制（KB）
     */
    private Long memoryLimit;

    /**
     * 堆栈限制（KB）
     */
    private Long stackLimit;

    /**
     * 输出比对模式（standard - 标准比对、strict - 严格比对、spj - 特判），缺省按 standard 处理
     */
    private String compareMode;
}
