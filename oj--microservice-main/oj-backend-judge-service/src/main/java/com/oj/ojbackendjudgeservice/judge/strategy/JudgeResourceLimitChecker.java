package com.oj.ojbackendjudgeservice.judge.strategy;

import com.oj.ojbackendmodel.model.codesandbox.JudgeInfo;
import com.oj.ojbackendmodel.model.dto.question.JudgeConfig;
import com.oj.ojbackendmodel.model.enums.JudgeInfoMessageEnum;

import java.util.Optional;

/**
 * 资源限制校验（内存 / 时间，对所有语言一视同仁）
 */
public class JudgeResourceLimitChecker {

    private JudgeResourceLimitChecker() {
    }

    /**
     * 校验用户程序的内存与时间限制
     *
     * @param userJudgeInfo 沙箱返回的执行信息
     * @param judgeConfig 判题配置（可为 null，null 表示不校验）
     * @return 超限的判定枚举，未超限返回 null
     */
    public static JudgeInfoMessageEnum checkLimit(JudgeInfo userJudgeInfo, JudgeConfig judgeConfig) {
        if (judgeConfig == null) {
            return null;
        }
        Long memory = Optional.ofNullable(userJudgeInfo).map(JudgeInfo::getMemory).orElse(0L);
        Long time = Optional.ofNullable(userJudgeInfo).map(JudgeInfo::getTime).orElse(0L);
        if (memory > judgeConfig.getMemoryLimit()) {
            return JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
        }
        if (time > judgeConfig.getTimeLimit()) {
            return JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
        }
        return null;
    }
}
