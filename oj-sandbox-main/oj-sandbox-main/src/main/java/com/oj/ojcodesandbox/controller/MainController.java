package com.oj.ojcodesandbox.controller;

import cn.hutool.core.util.StrUtil;
import com.oj.ojcodesandbox.CodeSandbox;
import com.oj.ojcodesandbox.CodeSandboxFactory;
import com.oj.ojcodesandbox.enums.LanguageEnum;
import com.oj.ojcodesandbox.model.ExecuteCodeRequest;
import com.oj.ojcodesandbox.model.ExecuteCodeResponse;
import com.oj.ojcodesandbox.model.JudgeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

@RestController("/")
public class MainController {

    @Autowired
    private CodeSandboxFactory codeSandboxFactory;

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                    HttpServletResponse response) {
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        if (StrUtil.isBlank(executeCodeRequest.getLanguage())) {
            throw new RuntimeException("编程语言不能为空");
        }
        if (LanguageEnum.getEnumByValue(executeCodeRequest.getLanguage()) == null) {
            // 返回结构化的沙箱错误，便于判题服务识别
            ExecuteCodeResponse errorResponse = new ExecuteCodeResponse();
            errorResponse.setOutputList(new ArrayList<>());
            errorResponse.setMessage("不支持的编程语言：" + executeCodeRequest.getLanguage());
            errorResponse.setStatus(2);
            errorResponse.setJudgeInfo(new JudgeInfo());
            return errorResponse;
        }
        // 根据语言分发到对应的代码沙箱
        CodeSandbox codeSandbox = codeSandboxFactory.newInstance(executeCodeRequest.getLanguage());
        return codeSandbox.executeCode(executeCodeRequest);
    }
}
