package com.oj.ojbackendjudgeservice.judge.comparator;

import com.oj.ojbackendmodel.model.enums.CompareMode;

/**
 * 输出比对器工厂
 */
public class OutputComparatorFactory {

    private static final OutputComparator STANDARD = new StandardOutputComparator();

    private static final OutputComparator STRICT = new StrictOutputComparator();

    private OutputComparatorFactory() {
    }

    /**
     * 根据比对模式获取比对器，未识别模式（含 spj）回退到标准比对
     *
     * @param compareMode 比对模式
     * @return
     */
    public static OutputComparator getComparator(CompareMode compareMode) {
        if (compareMode == CompareMode.STRICT) {
            return STRICT;
        }
        return STANDARD;
    }
}
