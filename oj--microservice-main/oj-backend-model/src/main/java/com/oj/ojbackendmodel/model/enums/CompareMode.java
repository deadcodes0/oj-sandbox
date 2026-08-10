package com.oj.ojbackendmodel.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 输出比对模式枚举
 */
public enum CompareMode {

    STANDARD("standard", "标准比对（忽略行末空格与末尾换行）"),
    STRICT("strict", "严格比对（逐字符完全一致）"),
    SPJ("spj", "特判（调用特判程序判定）");

    private final String value;

    private final String text;

    CompareMode(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 获取值列表
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(CompareMode::getValue).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举，未匹配返回 null
     */
    public static CompareMode getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (CompareMode compareMode : CompareMode.values()) {
            if (compareMode.value.equals(value)) {
                return compareMode;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
