# Loan-MS 贷款管理系统

基于 Spring Boot + Vue 3 的贷款申请与审批管理系统。

## 技术栈

### 后端
- **框架**: Spring Boot 3.5.13
- **ORM**: MyBatis 3.0.5
- **数据库**: MySQL 8.0+
- **认证**: JWT (jjwt 0.11.5，HS256，2h 有效期，支持黑名单吊销)
- **密码加密**: BCrypt (strength=12)
- **接口文档**: SpringDoc OpenAPI 2.8.6
- **AOP**: spring-boot-starter-aop
- **Java版本**: 17

### 前端
- **框架**: Vue 3.5.32
- **UI组件库**: Element Plus 2.13.7
- **路由**: Vue Router 5.0.4
- **状态管理**: Pinia 3.0.4
- **HTTP客户端**: Axios 1.15.0
- **图表库**: ECharts 6.1.0（按需引入）
- **JWT解析**: jwt-decode 4.0.0
- **构建工具**: Vite 8.0.4

## 功能模块

### 客户端功能
- 用户注册/登录
- KYC实名认证
- 贷款产品浏览
- 在线贷款申请
- 额度申请/提额
- 还款计划查看
- 在线还款
- 站内消息通知
- 账户解冻申请

### 管理员端功能
- KYC资料审核
- 贷款申请审批与放款
- 额度申请审批
- 贷款产品管理
- 还款管理与催收记录
- 财务统计与数据看板
- 系统消息发送
- 用户账户冻结/解冻
- 逾期账单自动扫描与罚息计算

## 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 数据库配置

1. 创建数据库

```sql
CREATE DATABASE loan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本

```bash
# 文件位置：backend/src/main/resources/sql/init.sql
```

3. 修改数据库连接配置

```yaml
# 文件位置：backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/loan
    username: root
    password: 1234
```

### 文件上传目录配置

系统上传的证件影像等文件存储在本地磁盘，默认存储路径为 `backend/uploads/`（相对后端启动目录）。

```yaml
# 文件位置：backend/src/main/resources/application.yml
upload:
  dir: uploads/
```

> **说明**：
> - 默认配置为相对路径 `uploads/`，请务必在 `backend/` 目录下启动后端，文件将存储在 `backend/uploads/`。
> - 如需更改存储位置，可修改为绝对路径（如 `D:/loan-ms/uploads/`）或相对启动目录的路径。
> - 上传接口会在首次上传时自动创建目录，无需手动创建。

### 后端启动

```bash
cd backend
# 使用 Maven 编译运行
./mvnw spring-boot:run
# Windows
mvnw.cmd spring-boot:run
```

后端服务默认运行在 `http://localhost:8080`

接口文档地址: `http://localhost:8080/doc.html`

### 前端启动

```bash
cd frontend
# 安装依赖
npm install
# 启动开发服务器
npm run dev
```

前端默认运行在 `http://localhost:5173`

## 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | 123456 | 系统管理员 |
| 客户 | user1 | 123456 | 已认证客户 |
| 客户 | user2 | 123456 | 待审核认证 |
| 客户 | user3 | 123456 | 已认证客户 |

> 注：以上账号为数据库预置的测试账号，可直接登录。新注册用户的密码需满足：长度≥8位、同时包含字母和数字、不在弱口令黑名单中。

## 项目结构

```
loan-ms/
├── backend/              # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/young/
│   │   │   │   ├── controller/      # 控制器
│   │   │   │   ├── service/         # 业务逻辑接口
│   │   │   │   │   └── impl/        # 业务逻辑实现
│   │   │   │   ├── mapper/          # MyBatis映射
│   │   │   │   ├── pojo/            # 实体类
│   │   │   │   ├── config/          # 配置类
│   │   │   │   ├── utils/           # 工具类
│   │   │   │   ├── task/            # 定时任务
│   │   │   │   └── common/          # 公共类（异常、拦截器、注解、AOP）
│   │   │   └── resources/
│   │   │       ├── sql/             # 数据库脚本
│   │   │       └── application.yml  # 配置文件
│   │   └── test/
│   └── pom.xml
└── frontend/             # 前端项目
    ├── src/
    │   ├── api/          # API 请求模块
    │   ├── constants/    # 业务常量与脱敏函数
    │   ├── stores/       # Pinia 状态管理
    │   ├── layout/       # 布局组件
    │   ├── router/       # 路由配置
    │   ├── utils/        # 工具函数（请求封装、格式化、echarts按需引入）
    │   └── views/        # 页面组件
    │       ├── admin/    # 管理端页面
    │       └── client/   # 客户端页面
    ├── index.html
    └── package.json
```

## 核心数据表

| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户表 |
| `user_profile` | 用户实名认证表 |
| `user_credit` | 用户授信额度表 |
| `loan_product` | 贷款产品表 |
| `loan_application` | 贷款申请表 |
| `repayment_plan` | 还款计划表 |
| `repayment_record` | 还款记录表 |
| `collection_record` | 催收记录表 |
| `sys_message` | 系统消息表 |
| `credit_application` | 额度申请表 |
| `unfreeze_application` | 解冻申请表 |

## 联系方式

如有问题或需要商业服务，欢迎联系：

- **QQ**: `1600386893`
- **服务内容**: 付费部署、定制修改、功能扩展等

## License

MIT License
