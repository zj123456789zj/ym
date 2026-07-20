-- ============================================================
-- 鱼面 (Yumian) Database Schema
-- 完全基于 Entity 类字段生成
-- ============================================================

CREATE DATABASE IF NOT EXISTS yumian DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE yumian;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) DEFAULT NULL,
    `avatar` VARCHAR(255) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `email` VARCHAR(100) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 简历表
CREATE TABLE IF NOT EXISTS `resume` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `raw_content` TEXT COMMENT '原始简历文本',
    `parsed_content` TEXT COMMENT 'AI解析后的结构化JSON',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 简历优化记录表
CREATE TABLE IF NOT EXISTS `resume_optimization` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `resume_id` BIGINT NOT NULL,
    `suggestions` TEXT COMMENT 'JSON数组：逐条修改建议',
    `score` INT DEFAULT NULL COMMENT 'AI评分(百分制)',
    `optimized_content` TEXT COMMENT 'AI优化后的完整简历',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`resume_id`) REFERENCES `resume`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 面试会话表
CREATE TABLE IF NOT EXISTS `interview_session` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `type` TINYINT NOT NULL COMMENT '1-模拟面试 2-专项训练',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-进行中 1-已结束',
    `question_count` INT DEFAULT 0 COMMENT '题目总数',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `overall_score` INT DEFAULT NULL COMMENT '总分',
    `overall_evaluation` TEXT COMMENT 'AI综合评语',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 面试问答表（核心表）
CREATE TABLE IF NOT EXISTS `interview_qa` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL,
    `question` TEXT NOT NULL COMMENT '面试题目',
    `question_type` TINYINT NOT NULL DEFAULT 0 COMMENT '0-普通题 1-追问',
    `parent_qa_id` BIGINT DEFAULT NULL COMMENT '若为追问，指向被追问的QA',
    `user_answer` TEXT COMMENT '用户回答',
    `ai_evaluation` TEXT COMMENT 'AI评价',
    `score` INT DEFAULT NULL COMMENT '单项评分',
    `sort_order` INT NOT NULL COMMENT '题目顺序',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`session_id`) REFERENCES `interview_session`(`id`),
    FOREIGN KEY (`parent_qa_id`) REFERENCES `interview_qa`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 面试复盘表
CREATE TABLE IF NOT EXISTS `interview_review` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL UNIQUE,
    `user_id` BIGINT NOT NULL,
    `summary` TEXT COMMENT '面试总结',
    `strengths` TEXT COMMENT 'JSON数组：表现好的方面',
    `weaknesses` TEXT COMMENT 'JSON数组：不足之处',
    `suggestions` TEXT COMMENT 'JSON数组：改进建议',
    `knowledge_gaps` TEXT COMMENT 'JSON数组：知识盲区',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`session_id`) REFERENCES `interview_session`(`id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 错题本表
CREATE TABLE IF NOT EXISTS `wrong_question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question` TEXT NOT NULL COMMENT '题目',
    `user_answer` TEXT COMMENT '用户的错误回答',
    `reference_answer` TEXT COMMENT '正确答案/参考回答',
    `analysis` TEXT COMMENT '解析',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类(Java/MySQL/项目等)',
    `source` VARCHAR(30) DEFAULT NULL COMMENT '来源(模拟面试/专项训练/手动)',
    `source_qa_id` BIGINT DEFAULT NULL COMMENT '关联的面试QA',
    `review_count` INT DEFAULT 0 COMMENT '复习次数',
    `last_reviewed_at` DATETIME DEFAULT NULL COMMENT '上次复习时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 简历表新增 pdf_path 列
ALTER TABLE `resume` ADD COLUMN `pdf_path` VARCHAR(255) DEFAULT NULL COMMENT 'PDF 文件存储路径' AFTER `parsed_content`;
