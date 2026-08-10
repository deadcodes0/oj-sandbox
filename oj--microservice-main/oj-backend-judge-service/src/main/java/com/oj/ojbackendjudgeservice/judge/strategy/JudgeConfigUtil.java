package com.oj.ojbackendjudgeservice.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.oj.ojbackendmodel.model.dto.question.JudgeConfig;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.enums.CompareMode;
import org.apache.commons.lang3.StringUtils;

/**
 * 判题配置解析工具
 */
public class JudgeConfigUtil {

    private JudgeConfigUtil() {
    }

    /**
     * 解析题目判题配置，配置缺失返回 null
     */
    public static JudgeConfig parse(Question question) {
        if (question == null || StringUtils.isBlank(question.getJudgeConfig())) {
            return null;
        }
        return JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class);
    }

    /**
     * 解析比对模式，未配置或无法识别时返回默认的标准比对
     */
    public static CompareMode resolveCompareMode(Question question) {
        JudgeConfig judgeConfig = parse(question);
        if (judgeConfig == null || StringUtils.isBlank(judgeConfig.getCompareMode())) {
            return CompareMode.STANDARD;
        }
        CompareMode compareMode = CompareMode.getEnumByValue(judgeConfig.getCompareMode());
        return compareMode == null ? CompareMode.STANDARD : compareMode;
    }
}
