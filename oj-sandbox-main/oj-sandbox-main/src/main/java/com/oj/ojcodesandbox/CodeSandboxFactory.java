package com.oj.ojcodesandbox;

import org.springframework.stereotype.Component;

/**
 * 代码沙箱工厂：根据语言返回对应的沙箱实例（参考 judge-service 的 CodeSandboxFactory）。
 * 语言校验在沙箱内部完成（不支持的编程语言会返回 status=2 的结构化错误）。
 */
@Component
public class CodeSandboxFactory {

    private final DockerCodeSandbox dockerCodeSandbox;

    public CodeSandboxFactory(DockerCodeSandbox dockerCodeSandbox) {
        this.dockerCodeSandbox = dockerCodeSandbox;
    }

    /**
     * 根据语言返回代码沙箱实例
     *
     * @param language 编程语言
     * @return
     */
    public CodeSandbox newInstance(String language) {
        // 目前所有语言统一走 Docker 沙箱，未来可按语言扩展其它实现
        return dockerCodeSandbox;
    }
}
