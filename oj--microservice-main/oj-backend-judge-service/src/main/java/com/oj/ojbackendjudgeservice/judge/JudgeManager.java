package com.oj.ojbackendjudgeservice.judge;

import com.oj.ojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.oj.ojbackendjudgeservice.judge.special.SpecialJudgeExecutor;
import com.oj.ojbackendjudgeservice.judge.strategy.DefaultJudgeStrategy;
import com.oj.ojbackendjudgeservice.judge.strategy.JudgeConfigUtil;
import com.oj.ojbackendjudgeservice.judge.strategy.JudgeContext;
import com.oj.ojbackendjudgeservice.judge.strategy.JudgeStrategy;
import com.oj.ojbackendmodel.model.codesandbox.JudgeInfo;
import com.oj.ojbackendmodel.model.enums.CompareMode;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 判题管理（简化调用）
 */
@Service
public class JudgeManager {

    @Resource
    private SpecialJudgeExecutor specialJudgeExecutor;

    /**
     * 执行判题
     *
     * @param judgeContext 判题上下文
     * @param codeSandbox 代码沙箱（特判时需要二次调用）
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext, CodeSandbox codeSandbox) {
        CompareMode compareMode = JudgeConfigUtil.resolveCompareMode(judgeContext.getQuestion());
        // 特判模式：直接交给特判执行器
        if (compareMode == CompareMode.SPJ) {
            return specialJudgeExecutor.doJudge(judgeContext, codeSandbox);
        }
        // 非特判模式：不同语言的判定逻辑一致，统一使用默认判题策略
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        return judgeStrategy.doJudge(judgeContext);
    }

}
