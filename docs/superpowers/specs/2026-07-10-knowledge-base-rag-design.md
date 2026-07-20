# 知识库 RAG 模拟面试系统设计

## 背景

"ym" 模拟面试平台目前由 AI 随机生成面试题，题目来源不可控、与用户实际备考方向可能脱节。用户希望自建面试题库（知识库），通过 RAG（检索增强生成）方式让 AI 基于题库出题，支持直接抽取原题或改编后出题。

## 数据层

### 新建表：`knowledge_question`

```sql
CREATE TABLE knowledge_question (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    question    TEXT         NOT NULL COMMENT '题目内容',
    reference_answer TEXT    COMMENT '参考答案',
    category    VARCHAR(100) NOT NULL COMMENT '分类：Java/数据库/系统设计/项目/...',
    difficulty  TINYINT      DEFAULT 1 COMMENT '难度 1-5',
    tags        VARCHAR(500) COMMENT '标签，逗号分隔',
    embedding   JSON         COMMENT '语义向量，数字数组',
    status      TINYINT      DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_by  BIGINT       COMMENT '创建人（admin 用户 ID）',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

向量直接存 MySQL JSON 列。由于数据量级为数百条，每次出题时全量加载到内存做余弦相似度计算，性能足够。

### 新建表：`knowledge_import_log`

```sql
CREATE TABLE knowledge_import_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name     VARCHAR(255),
    total_count   INT,
    success_count INT,
    fail_count    INT,
    created_by    BIGINT,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 修改表：`user`

```sql
ALTER TABLE user ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin/user';
```

初始化 admin 账号：手写 SQL 插入一条 role='admin' 的记录。

## Embedding 层

### 接口

```java
public interface EmbeddingService {
    float[] embed(String text);
}
```

### 实现：OllamaEmbeddingService

基于 Ollama 的 `/api/embeddings` 端点，模型使用 `nomic-embed-text`（轻量，274MB）。

```json
POST http://localhost:11434/api/embeddings
{
    "model": "nomic-embed-text",
    "prompt": "Java 线程池的核心参数有哪些？"
}
```

**用户需执行：** `ollama pull nomic-embed-text`

### 触发时机

- **导入题目时**：Admin 导入每道题后自动调用 embed() 生成向量，存入 embedding 字段
- **出题时**：用当前面试上下文（面试类型 + 分类）生成查询向量，用于相似度匹配

## 后端架构

### 新增文件

| 文件 | 说明 |
|------|------|
| `entity/KnowledgeQuestion.java` | 题库实体 |
| `entity/KnowledgeImportLog.java` | 导入记录实体 |
| `mapper/KnowledgeQuestionMapper.java` | MyBatis-Plus Mapper |
| `mapper/KnowledgeImportLogMapper.java` | 导入记录 Mapper |
| `service/EmbeddingService.java` | Embedding 接口 |
| `service/impl/OllamaEmbeddingServiceImpl.java` | Ollama 向量生成 |
| `service/KnowledgeQuestionService.java` | 题库服务接口 |
| `service/impl/KnowledgeQuestionServiceImpl.java` | 题库服务实现 |
| `controller/KnowledgeController.java` | 题库管理 API |

### 管理 API

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/admin/questions` | admin | 分页列表，支持分类筛选/关键词搜索 |
| POST | `/api/admin/questions` | admin | 新增（自动生成向量） |
| PUT | `/api/admin/questions/{id}` | admin | 编辑（重新生成向量） |
| DELETE | `/api/admin/questions/{id}` | admin | 删除 |
| POST | `/api/admin/questions/import` | admin | 批量导入 Excel/JSON，逐条生成向量 |
| GET | `/api/admin/questions/export` | admin | 导出题库为 JSON |
| GET | `/api/admin/questions/import-logs` | admin | 导入记录列表 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `entity/User.java` | 新增 `role` 字段 |
| `service/impl/InterviewServiceImpl.java` | askQuestion() 增加 RAG 出题分支 |

### RAG 出题流程（InterviewServiceImpl.askQuestion）

```
1. 获取面试会话信息（type、已问题目列表）
2. 构建查询上下文文本 → "面试类型: {type}, 分类: {category}"
3. EmbeddingService.embed(上下文) → 查询向量
4. 从 knowledge_question 加载所有 status=1 的题目及向量
5. 跳过该会话已问过的题目 ID
6. 计算余弦相似度，取 TOP-3
7. 若无匹配（知识库为空或全部已问）→ 降级到原随机出题逻辑
8. 构建 AI 提示词：

   "你是一个技术面试官。以下是知识库中与当前面试相关的参考题：
    参考题1: {question1} (答案: {answer1})
    参考题2: {question2} (答案: {answer2})
    参考题3: {question3} (答案: {answer3})
    
    请出一道面试题。你可以：
    1. 直接使用其中一道原题
    2. 改编其中一道题（换场景、换参数、换问法）
    
    只返回问题本身，不要多余内容。"

9. 调用 AiService.chat() 获取生成的题目
```

## 前端架构

### 新增页面

| 路径 | 文件 | 说明 |
|------|------|------|
| `/admin/questions` | `views/admin/QuestionList.vue` | 题库表格列表 |
| — | 内嵌于 QuestionList | 新增/编辑弹窗 |
| — | 内嵌于 QuestionList | 批量导入弹窗 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `router/index.js` | 新增 admin 路由（带 role 守卫） |
| `layout/MainLayout.vue` | admin 用户显示"题库管理"入口 |
| `api/questionBank.js` | 新建 API 封装 |
| `stores/user.js` | 登录后保存 role 信息 |

### 导入格式

Excel 格式：

| question | reference_answer | category | difficulty | tags |
|----------|-----------------|----------|-----------|------|
| Java 线程池核心参数？ | corePoolSize... | Java | 3 | 线程,并发 |

JSON 格式：

```json
[
  {
    "question": "...",
    "referenceAnswer": "...",
    "category": "Java",
    "difficulty": 3,
    "tags": "线程,并发"
  }
]
```

## 面试类型扩展

现有 type=1（通用技术）、type=2（简历面）。增加 type=3（知识库面），匹配知识库中的题目分类。

## 不受影响的部分

- 错题本（wrong_question）逻辑不变，答错的题仍然自动记入
- 面试回顾、统计分析等页面
- 简历优化功能
- AiService 接口本身不变
- DeepSeek/Ollama 切换机制不变

## 验证方式

1. `ollama pull nomic-embed-text` 拉取 embedding 模型
2. 启动后端，用 admin 登录，进入题库管理页
3. 手动新增几道题 + 导入 Excel/JSON 文件
4. 创建 type=3 面试会话，确认 AI 从知识库出题
5. 多次出题确认：有时直接出原题、有时出改编题
6. 确认已问题目不会重复出现
