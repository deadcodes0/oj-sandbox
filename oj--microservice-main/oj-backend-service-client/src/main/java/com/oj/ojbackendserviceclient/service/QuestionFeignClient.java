package com.oj.ojbackendserviceclient.service;


import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.entity.QuestionSubmit;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
*
*/


@FeignClient(name = "oj-backend-question-service", path = "/api/question/inner")
public interface QuestionFeignClient {

    @GetMapping("/get/id")
    Question getQuestionById(@RequestParam("questionId") long questionId);

    @GetMapping("/list/all")
    List<Question> listAllQuestions();

    @GetMapping("/question_submit/get/id")
    QuestionSubmit getQuestionSubmitById(@RequestParam("questionId") long questionSubmitId);

    @PostMapping("/question_submit/update")
    boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit);

    /**
     * 增减题目的计数字段（submitNum / acceptedNum），供判题服务跨服务调用
     *
     * @param questionId 题目 id
     * @param fieldName  计数字段名（submitNum 或 acceptedNum）
     * @param isIncr     true 自增 1，false 自减 1
     */
    @PostMapping("/question/increase/count")
    boolean increaseQuestionCount(@RequestParam("questionId") long questionId,
                                 @RequestParam("fieldName") String fieldName,
                                 @RequestParam("isIncr") boolean isIncr);

}
