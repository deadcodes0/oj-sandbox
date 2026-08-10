package com.oj.ojbackendmodel.model.codesandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {

    private List<String> inputList;

    /**
     * 单个源码（向后兼容）。若提供了 codeList，则忽略此字段。
     */
    private String code;

    private String language;

    /**
     * 可选多个源码文件：后端可把入口（如 Main.java）与用户代码（如 Solution.java）一起下发。
     */
    private List<CodeFile> codeList;
}
