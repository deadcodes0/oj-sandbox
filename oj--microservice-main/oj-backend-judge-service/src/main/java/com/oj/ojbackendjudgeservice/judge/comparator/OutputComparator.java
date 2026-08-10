package com.oj.ojbackendjudgeservice.judge.comparator;

import com.oj.ojbackendmodel.model.enums.JudgeInfoMessageEnum;

/**
 * 输出比对器
 */
public interface OutputComparator {

    /**
     * 比对标准输出与实际输出
     *
     * @param expected 标准输出
     * @param actual 实际输出
     * @return ACCEPTED 或 WRONG_ANSWER
     */
    JudgeInfoMessageEnum compare(String expected, String actual);
}
