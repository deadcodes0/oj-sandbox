package com.oj.ojbackendquestionservice.controller.inner;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.entity.QuestionSubmit;
import com.oj.ojbackendquestionservice.service.QuestionService;
import com.oj.ojbackendquestionservice.service.QuestionSubmitService;
import com.oj.ojbackendserviceclient.service.QuestionFeignClient;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 该服务仅内部调用，不是给前端的
 */

//server:
//address: 0.0.0.0
//port: 8103
//servlet:
//context-path: /api/question

@RestController
@RequestMapping("/inner")
// api/question + /inner
public class QuestionInnerController implements QuestionFeignClient {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @GetMapping("/get/id")
    @Override
    public Question getQuestionById(@RequestParam("questionId") long questionId) {
        return questionService.getById(questionId);
    }

    @GetMapping("/list/all")
    @Override
    public List<Question> listAllQuestions() {
        return questionService.list();
    }

    @GetMapping("/question_submit/get/id")
    @Override
    public QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId) {
        return questionSubmitService.getById(questionSubmitId);
    }

    @PostMapping("/question_submit/update")
    @Override
    public boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit) {
        return questionSubmitService.updateById(questionSubmit);
    }

    /**
     * 增减题目的计数字段（submitNum / acceptedNum），采用 SQL 自增保证并发安全。
     * 通过白名单限制可更新字段，避免任意字段被拼入 setSql。
     */
    @PostMapping("/question/increase/count")
    @Override
    public boolean increaseQuestionCount(@RequestParam("questionId") long questionId,
                                        @RequestParam("fieldName") String fieldName,
                                        @RequestParam("isIncr") boolean isIncr) {
        if (!"submitNum".equals(fieldName) && !"acceptedNum".equals(fieldName)) {
            return false;
        }
        LambdaUpdateWrapper<Question> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Question::getId, questionId);
        updateWrapper.setSql(fieldName + " = " + fieldName + (isIncr ? " + 1" : " - 1"));
        return questionService.update(updateWrapper);
    }

}
