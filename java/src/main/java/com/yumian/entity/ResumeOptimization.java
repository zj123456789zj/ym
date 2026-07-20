package com.yumian.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume_optimization")
public class ResumeOptimization {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long resumeId;
    private String suggestions;
    private Integer score;
    private String optimizedContent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
