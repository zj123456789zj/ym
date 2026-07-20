package com.yumian.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wrong_question")
public class WrongQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String question;
    private String userAnswer;
    private String referenceAnswer;
    private String analysis;
    private String category;
    private String source;
    private Long sourceQaId;
    private Integer reviewCount;
    private LocalDateTime lastReviewedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
