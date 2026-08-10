package com.oj.ojbackendjudgeservice.judge.comparator;

import com.oj.ojbackendmodel.model.enums.JudgeInfoMessageEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 标准比对：统一换行符后逐行比较，忽略行末空格与末尾空行
 */
public class StandardOutputComparator implements OutputComparator {

    @Override
    public JudgeInfoMessageEnum compare(String expected, String actual) {
        if (expected == null || actual == null) {
            return JudgeInfoMessageEnum.WRONG_ANSWER;
        }
        List<String> expectedLines = normalizeLines(expected);
        List<String> actualLines = normalizeLines(actual);
        if (expectedLines.size() != actualLines.size()) {
            return JudgeInfoMessageEnum.WRONG_ANSWER;
        }
        for (int i = 0; i < expectedLines.size(); i++) {
            if (!expectedLines.get(i).equals(actualLines.get(i))) {
                return JudgeInfoMessageEnum.WRONG_ANSWER;
            }
        }
        return JudgeInfoMessageEnum.ACCEPTED;
    }

    private List<String> normalizeLines(String output) {
        String normalized = output.replace("\r\n", "\n").replace("\r", "\n");
        List<String> lines = new ArrayList<>(Arrays.asList(normalized.split("\n", -1)));
        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, lines.get(i).replaceAll("\\s+$", ""));
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }
}
