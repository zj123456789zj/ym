package com.yumian.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yumian.entity.KnowledgeImportLog;
import com.yumian.entity.KnowledgeQuestion;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeQuestionService {
    /** 分页查询 */
    IPage<KnowledgeQuestion> page(int pageNum, int pageSize, String category, String keyword);

    /** 根据 ID 查询 */
    KnowledgeQuestion getById(Long id);

    /** 新增（自动生成向量） */
    KnowledgeQuestion add(KnowledgeQuestion question, Long adminId);

    /** 编辑（重新生成向量） */
    KnowledgeQuestion update(KnowledgeQuestion question);

    /** 删除 */
    void delete(Long id);

    /** 批量导入（支持 .xlsx / .json） */
    KnowledgeImportLog importFile(MultipartFile file, Long adminId);

    /** 导出为 JSON */
    String exportJson();

    /** 获取所有启用的题目（含向量，用于 RAG 检索） */
    List<KnowledgeQuestion> getAllEnabled();

    /** 随机获取一条未出过的启用题目 */
    KnowledgeQuestion getRandomEnabled(List<Long> excludeIds);

    /** 导入记录列表 */
    List<KnowledgeImportLog> getImportLogs(Long adminId);
}
