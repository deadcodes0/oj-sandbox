package com.oj.ojcodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个源码文件：文件名 + 内容。用于一次提交多个文件（如 Main.java + Solution.java）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeFile {

    private String name;

    private String content;
}