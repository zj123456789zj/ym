package com.yumian.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_question")
public class KnowledgeQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String question;
    private String referenceAnswer;
    private String category;
    private Integer difficulty;
    private String tags;
    private String embedding;   // JSON 字符串 "[0.1,0.2,...]"
    private Integer status;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
