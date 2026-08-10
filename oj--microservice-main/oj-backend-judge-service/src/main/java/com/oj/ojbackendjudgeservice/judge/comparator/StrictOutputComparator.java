package com.oj.ojbackendjudgeservice.judge.comparator;

import com.oj.ojbackendmodel.model.enums.JudgeInfoMessageEnum;

/**
 * 严格比对：统一换行符后整体字符串逐字符完全一致
 */
public class StrictOutputComparator implements OutputComparator {

    @Override
    public JudgeInfoMessageEnum compare(String expected, String actual) {
        if (expected == null || actual == null) {
            return JudgeInfoMessageEnum.WRONG_ANSWER;
        }
        String expectedNormalized = expected.replace("\r\n", "\n").replace("\r", "\n");
        String actualNormalized = actual.replace("\r\n", "\n").replace("\r", "\n");
        if (expectedNormalized.equals(actualNormalized)) {
            return JudgeInfoMessageEnum.ACCEPTED;
        }
        return JudgeInfoMessageEnum.WRONG_ANSWER;
    }
}
