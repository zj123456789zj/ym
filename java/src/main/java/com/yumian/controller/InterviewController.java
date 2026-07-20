package com.yumian.controller;

import com.yumian.common.Result;
import com.yumian.dto.InterviewAnswerRequest;
import com.yumian.entity.InterviewQa;
import com.yumian.entity.InterviewSession;
import com.yumian.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/session")
    public Result<InterviewSession> createSession(@RequestAttribute("userId") Long userId,
                                                   @RequestParam int type,
                                                   @RequestParam(required = false, defaultValue = "0") int targetCount) {
        return Result.success(interviewService.createSession(userId, type, targetCount));
    }

    @PostMapping("/question")
    public Result<InterviewQa> askQuestion(@RequestParam Long sessionId) {
        return Result.success(interviewService.askQuestion(sessionId));
    }

    @PostMapping("/answer")
    public Result<InterviewQa> submitAnswer(@RequestBody InterviewAnswerRequest request) {
        return Result.success(interviewService.submitAnswer(request.getQaId(), request.getAnswer()));
    }

    @PostMapping("/follow-up")
    public Result<InterviewQa> askFollowUp(@RequestParam Long qaId) {
        return Result.success(interviewService.askFollowUp(qaId));
    }

    @PostMapping("/end")
    public Result<Void> endSession(@RequestAttribute("userId") Long userId,
                                   @RequestParam Long sessionId) {
        interviewService.endSession(sessionId, userId);
        return Result.success();
    }

    @GetMapping("/history")
    public Result<List<InterviewSession>> getHistory(@RequestAttribute("userId") Long userId) {
        return Result.success(interviewService.getHistory(userId));
    }

    @GetMapping("/session/{id}")
    public Result<InterviewSession> getSession(@RequestAttribute("userId") Long userId,
                                               @PathVariable Long id) {
        return Result.success(interviewService.getSession(id, userId));
    }

    @GetMapping("/qa/{sessionId}")
    public Result<List<InterviewQa>> getQaList(@PathVariable Long sessionId) {
        return Result.success(interviewService.getQaList(sessionId));
    }
}
