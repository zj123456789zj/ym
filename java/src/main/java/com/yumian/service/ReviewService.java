package com.yumian.service;

import com.yumian.entity.InterviewReview;

public interface ReviewService {
    InterviewReview getReview(Long sessionId, Long userId);
    InterviewReview generateReview(Long sessionId, Long userId);
}
