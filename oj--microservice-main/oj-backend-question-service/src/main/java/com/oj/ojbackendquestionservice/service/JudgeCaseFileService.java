package com.oj.ojbackendquestionservice.service;

import com.oj.ojbackendcommon.utils.TestCaseFileUtil;
import com.oj.ojbackendmodel.model.dto.question.JudgeCase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 判题用例文件服务：把题目用例写入文件存储
 */
@Component
public class JudgeCaseFileService {

    @Value("${oj.judge-case.storage-path:D:/code/oj/in-out}")
    private String storagePath;

    /**
     * 保存用例到文件
     */
    public void saveCases(long questionId, List<JudgeCase> cases) {
        if (CollectionUtils.isEmpty(cases)) {
            return;
        }
        List<String> inputs = cases.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        List<String> outputs = cases.stream().map(JudgeCase::getOutput).collect(Collectors.toList());
        TestCaseFileUtil.writeCaseFiles(storagePath, questionId, inputs, outputs);
    }

    /**
     * 重写用例文件（先清空再写入）
     */
    public void rewriteCases(long questionId, List<JudgeCase> cases) {
        TestCaseFileUtil.deleteCaseFiles(storagePath, questionId);
        saveCases(questionId, cases);
    }

    /**
     * 删除题目的用例文件
     */
    public void deleteCases(long questionId) {
        TestCaseFileUtil.deleteCaseFiles(storagePath, questionId);
    }
}
