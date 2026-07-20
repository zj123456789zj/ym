package com.yumian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yumian.common.exception.BusinessException;
import com.yumian.entity.InterviewQa;
import com.yumian.entity.InterviewReview;
import com.yumian.entity.InterviewSession;
import com.yumian.mapper.InterviewQaMapper;
import com.yumian.mapper.InterviewReviewMapper;
import com.yumian.mapper.InterviewSessionMapper;
import com.yumian.service.ReviewService;
import com.yumian.service.AiService;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final InterviewReviewMapper reviewMapper;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewQaMapper qaMapper;
    private final AiService aiService;

    @Override
    public InterviewReview getReview(Long sessionId, Long userId) {
        InterviewReview review = reviewMapper.selectOne(
                new LambdaQueryWrapper<InterviewReview>()
                        .eq(InterviewReview::getSessionId, sessionId));
        if (review == null) {
            return generateReview(sessionId, userId);
        }
        return review;
    }

    @Override
    public InterviewReview generateReview(Long sessionId, Long userId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException("面试会话不存在");

        List<InterviewQa> qaList = qaMapper.selectList(
                new LambdaQueryWrapper<InterviewQa>()
                        .eq(InterviewQa::getSessionId, sessionId)
                        .orderByAsc(InterviewQa::getSortOrder));

        StringBuilder qaText = new StringBuilder();
        for (InterviewQa qa : qaList) {
            qaText.append("题目：").append(qa.getQuestion()).append("\n");
            if (qa.getUserAnswer() != null) {
                qaText.append("回答：").append(qa.getUserAnswer()).append("\n");
            }
            qaText.append("---\n");
        }

        String prompt = "你是一个面试复盘顾问。基于以下面试记录，生成复盘报告。"
                + "请严格按以下 JSON 格式返回，不要加多余说明：\n"
                + "{\n"
                + "  \"summary\": \"面试总结（一段话概括整体表现）\",\n"
                + "  \"strengths\": [\"优点1\", \"优点2\"],\n"
                + "  \"weaknesses\": [\"不足1\", \"不足2\"],\n"
                + "  \"suggestions\": [\"建议1\", \"建议2\"],\n"
                + "  \"knowledgeGaps\": [\"知识盲区1\", \"知识盲区2\"]\n"
                + "}";

        String response = aiService.chat(prompt, qaText.toString());

        InterviewReview review = reviewMapper.selectOne(
                new LambdaQueryWrapper<InterviewReview>()
                        .eq(InterviewReview::getSessionId, sessionId));
        if (review == null) {
            review = new InterviewReview();
            review.setSessionId(sessionId);
            review.setUserId(userId);
        }

        try {
            JSONObject json = new JSONObject(response);
            review.setSummary(json.getStr("summary"));
            review.setStrengths(json.getJSONArray("strengths").toString());
            review.setWeaknesses(json.getJSONArray("weaknesses").toString());
            review.setSuggestions(json.getJSONArray("suggestions").toString());
            review.setKnowledgeGaps(json.getJSONArray("knowledgeGaps").toString());
        } catch (Exception e) {
            // JSON 解析失败，降级：把整个响应存到 summary，其他留空
            review.setSummary(response.length() > 500 ? response.substring(0, 500) : response);
            review.setStrengths("[]");
            review.setWeaknesses("[]");
            review.setSuggestions("[]");
            review.setKnowledgeGaps("[]");
        }

        if (review.getId() == null) {
            reviewMapper.insert(review);
        } else {
            reviewMapper.updateById(review);
        }
        return review;
    }
}
