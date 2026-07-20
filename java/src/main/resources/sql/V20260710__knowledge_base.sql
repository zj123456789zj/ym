-- 题库主表
CREATE TABLE IF NOT EXISTS knowledge_question (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    question         TEXT         NOT NULL COMMENT '题目内容',
    reference_answer TEXT         COMMENT '参考答案',
    category         VARCHAR(100) NOT NULL COMMENT '分类：Java/数据库/系统设计/项目/...',
    difficulty       TINYINT      DEFAULT 1 COMMENT '难度 1-5',
    tags             VARCHAR(500) COMMENT '标签，逗号分隔',
    embedding        JSON         COMMENT '语义向量，数字数组',
    status           TINYINT      DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_by       BIGINT       COMMENT '创建人（admin 用户 ID）',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库题库';

-- 导入记录表
CREATE TABLE IF NOT EXISTS knowledge_import_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name      VARCHAR(255) COMMENT '文件名',
    total_count    INT          DEFAULT 0 COMMENT '总条数',
    success_count  INT          DEFAULT 0 COMMENT '成功数',
    fail_count     INT          DEFAULT 0 COMMENT '失败数',
    created_by     BIGINT       COMMENT '操作人',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库导入记录';

-- user 表加 role 字段
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'yumian' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'role');
SET @sql = IF(@exists = 0, 'ALTER TABLE user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT ''角色: admin/user'' AFTER email', 'SELECT ''role already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
