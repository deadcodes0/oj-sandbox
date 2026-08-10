package com.oj.ojbackendjudgeservice.judge.special;

import cn.hutool.json.JSONUtil;
import com.oj.ojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.oj.ojbackendjudgeservice.judge.strategy.JudgeContext;
import com.oj.ojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.oj.ojbackendmodel.model.codesandbox.JudgeInfo;
import com.oj.ojbackendmodel.model.dto.question.JudgeCase;
import com.oj.ojbackendmodel.model.dto.question.JudgeConfig;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.entity.QuestionSubmit;
import com.oj.ojbackendmodel.model.enums.JudgeInfoMessageEnum;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecialJudgeExecutorTest {

    private final SpecialJudgeExecutor executor = new SpecialJudgeExecutor();

    @Test
    void allCasesAccepted() {
        JudgeContext context = buildContext("spj-code", "java", buildCases("2", "3"),
                Arrays.asList("2", "3"), 100L, 100L);
        CodeSandbox spjSandbox = request -> {
            ExecuteCodeResponse response = new ExecuteCodeResponse();
            // 忽略大小写 + 容忍尾随空格
            response.setOutputList(Arrays.asList("AC", "ac "));
            return response;
        };
        JudgeInfo result = executor.doJudge(context, spjSandbox);
        assertEquals(JudgeInfoMessageEnum.ACCEPTED.getValue(), result.getMessage());
        assertEquals(2, result.getPassedCase());
        assertEquals(2, result.getTotalCase());
    }

    @Test
    void partialWrongAnswer() {
        JudgeContext context = buildContext("spj-code", "java", buildCases("2", "3"),
                Arrays.asList("2", "3"), 100L, 100L);
        CodeSandbox spjSandbox = request -> {
            ExecuteCodeResponse response = new ExecuteCodeResponse();
            response.setOutputList(Arrays.asList("AC", "WA"));
            return response;
        };
        JudgeInfo result = executor.doJudge(context, spjSandbox);
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER.getValue(), result.getMessage());
        assertEquals(1, result.getPassedCase());
        assertEquals(2, result.getTotalCase());
    }

    @Test
    void spjSandboxReturnsNull() {
        JudgeContext context = buildContext("spj-code", "java", buildCases("2"),
                Collections.singletonList("2"), 100L, 100L);
        CodeSandbox spjSandbox = request -> null;
        JudgeInfo result = executor.doJudge(context, spjSandbox);
        assertEquals(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue(), result.getMessage());
    }

    @Test
    void spjSandboxOutputCountMismatch() {
        JudgeContext context = buildContext("spj-code", "java", buildCases("2"),
                Collections.singletonList("2"), 100L, 100L);
        CodeSandbox spjSandbox = request -> {
            ExecuteCodeResponse response = new ExecuteCodeResponse();
            response.setOutputList(Arrays.asList("AC", "AC"));
            return response;
        };
        JudgeInfo result = executor.doJudge(context, spjSandbox);
        assertEquals(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue(), result.getMessage());
    }

    @Test
    void missingSpjCode() {
        JudgeContext context = buildContext(null, "java", buildCases("2"),
                Collections.singletonList("2"), 100L, 100L);
        JudgeInfo result = executor.doJudge(context, request -> null);
        assertEquals(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue(), result.getMessage());
    }

    @Test
    void userOutputCountMismatch() {
        JudgeContext context = buildContext("spj-code", "java", buildCases("2", "3"),
                Collections.singletonList("2"), 100L, 100L);
        JudgeInfo result = executor.doJudge(context, request -> null);
        assertEquals(JudgeInfoMessageEnum.WRONG_ANSWER.getValue(), result.getMessage());
    }

    @Test
    void timeLimitExceededSkipsSpj() {
        JudgeContext context = buildContext("spj-code", "java", buildCases("2"),
                Collections.singletonList("2"), 100L, 20000L);
        // Java 语言扣除 10s 启动开销后仍超限，特判不应被调用
        CodeSandbox spjSandbox = request -> {
            throw new UnsupportedOperationException("SPJ should not be called after TLE");
        };
        JudgeInfo result = executor.doJudge(context, spjSandbox);
        assertEquals(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue(), result.getMessage());
    }

    @Test
    void memoryLimitExceeded() {
        JudgeContext context = buildContext("spj-code", "java", buildCases("2"),
                Collections.singletonList("2"), 20000L, 100L);
        JudgeInfo result = executor.doJudge(context, request -> null);
        assertEquals(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue(), result.getMessage());
    }

    private JudgeContext buildContext(String spjCode, String spjLanguage, List<JudgeCase> judgeCaseList,
            List<String> outputList, Long memory, Long time) {
        JudgeConfig judgeConfig = new JudgeConfig();
        judgeConfig.setCompareMode("spj");
        judgeConfig.setMemoryLimit(10000L);
        judgeConfig.setTimeLimit(1000L);

        Question question = new Question();
        question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        question.setSpjCode(spjCode);
        question.setSpjLanguage(spjLanguage);

        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setLanguage("java");

        JudgeInfo userJudgeInfo = new JudgeInfo();
        userJudgeInfo.setMemory(memory);
        userJudgeInfo.setTime(time);

        JudgeContext context = new JudgeContext();
        context.setQuestion(question);
        context.setQuestionSubmit(questionSubmit);
        context.setJudgeInfo(userJudgeInfo);
        context.setJudgeCaseList(judgeCaseList);
        context.setOutputList(outputList);
        return context;
    }

    private List<JudgeCase> buildCases(String... outputs) {
        List<JudgeCase> cases = new ArrayList<>();
        for (String output : outputs) {
            JudgeCase judgeCase = new JudgeCase();
            judgeCase.setInput("1");
            judgeCase.setOutput(output);
            cases.add(judgeCase);
        }
        return cases;
    }
}
