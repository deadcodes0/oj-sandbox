package com.oj.ojcodesandbox.enums;

import cn.hutool.core.util.StrUtil;

/**
 * 编程语言枚举：定义每种语言对应的源文件名、Docker 镜像、容器内编译命令、容器内运行命令
 */
public enum LanguageEnum {

    // 编译命令：源码在只读挂载的 /code，编译产物输出到 /app
    // 运行命令：程序本体，输入由沙箱通过 "程序 < /in/<i>.in" 重定向到 stdin
    JAVA("java", "Main.java", "openjdk:8-alpine", "javac -encoding utf-8 -d /app /code/*.java", "java -cp /app Main"),
    C("c", "Main.c", "gcc:latest", "gcc /code/Main.c -o /app/Main", "/app/Main"),
    CPP("cpp", "Main.cpp", "gcc:latest", "g++ /code/Main.cpp -o /app/Main", "/app/Main");

    private final String value;

    private final String sourceFileName;

    private final String dockerImage;

    private final String compileCmd;

    private final String runCmd;

    LanguageEnum(String value, String sourceFileName, String dockerImage, String compileCmd, String runCmd) {
        this.value = value;
        this.sourceFileName = sourceFileName;
        this.dockerImage = dockerImage;
        this.compileCmd = compileCmd;
        this.runCmd = runCmd;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static LanguageEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (LanguageEnum languageEnum : LanguageEnum.values()) {
            if (languageEnum.value.equals(value)) {
                return languageEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public String getCompileCmd() {
        return compileCmd;
    }

    public String getRunCmd() {
        return runCmd;
    }
}
