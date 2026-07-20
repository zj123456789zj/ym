package com.yumian.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interview_session")
public class InterviewSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer type;
    private Integer status;
    private Integer questionCount;
    private Integer targetCount;
    private String sourceQuestionIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer overallScore;
    private String overallEvaluation;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
