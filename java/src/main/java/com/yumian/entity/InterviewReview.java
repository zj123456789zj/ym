package com.yumian.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interview_review")
public class InterviewReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long userId;
    private String summary;
    private String strengths;
    private String weaknesses;
    private String suggestions;
    private String knowledgeGaps;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
