package com.oj.ojbackendjudgeservice.judge.special;

import com.oj.ojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.oj.ojbackendjudgeservice.judge.strategy.JudgeConfigUtil;
import com.oj.ojbackendjudgeservice.judge.strategy.JudgeResourceLimitChecker;
import com.oj.ojbackendjudgeservice.judge.strategy.JudgeContext;
import com.oj.ojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.oj.ojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.oj.ojbackendmodel.model.codesandbox.JudgeInfo;
import com.oj.ojbackendmodel.model.dto.question.JudgeCase;
import com.oj.ojbackendmodel.model.dto.question.JudgeConfig;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.enums.JudgeInfoMessageEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 特判（SPJ）执行器
 *
 * <p>约定：每个测试点把「测试输入 + =====SPJ_ANSWER===== + 标准答案 + =====SPJ_OUTPUT===== + 用户实际输出」
 * 拼成特判程序的 stdin；特判程序输出（去空白后）等于 AC（忽略大小写）即通过，否则判 Wrong Answer。
 */
@Component
public class SpecialJudgeExecutor {

    private static final String SPJ_ANSWER_MARKER = "=====SPJ_ANSWER=====";

    private static final String SPJ_OUTPUT_MARKER = "=====SPJ_OUTPUT=====";

    private static final String SPJ_ACCEPTED_OUTPUT = "AC";

    /**
     * 执行特判
     *
     * @param judgeContext 判题上下文（含用户程序输出）
     * @param codeSandbox 代码沙箱
     * @return 判题结果
     */
    public JudgeInfo doJudge(JudgeContext judgeContext, CodeSandbox codeSandbox) {
        Question question = judgeContext.getQuestion();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        List<String> outputList = judgeContext.getOutputList();

        // 1）用户程序资源限制校验
        JudgeConfig judgeConfig = JudgeConfigUtil.parse(question);
        JudgeInfoMessageEnum limitMessage = JudgeResourceLimitChecker.checkLimit(
                judgeContext.getJudgeInfo(), judgeConfig);
        if (limitMessage != null) {
            return buildResult(judgeContext, limitMessage, 0, judgeCaseList == null ? 0 : judgeCaseList.size());
        }
        // 2）特判代码配置校验
        if (question == null || StringUtils.isBlank(question.getSpjCode())) {
            return buildResult(judgeContext, JudgeInfoMessageEnum.SYSTEM_ERROR, 0,
                    judgeCaseList == null ? 0 : judgeCaseList.size());
        }
        if (outputList == null || judgeCaseList == null || outputList.size() != judgeCaseList.size()) {
            return buildResult(judgeContext, JudgeInfoMessageEnum.WRONG_ANSWER, 0,
                    judgeCaseList == null ? 0 : judgeCaseList.size());
        }
        // 3）组装特判输入并调用沙箱
        List<String> spjInputList = new ArrayList<>();
        for (int i = 0; i < judgeCaseList.size(); i++) {
            spjInputList.add(buildSpjInput(judgeCaseList.get(i), outputList.get(i)));
        }
        ExecuteCodeRequest spjRequest = ExecuteCodeRequest.builder()
                .code(question.getSpjCode())
                .language(question.getSpjLanguage())
                .inputList(spjInputList)
                .build();
        ExecuteCodeResponse spjResponse = codeSandbox.executeCode(spjRequest);
        if (spjResponse == null || spjResponse.getOutputList() == null
                || spjResponse.getOutputList().size() != spjInputList.size()) {
            return buildResult(judgeContext, JudgeInfoMessageEnum.SYSTEM_ERROR, 0, spjInputList.size());
        }
        // 4）判定特判结果
        int passed = 0;
        for (String spjOutput : spjResponse.getOutputList()) {
            if (StringUtils.trim(spjOutput).equalsIgnoreCase(SPJ_ACCEPTED_OUTPUT)) {
                passed++;
            }
        }
        JudgeInfoMessageEnum message = passed == spjInputList.size()
                ? JudgeInfoMessageEnum.ACCEPTED
                : JudgeInfoMessageEnum.WRONG_ANSWER;
        return buildResult(judgeContext, message, passed, spjInputList.size());
    }

    private String buildSpjInput(JudgeCase judgeCase, String actualOutput) {
        StringBuilder sb = new StringBuilder();
        sb.append(judgeCase.getInput());
        sb.append("\n").append(SPJ_ANSWER_MARKER).append("\n");
        sb.append(judgeCase.getOutput());
        sb.append("\n").append(SPJ_OUTPUT_MARKER).append("\n");
        sb.append(actualOutput);
        return sb.toString();
    }

    private JudgeInfo buildResult(JudgeContext judgeContext, JudgeInfoMessageEnum message, int passed, int total) {
        JudgeInfo userJudgeInfo = judgeContext.getJudgeInfo();
        Long memory = Optional.ofNullable(userJudgeInfo).map(JudgeInfo::getMemory).orElse(0L);
        Long time = Optional.ofNullable(userJudgeInfo).map(JudgeInfo::getTime).orElse(0L);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(message.getValue());
        judgeInfo.setMemory(memory);
        judgeInfo.setTime(time);
        judgeInfo.setPassedCase(passed);
        judgeInfo.setTotalCase(total);
        return judgeInfo;
    }
}
