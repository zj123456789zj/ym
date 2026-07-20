-- interview_session 新增字段
ALTER TABLE interview_session
    ADD COLUMN target_count INT DEFAULT 0 COMMENT '目标题数，达到后自动结束',
    ADD COLUMN source_question_ids TEXT COMMENT '已出的知识库题ID列表，逗号分隔';

-- interview_qa 新增字段
ALTER TABLE interview_qa
    ADD COLUMN source_question_id BIGINT DEFAULT NULL COMMENT '关联的知识库题目ID',
    ADD COLUMN source_question_text TEXT DEFAULT NULL COMMENT '改写前的原题内容（智能模式用）';
