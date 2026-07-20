package com.yumian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yumian.entity.InterviewReview;
import com.yumian.entity.InterviewSession;
import com.yumian.entity.WrongQuestion;
import com.yumian.mapper.InterviewQaMapper;
import com.yumian.mapper.InterviewReviewMapper;
import com.yumian.mapper.InterviewSessionMapper;
import com.yumian.mapper.WrongQuestionMapper;
import com.yumian.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final InterviewSessionMapper sessionMapper;
    private final InterviewReviewMapper reviewMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final InterviewQaMapper interviewQaMapper;

    @Override
    public Map<String, Object> getOverview(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        Long interviewCount = sessionMapper.selectCount(
                new LambdaQueryWrapper<InterviewSession>().eq(InterviewSession::getUserId, userId));
        stats.put("totalInterviews", interviewCount);

        Long reviewCount = reviewMapper.selectCount(
                new LambdaQueryWrapper<InterviewReview>().eq(InterviewReview::getUserId, userId));
        stats.put("totalReviews", reviewCount);

        Long wrongCount = wrongQuestionMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));
        stats.put("totalWrongQuestions", wrongCount);

        stats.put("totalQuestions", 0);
        return stats;
    }

    @Override
    public Map<String, Object> getTrend(Long userId, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<String> dates = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dates.add(d.toString());
        }

        Map<String, Long> interviews = countByDate(sessionMapper, userId, startDate, endDate);
        Map<String, Long> reviews = countByDate(reviewMapper, userId, startDate, endDate);
        Map<String, Long> questions = countQaByDate(userId, startDate, endDate);
        Map<String, Long> wrongQuestions = countByDate(wrongQuestionMapper, userId, startDate, endDate);

        List<Long> interviewList = fillZeros(dates, interviews);
        List<Long> reviewList = fillZeros(dates, reviews);
        List<Long> questionList = fillZeros(dates, questions);
        List<Long> wrongList = fillZeros(dates, wrongQuestions);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("totalInterviews", interviewList);
        result.put("totalReviews", reviewList);
        result.put("totalQuestions", questionList);
        result.put("totalWrongQuestions", wrongList);
        return result;
    }

    @Override
    public Map<String, Object> getOverview(Long userId, String startDate, String endDate) {
        Map<String, Object> trend = getTrend(userId, startDate, endDate);
        Map<String, Object> result = new LinkedHashMap<>();

        for (String key : new String[]{"totalInterviews", "totalReviews", "totalQuestions", "totalWrongQuestions"}) {
            long sum = 0;
            for (Long v : (List<Long>) trend.get(key)) sum += v;
            result.put(key, sum);
        }
        return result;
    }

    private <T> Map<String, Long> countByDate(BaseMapper<T> mapper, Long userId, String startDate, String endDate) {
        List<Map<String, Object>> rows = mapper.selectMaps(
                new QueryWrapper<T>()
                        .select("DATE(created_at) as date, COUNT(*) as cnt")
                        .eq("user_id", userId)
                        .between("created_at", startDate + " 00:00:00", endDate + " 23:59:59")
                        .groupBy("DATE(created_at)")
                        .orderByAsc("DATE(created_at)")
        );
        Map<String, Long> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(row.get("date").toString(), ((Number) row.get("cnt")).longValue());
        }
        return map;
    }

    private Map<String, Long> countQaByDate(Long userId, String startDate, String endDate) {
        List<Map<String, Object>> rows = interviewQaMapper.countByDateWithUser(
                userId, startDate + " 00:00:00", endDate + " 23:59:59");
        Map<String, Long> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(row.get("date").toString(), ((Number) row.get("cnt")).longValue());
        }
        return map;
    }

    private List<Long> fillZeros(List<String> dates, Map<String, Long> data) {
        List<Long> result = new ArrayList<>();
        for (String date : dates) {
            result.add(data.getOrDefault(date, 0L));
        }
        return result;
    }
}
