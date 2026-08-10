package com.oj.ojbackendjudgeservice.judge.comparator;

import com.oj.ojbackendmodel.model.enums.CompareMode;
import com.oj.ojbackendmodel.model.enums.JudgeInfoMessageEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputComparatorTest {

    private final OutputComparator standard = new StandardOutputComparator();

    private final OutputComparator strict = new StrictOutputComparator();

    @Test
    void standard_acceptsExactMatch() {
        assertEquals(JudgeInfoMessageEnum.ACCEPTED, standard.compare("1\n2", "1\n2"));
    }

    @Test
    void standard_ignoresTrailingWhitespaceAndBlankLines() {
        assertEquals(JudgeInfoMessageEnum.ACCEPTED, standard.compare("1\n2\n", "1 \n2  \n\n"));
    }

    @Test
    void standard_normalizesLineEndings() {
        assertEquals(JudgeInfoMessageEnum.ACCEPTED, standard.compare("1\r\n2\r\n", "1\n2"));
    }

    @Test
    void standard_rejectsDifferentContent() {
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER, standard.compare("1\n2", "1\n3"));
    }

    @Test
    void standard_rejectsLineCountMismatch() {
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER, standard.compare("1\n2", "1"));
    }

    @Test
    void standard_keepsLeadingWhitespace() {
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER, standard.compare("1\n2", "1\n 2"));
    }

    @Test
    void standard_rejectsNull() {
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER, standard.compare("abc", null));
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER, standard.compare(null, "abc"));
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER, standard.compare(null, null));
    }

    @Test
    void strict_acceptsExactMatch() {
        assertEquals(JudgeInfoMessageEnum.ACCEPTED, strict.compare("1\n2", "1\n2"));
    }

    @Test
    void strict_normalizesLineEndings() {
        assertEquals(JudgeInfoMessageEnum.ACCEPTED, strict.compare("1\r\n2", "1\n2"));
    }

    @Test
    void strict_rejectsTrailingNewlineDifference() {
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER, strict.compare("1\n2\n", "1\n2"));
    }

    @Test
    void strict_rejectsTrailingWhitespaceDifference() {
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER, strict.compare("1 2", "1 2 "));
    }

    @Test
    void strict_rejectsDifferentContent() {
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER, strict.compare("1\n2", "1\n3"));
    }

    @Test
    void strict_rejectsNull() {
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER, strict.compare("abc", null));
    }

    @Test
    void factory_returnsExpectedComparator() {
        assertEquals(JudgeInfoMessageEnum.ACCEPTED,
                OutputComparatorFactory.getComparator(CompareMode.STANDARD).compare("1 ", "1"));
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER,
                OutputComparatorFactory.getComparator(CompareMode.STRICT).compare("1 ", "1"));
        // 未识别模式（含 spj）回退到标准比对
        assertEquals(JudgeInfoMessageEnum.ACCEPTED,
                OutputComparatorFactory.getComparator(CompareMode.SPJ).compare("1 ", "1"));
        assertEquals(JudgeInfoMessageEnum.ACCEPTED,
                OutputComparatorFactory.getComparator(null).compare("1 ", "1"));
    }
}
