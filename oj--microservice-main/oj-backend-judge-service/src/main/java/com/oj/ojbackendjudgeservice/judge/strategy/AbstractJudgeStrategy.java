package com.oj.ojbackendjudgeservice.judge.strategy;

import com.oj.ojbackendjudgeservice.judge.comparator.OutputComparator;
import com.oj.ojbackendjudgeservice.judge.comparator.OutputComparatorFactory;
import com.oj.ojbackendmodel.model.codesandbox.JudgeInfo;
import com.oj.ojbackendmodel.model.dto.question.JudgeCase;
import com.oj.ojbackendmodel.model.dto.question.JudgeConfig;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.enums.JudgeInfoMessageEnum;

import java.util.List;
import java.util.Optional;

/**
 * 判题策略抽象骨架：输出比对 + 资源限制校验
 */
public abstract class AbstractJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeInfo userJudgeInfo = judgeContext.getJudgeInfo();
        Long memory = Optional.ofNullable(userJudgeInfo).map(JudgeInfo::getMemory).orElse(0L);
        Long time = Optional.ofNullable(userJudgeInfo).map(JudgeInfo::getTime).orElse(0L);

        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);

        List<String> outputList = judgeContext.getOutputList();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        int total = judgeCaseList == null ? 0 : judgeCaseList.size();
        // 1）输出数量校验（输出缺失或与用例数不一致直接判 WA）
        if (outputList == null || judgeCaseList == null || outputList.size() != judgeCaseList.size()) {
            return buildResult(judgeInfoResponse, JudgeInfoMessageEnum.WRONG_ANSWER, 0, total);
        }
        // 2）逐用例比对
        OutputComparator comparator = OutputComparatorFactory.getComparator(
                JudgeConfigUtil.resolveCompareMode(judgeContext.getQuestion()));
        int passed = 0;
        for (int i = 0; i < judgeCaseList.size(); i++) {
            String expected = judgeCaseList.get(i).getOutput();
            String actual = outputList.get(i);
            if (comparator.compare(expected, actual) == JudgeInfoMessageEnum.ACCEPTED) {
                passed++;
            }
        }
        if (passed < total) {
            return buildResult(judgeInfoResponse, JudgeInfoMessageEnum.WRONG_ANSWER, passed, total);
        }
        // 3）资源限制校验
        Question question = judgeContext.getQuestion();
        JudgeConfig judgeConfig = JudgeConfigUtil.parse(question);
        JudgeInfoMessageEnum limitMessage = JudgeResourceLimitChecker.checkLimit(userJudgeInfo, judgeConfig);
        if (limitMessage != null) {
            return buildResult(judgeInfoResponse, limitMessage, passed, total);
        }
        // 4）全部通过
        return buildResult(judgeInfoResponse, JudgeInfoMessageEnum.ACCEPTED, passed, total);
    }

    private JudgeInfo buildResult(JudgeInfo judgeInfo, JudgeInfoMessageEnum message, int passed, int total) {
        judgeInfo.setMessage(message.getValue());
        judgeInfo.setPassedCase(passed);
        judgeInfo.setTotalCase(total);
        return judgeInfo;
    }
}
