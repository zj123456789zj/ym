package com.yumian.service;

import com.yumian.entity.InterviewSession;
import com.yumian.entity.InterviewQa;

import java.util.List;

public interface InterviewService {
    InterviewSession createSession(Long userId, int type, int targetCount);

    InterviewQa askQuestion(Long sessionId);

    InterviewQa submitAnswer(Long qaId, String answer);

    InterviewQa askFollowUp(Long parentQaId);

    void endSession(Long sessionId, Long userId);

    List<InterviewSession> getHistory(Long userId);

    InterviewSession getSession(Long sessionId, Long userId);

    List<InterviewQa> getQaList(Long sessionId);
}
