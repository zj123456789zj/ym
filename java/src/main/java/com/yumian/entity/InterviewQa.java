package com.yumian.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interview_qa")
public class InterviewQa {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String question;
    private Integer questionType;
    private Long parentQaId;
    private String userAnswer;
    private String aiEvaluation;
    private Integer score;
    private String referenceAnswer;
    private Long sourceQuestionId;
    private String sourceQuestionText;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // === 以下为响应专用字段，不持久化 ===
    @TableField(exist = false)
    private Boolean fromKnowledgeBase;

    @TableField(exist = false)
    private String originalQuestion;

    @TableField(exist = false)
    private Boolean noMoreQuestions;

    @TableField(exist = false)
    private Boolean sessionEnded;
}
