package com.yumian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yumian.common.exception.BusinessException;
import com.yumian.entity.InterviewQa;
import com.yumian.entity.InterviewSession;
import com.yumian.entity.KnowledgeQuestion;
import com.yumian.entity.Resume;
import com.yumian.mapper.InterviewQaMapper;
import com.yumian.mapper.InterviewSessionMapper;
import com.yumian.mapper.ResumeMapper;
import com.yumian.service.InterviewService;
import com.yumian.service.AiService;
import com.yumian.service.KnowledgeQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.json.JSONObject;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewQaMapper qaMapper;
    private final ResumeMapper resumeMapper;
    private final AiService aiService;
    private final KnowledgeQuestionService knowledgeQuestionService;

    @Override
    @Transactional
    public InterviewSession createSession(Long userId, int type, int targetCount) {
        if ((type == 1 || type == 3) && targetCount <= 0) {
            throw new BusinessException("题库模式和智能模式必须设置目标题数");
        }
        if (type == 1 || type == 3) {
            long count = knowledgeQuestionService.getAllEnabled().size();
            if (count == 0) {
                throw new BusinessException("知识库暂无可用题目，请联系管理员导入题库");
            }
        }
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setType(type);
        session.setStatus(0);
        session.setQuestionCount(0);
        session.setTargetCount(targetCount);
        session.setSourceQuestionIds("");
        session.setStartTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public InterviewQa askQuestion(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("面试会话不存在");
        }
        if (session.getStatus() == 1) {
            InterviewQa qa = new InterviewQa();
            qa.setNoMoreQuestions(true);
            return qa;
        }

        Long count = qaMapper.selectCount(
                new LambdaQueryWrapper<InterviewQa>().eq(InterviewQa::getSessionId, sessionId));

        InterviewQa qa = new InterviewQa();
        qa.setSessionId(sessionId);
        qa.setQuestionType(0);
        qa.setSortOrder(count.intValue() + 1);

        if (session.getType() == 1 || session.getType() == 3) {
            handleKnowledgeBaseQuestion(session, qa);
        } else if (session.getType() == 2) {
            handleResumeQuestion(session, qa);
        }

        qaMapper.insert(qa);
        session.setQuestionCount(session.getQuestionCount() + 1);
        sessionMapper.updateById(session);

        qa.setFromKnowledgeBase(session.getType() == 1 || session.getType() == 3);
        return qa;
    }

    private void handleKnowledgeBaseQuestion(InterviewSession session, InterviewQa qa) {
        List<Long> excludeIds = new java.util.ArrayList<>();
        if (session.getSourceQuestionIds() != null && !session.getSourceQuestionIds().isBlank()) {
            for (String id : session.getSourceQuestionIds().split(",")) {
                try {
                    excludeIds.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        KnowledgeQuestion kq = knowledgeQuestionService.getRandomEnabled(excludeIds);
        if (kq == null) {
            qa.setNoMoreQuestions(true);
            qa.setQuestion("题库中的题目已全部出完，点击结束按钮完成面试。");
            return;
        }

        qa.setSourceQuestionId(kq.getId());
        String updatedIds = session.getSourceQuestionIds() == null || session.getSourceQuestionIds().isBlank()
                ? String.valueOf(kq.getId())
                : session.getSourceQuestionIds() + "," + kq.getId();
        session.setSourceQuestionIds(updatedIds);

        if (session.getType() == 1) {
            qa.setQuestion(kq.getQuestion());
        } else {
            qa.setSourceQuestionText(kq.getQuestion());
            try {
                String rewritten = aiService.chat(
                        "你是一个技术面试官。请根据下面的题目进行改写，保持考察点不变，换一种表述方式或变换角度出题。只返回改写后的问题本身，不要多余内容。",
                        "原题：" + kq.getQuestion()
                                + "\n参考答案：" + (kq.getReferenceAnswer() != null ? kq.getReferenceAnswer() : ""));
                qa.setQuestion(rewritten);
                qa.setOriginalQuestion(kq.getQuestion());
            } catch (Exception e) {
                log.warn("AI 改写失败，降级为原题: {}", e.getMessage());
                qa.setQuestion(kq.getQuestion());
            }
        }
    }

    private String extractTextFromPdf(String pdfPath) {
        java.io.File file = new java.io.File(
                System.getProperty("user.dir") + java.io.File.separator + "userdata" + java.io.File.separator + "resumes" + java.io.File.separator + pdfPath);
        if (!file.exists()) {
            throw new BusinessException("简历文件不存在");
        }
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (java.io.IOException e) {
            throw new BusinessException("简历 PDF 解析失败");
        }
    }

    private void handleResumeQuestion(InterviewSession session, InterviewQa qa) {
        Resume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, session.getUserId()));
        if (resume == null) {
            qa.setQuestion("请先在「我的简历」中上传简历");
            return;
        }

        String resumeContent;
        if (resume.getPdfPath() != null) {
            resumeContent = extractTextFromPdf(resume.getPdfPath());
        } else if (resume.getRawContent() != null && !resume.getRawContent().isBlank()) {
            resumeContent = resume.getRawContent();
        } else {
            qa.setQuestion("请先在「我的简历」中上传简历");
            return;
        }

        List<InterviewQa> existingQas = qaMapper.selectList(
                new LambdaQueryWrapper<InterviewQa>()
                        .eq(InterviewQa::getSessionId, session.getId())
                        .eq(InterviewQa::getQuestionType, 0));
        StringBuilder previousQuestions = new StringBuilder();
        for (InterviewQa existing : existingQas) {
            previousQuestions.append("- ").append(existing.getQuestion()).append("\n");
        }

        String prompt = "你是一位资深技术面试官。以下是一份候选人的简历，请根据简历中出现的" +
                "技术栈、项目经验、专业技能进行提问，每次只出一道题。\n\n" +
                "要求：\n" +
                "- 问题必须严格基于简历中出现的内容\n" +
                "- 考察技术深度，而非表面概念\n" +
                "- 不要出简历中没有提到的技术\n" +
                "- 不要重复已出过的题目\n\n" +
                "简历内容：\n" + resumeContent + "\n\n";
        if (previousQuestions.length() > 0) {
            prompt += "已出过的题目：\n" + previousQuestions.toString() + "\n";
        }
        prompt += "请输出下一道面试题：";

        String question = aiService.chat(prompt, "");
        qa.setQuestion(question);
    }

    @Override
    @Transactional
    public InterviewQa submitAnswer(Long qaId, String answer) {
        InterviewQa qa = qaMapper.selectById(qaId);
        if (qa == null) {
            throw new BusinessException("题目不存在");
        }
        qa.setUserAnswer(answer);

        String response = aiService.chat(
                "你是一个面试评分专家。对于以下面试题目和回答，请严格按 JSON 格式返回："
                + "{\"score\": 数字, \"evaluation\": \"评价内容\", \"referenceAnswer\": \"参考答案\"}"
                + " 注意：只返回 JSON，不要加多余说明。",
                "题目：" + qa.getQuestion() + "\n回答：" + answer);

        try {
            JSONObject json = new JSONObject(response);
            qa.setScore(json.getInt("score"));
            qa.setAiEvaluation(json.getStr("evaluation"));
            qa.setReferenceAnswer(json.getStr("referenceAnswer"));
        } catch (Exception e) {
            // JSON 解析失败，降级为旧行为
            qa.setAiEvaluation(response);
            qa.setScore(75);
        }

        qaMapper.updateById(qa);

        // 判断是否达到目标题数，自动结束
        InterviewSession session = sessionMapper.selectById(qa.getSessionId());
        if (session != null && session.getTargetCount() != null && session.getTargetCount() > 0
                && session.getQuestionCount() >= session.getTargetCount()) {
            endSession(session.getId(), session.getUserId());
            qa.setSessionEnded(true);
        }

        return qa;
    }

    @Override
    public InterviewQa askFollowUp(Long parentQaId) {
        InterviewQa parent = qaMapper.selectById(parentQaId);
        if (parent == null) {
            throw new BusinessException("原题目不存在");
        }

        String followUp = aiService.chat(
                "你是一个面试官。请根据候选人的回答进行追问，深入考察。只返回追问的问题本身。",
                "原题：" + parent.getQuestion() + "\n回答：" + (parent.getUserAnswer() != null ? parent.getUserAnswer() : "未作答"));

        InterviewQa qa = new InterviewQa();
        qa.setSessionId(parent.getSessionId());
        qa.setQuestion(followUp);
        qa.setQuestionType(1);
        qa.setParentQaId(parentQaId);
        qa.setSortOrder(parent.getSortOrder() + 1);
        qaMapper.insert(qa);
        return qa;
    }

    @Override
    @Transactional
    public void endSession(Long sessionId, Long userId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (session.getStatus() == 1) {
            return; // 已结束，避免重复处理
        }
        session.setStatus(1);
        session.setEndTime(LocalDateTime.now());

        List<InterviewQa> qaList = qaMapper.selectList(
                new LambdaQueryWrapper<InterviewQa>().eq(InterviewQa::getSessionId, sessionId));
        if (!qaList.isEmpty()) {
            try {
                String summary = aiService.chat(
                        "你是一个面试总结专家。以下是面试问答记录，请给出综合评价分数和改进建议。",
                        qaList.toString());
                session.setOverallEvaluation(summary);
            } catch (Exception e) {
                log.warn("AI 总结生成失败，不影响结束面试: {}", e.getMessage());
            }
        }
        sessionMapper.updateById(session);
    }

    @Override
    public List<InterviewSession> getHistory(Long userId) {
        return sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .orderByDesc(InterviewSession::getCreatedAt));
    }

    @Override
    public InterviewSession getSession(Long sessionId, Long userId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        return session;
    }

    @Override
    public List<InterviewQa> getQaList(Long sessionId) {
        return qaMapper.selectList(
                new LambdaQueryWrapper<InterviewQa>()
                        .eq(InterviewQa::getSessionId, sessionId)
                        .orderByAsc(InterviewQa::getSortOrder));
    }
}
