package com.yumian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yumian.entity.WrongQuestion;
import com.yumian.mapper.WrongQuestionMapper;
import com.yumian.service.WrongQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WrongQuestionServiceImpl implements WrongQuestionService {
    private final WrongQuestionMapper wrongQuestionMapper;

    @Override
    public List<WrongQuestion> getList(Long userId, String category) {
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(WrongQuestion::getCategory, category);
        }
        wrapper.orderByDesc(WrongQuestion::getCreatedAt);
        return wrongQuestionMapper.selectList(wrapper);
    }

    @Override
    public void addWrongQuestion(WrongQuestion question) {
        wrongQuestionMapper.insert(question);
    }

    @Override
    public void reviewAgain(Long id) {
        WrongQuestion wq = wrongQuestionMapper.selectById(id);
        if (wq != null) {
            wq.setReviewCount(wq.getReviewCount() == null ? 1 : wq.getReviewCount() + 1);
            wq.setLastReviewedAt(LocalDateTime.now());
            wrongQuestionMapper.updateById(wq);
        }
    }
}
