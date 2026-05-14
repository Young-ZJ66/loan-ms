# Loan-MS 贷款管理系统

一个基于 Spring Boot + Vue 3 的全栈贷款申请与审批管理系统。

## 技术栈

### 后端
- **框架**: Spring Boot 3.5.13
- **ORM**: MyBatis 3.0.5
- **数据库**: MySQL 8.0+
- **认证**: JWT (jjwt 0.11.5)
- **Java版本**: 17

### 前端
- **框架**: Vue 3.5.32
- **UI组件库**: Element Plus 2.13.7
- **路由**: Vue Router 5.0.4
- **HTTP客户端**: Axios 1.15.0
- **构建工具**: Vite 8.0.4

## 功能模块

### 客户端功能
- 用户注册/登录
- KYC实名认证（提交身份证、银行卡等信息）
- 贷款产品浏览
- 在线贷款申请
- 额度申请/提额
- 还款计划查看
- 在线还款
- 站内消息通知
- 账户解冻申请

### 管理员端功能
- KYC资料审核
- 贷款申请审批
- 额度申请审批
- 贷款产品管理
- 还款管理与催收记录
- 财务统计与数据看板
- 系统消息发送
- 用户账户冻结/解冻

## 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 数据库初始化

1. 创建数据库并执行初始化脚本：
```bash
mysql -u root -p < backend/src/main/resources/sql/init.sql
```

2. 修改数据库配置（如需要）：
编辑 `backend/src/main/resources/application.yml` 中的数据库连接信息。

### 后端启动

```bash
cd backend
# 使用 Maven 编译运行
./mvnw spring-boot:run
# Windows
mvnw.cmd spring-boot:run
```

后端服务默认运行在 `http://localhost:8080`

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

## 项目结构

```
loan-ms/
├── backend/              # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/young/
│   │   │   │   ├── controller/      # 控制器
│   │   │   │   ├── service/         # 业务逻辑
│   │   │   │   ├── mapper/          # MyBatis映射
│   │   │   │   ├── pojo/            # 实体类
│   │   │   │   ├── config/          # 配置类
│   │   │   │   ├── utils/           # 工具类
│   │   │   │   ├── task/            # 定时任务
│   │   │   │   └── common/          # 公共类
│   │   │   └── resources/
│   │   │       ├── sql/             # 数据库脚本
│   │   │       └── application.yml  # 配置文件
│   │   └── test/
│   └── pom.xml
└── frontend/             # 前端项目
    ├── src/
    │   ├── views/        # 页面组件
    │   ├── components/   # 通用组件
    │   ├── router/       # 路由配置
    │   └── utils/        # 工具函数
    ├── index.html
    └── package.json
```

## 核心数据表

- `sys_user` - 系统用户表
- `user_profile` - 用户实名认证表
- `user_credit` - 用户授信额度表
- `loan_product` - 贷款产品表
- `loan_application` - 贷款申请表
- `repayment_plan` - 还款计划表
- `repayment_record` - 还款记录表
- `collection_record` - 催收记录表
- `sys_message` - 系统消息表
- `credit_application` - 额度申请表
- `unfreeze_application` - 解冻申请表

## 联系方式

如有问题或需要商业服务，欢迎联系：

- **QQ**: `1600386893`
- **服务内容**: 付费部署、定制修改、功能扩展等

## License

MIT
