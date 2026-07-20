package com.yumian.controller;

import com.yumian.common.Result;
import com.yumian.entity.WrongQuestion;
import com.yumian.service.WrongQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {
    private final WrongQuestionService wrongQuestionService;

    @GetMapping
    public Result<List<WrongQuestion>> getList(@RequestAttribute("userId") Long userId,
                                                @RequestParam(required = false) String category) {
        return Result.success(wrongQuestionService.getList(userId, category));
    }

    @PostMapping
    public Result<Void> add(@RequestAttribute("userId") Long userId,
                            @RequestBody WrongQuestion question) {
        question.setUserId(userId);
        wrongQuestionService.addWrongQuestion(question);
        return Result.success();
    }

    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id) {
        wrongQuestionService.reviewAgain(id);
        return Result.success();
    }
}
