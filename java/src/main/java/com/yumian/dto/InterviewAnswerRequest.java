package com.yumian.dto;

import lombok.Data;

@Data
public class InterviewAnswerRequest {
    private Long sessionId;
    private Long qaId;
    private String answer;
    private Integer type;
}
