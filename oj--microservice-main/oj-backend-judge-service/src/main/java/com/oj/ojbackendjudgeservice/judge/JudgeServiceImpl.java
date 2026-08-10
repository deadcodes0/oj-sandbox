package com.oj.ojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;
import com.oj.ojbackendcommon.common.ErrorCode;
import com.oj.ojbackendcommon.exception.BusinessException;
import com.oj.ojbackendcommon.utils.TestCaseFileUtil;
import com.oj.ojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.oj.ojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.oj.ojbackendjudgeservice.judge.codesandbox.CodeSandboxProxy;
import com.oj.ojbackendjudgeservice.judge.strategy.JudgeContext;
import com.oj.ojbackendmodel.model.codesandbox.CodeFile;
import com.oj.ojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.oj.ojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import com.oj.ojbackendmodel.model.codesandbox.JudgeInfo;
import com.oj.ojbackendmodel.model.dto.question.JudgeCase;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.entity.QuestionSubmit;
import com.oj.ojbackendmodel.model.enums.JudgeInfoMessageEnum;
import com.oj.ojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.oj.ojbackendserviceclient.service.QuestionFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;

    @Value("${oj.judge-case.storage-path:D:/code/oj/in-out}")
    private String storagePath;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2）如果题目提交状态不为等待中，就不用重复执行了
        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
        }
        // 3）更改判题（题目提交）的状态为 “判题中”，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }

        try {
            // 4）调用沙箱，获取到执行结果
            CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
            codeSandbox = new CodeSandboxProxy(codeSandbox);
            String language = questionSubmit.getLanguage();
            String code = questionSubmit.getCode();
            // 获取输入用例（从文件读取）
            List<JudgeCase> judgeCaseList = loadJudgeCases(question);
            List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
            ExecuteCodeRequest executeCodeRequest = buildExecuteCodeRequest(language, code, question, inputList);
            ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
            // 5）沙箱执行失败（编译/运行/系统错误）时直接返回结果，不再比对输出
            JudgeInfo judgeInfo = resolveSandboxFailure(executeCodeResponse);
            if (judgeInfo != null) {
                return saveResult(questionSubmitId, questionId, judgeInfo, QuestionSubmitStatusEnum.SUCCEED);
            }
            // 6）根据沙箱的执行结果，设置题目的判题状态和信息
            JudgeContext judgeContext = new JudgeContext();
            judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
            judgeContext.setInputList(inputList);
            judgeContext.setOutputList(executeCodeResponse.getOutputList());
            judgeContext.setJudgeCaseList(judgeCaseList);
            judgeContext.setQuestion(question);
            judgeContext.setQuestionSubmit(questionSubmit);
            judgeInfo = judgeManager.doJudge(judgeContext, codeSandbox);
            return saveResult(questionSubmitId, questionId, judgeInfo, QuestionSubmitStatusEnum.SUCCEED);
        } catch (Exception e) {
            // 7）判题异常时尽力将提交置为失败，避免一直停留在判题中
            log.error("判题异常，questionSubmitId = {}", questionSubmitId, e);
            saveResult(questionSubmitId, questionId, buildErrorJudgeInfo(JudgeInfoMessageEnum.SYSTEM_ERROR),
                    QuestionSubmitStatusEnum.FAILED);
            throw e;
        }
    }

    /**
     * 构造沙箱请求。Java 题且已配置 mainTemplate 时，随 codeList 下发 Main.java（入口）+ Solution.java（用户代码）；
     * 其余语言（含未配置模板的 Java）走单文件 code，保持向后兼容。
     */
    private ExecuteCodeRequest buildExecuteCodeRequest(String language, String code, Question question,
            List<String> inputList) {
        boolean isJava = "java".equalsIgnoreCase(language);
        String mainTemplate = question.getMainTemplate();
        if (isJava && StringUtils.isNotBlank(mainTemplate)) {
            List<CodeFile> codeList = new ArrayList<>();
            codeList.add(new CodeFile("Solution.java", code));
            codeList.add(new CodeFile("Main.java", mainTemplate));
            return ExecuteCodeRequest.builder()
                    .codeList(codeList)
                    .language(language)
                    .inputList(inputList)
                    .build();
        }
        return ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
    }

    /**
     * 从文件加载题目的判题用例，未配置用例文件时报错
     */
    private List<JudgeCase> loadJudgeCases(Question question) {
        TestCaseFileUtil.CaseFiles caseFiles = TestCaseFileUtil.readCaseFiles(storagePath, question.getId());
        if (caseFiles == null || caseFiles.getInputs().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目用例不存在，请先配置用例文件");
        }
        List<JudgeCase> judgeCaseList = new ArrayList<>();
        List<String> inputs = caseFiles.getInputs();
        List<String> outputs = caseFiles.getOutputs();
        for (int i = 0; i < inputs.size(); i++) {
            JudgeCase judgeCase = new JudgeCase();
            judgeCase.setInput(inputs.get(i));
            judgeCase.setOutput(outputs.get(i));
            judgeCaseList.add(judgeCase);
        }
        return judgeCaseList;
    }

    /**
     * 解析沙箱执行失败：返回 null 表示执行成功，否则返回对应的错误判题信息
     */
    private JudgeInfo resolveSandboxFailure(ExecuteCodeResponse executeCodeResponse) {
        if (executeCodeResponse == null) {
            return buildErrorJudgeInfo(JudgeInfoMessageEnum.SYSTEM_ERROR);
        }
        if (executeCodeResponse.getOutputList() == null) {
            return buildErrorJudgeInfo(JudgeInfoMessageEnum.SYSTEM_ERROR);
        }
        Integer status = executeCodeResponse.getStatus();
        // 沙箱契约：status == 1 表示正常执行完成（2=沙箱系统错误，3=代码相关错误）。
        // 不可用 QuestionSubmitStatusEnum.SUCCEED(2) 比对——那是判题提交状态枚举，语义不同。
        if (status != null && !status.equals(1)) {
            String message = executeCodeResponse.getMessage();
            if (StringUtils.containsIgnoreCase(message, "compile")) {
                return buildErrorJudgeInfo(JudgeInfoMessageEnum.COMPILE_ERROR);
            }
            if (StringUtils.containsIgnoreCase(message, "runtime")
                    || StringUtils.containsIgnoreCase(message, "segmentation")
                    || StringUtils.containsIgnoreCase(message, "exception")) {
                return buildErrorJudgeInfo(JudgeInfoMessageEnum.RUNTIME_ERROR);
            }
            return buildErrorJudgeInfo(JudgeInfoMessageEnum.SYSTEM_ERROR);
        }
        return null;
    }

    private JudgeInfo buildErrorJudgeInfo(JudgeInfoMessageEnum messageEnum) {
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(messageEnum.getValue());
        return judgeInfo;
    }

    /**
     * 保存判题结果并返回更新后的提交记录
     */
    private QuestionSubmit saveResult(long questionSubmitId, long questionId, JudgeInfo judgeInfo,
            QuestionSubmitStatusEnum submitStatus) {
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(submitStatus.getValue());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        // 判题通过时，题目通过数 +1（独立于判题结果保存，避免计数失败影响主流程）
        if (judgeInfo != null && JudgeInfoMessageEnum.ACCEPTED.getValue().equals(judgeInfo.getMessage())) {
            try {
                questionFeignClient.increaseQuestionCount(questionId, "acceptedNum", true);
            } catch (Exception e) {
                log.error("更新题目通过数失败，questionId = {}", questionId, e);
            }
        }
        return questionFeignClient.getQuestionSubmitById(questionSubmitId);
    }
}
