package com.yumian.service;

import com.yumian.entity.WrongQuestion;

import java.util.List;

public interface WrongQuestionService {
    List<WrongQuestion> getList(Long userId, String category);
    void addWrongQuestion(WrongQuestion question);
    void reviewAgain(Long id);
}
