# 鱼面 (Yumian)

面试辅导系统 — 模拟面试、简历优化、错题本、知识库 RAG

## 技术栈

**前端**
- Vue 3 + Vite
- Vue Router + Pinia
- Element Plus
- ECharts (数据可视化)
- Axios

**后端**
- Spring Boot 3.2.4
- MyBatis-Plus
- MySQL + Redis
- JWT 认证
- DeepSeek / Ollama AI 集成

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis

### 启动后端

```bash
cd java
cp src/main/resources/application-example.yml src/main/resources/application.yml
# 编辑 application.yml 配置数据库、Redis、AI API 密钥
mvn spring-boot:run
```

### 启动前端

```bash
cd front
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`

## 功能模块

- **模拟面试** — AI 驱动的模拟面试，支持多轮对话
- **简历优化** — 简历上传与 AI 优化建议
- **错题本** — 自动收录错题，针对性复习
- **知识库** — RAG 知识库，导入资料智能问答
- **学习统计** — 学习趋势图表与数据分析
- **专项训练** — 针对薄弱环节强化练习
- **面试复盘** — 面试记录回看与分析

## 项目结构

```
ym/
├── java/          # Spring Boot 后端
│   └── src/main/java/com/yumian/
│       ├── common/      # 通用工具与异常处理
│       ├── config/      # 配置类
│       ├── controller/  # REST 控制器
│       ├── dto/         # 数据传输对象
│       ├── entity/      # 数据实体
│       ├── mapper/      # MyBatis Mapper
│       ├── service/     # 业务逻辑
│       └── utils/       # 工具类
├── front/         # Vue 3 前端
│   └── src/
│       ├── api/         # API 接口
│       ├── assets/      # 静态资源
│       ├── layout/      # 布局组件
│       ├── router/      # 路由
│       ├── stores/      # Pinia 状态管理
│       └── views/       # 页面视图
└── docs/          # 项目文档
```

## 许可证

MIT
