package com.yumian.controller;

import com.yumian.common.Result;
import com.yumian.entity.InterviewReview;
import com.yumian.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/{sessionId}")
    public Result<InterviewReview> getReview(@RequestAttribute("userId") Long userId,
                                              @PathVariable Long sessionId) {
        return Result.success(reviewService.getReview(sessionId, userId));
    }

    @PostMapping("/generate/{sessionId}")
    public Result<InterviewReview> generateReview(@RequestAttribute("userId") Long userId,
                                                   @PathVariable Long sessionId) {
        return Result.success(reviewService.generateReview(sessionId, userId));
    }
}
