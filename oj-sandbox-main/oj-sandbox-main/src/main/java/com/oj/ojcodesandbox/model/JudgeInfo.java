package com.oj.ojcodesandbox.model;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {

    /**
     * 程序执行信息
     */
    private String message;

    /**
     * 消耗内存（KB），对齐判题配置的 memoryLimit
     */
    private Long memory;

    /**
     * 消耗时间（KB）
     */
    private Long time;
}
