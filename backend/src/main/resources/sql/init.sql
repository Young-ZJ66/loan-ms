-- -----------------------------------------------------
-- 贷款管理系统 (Loan-MS) 数据库建表脚本 (MySQL)
-- -----------------------------------------------------
CREATE DATABASE IF NOT EXISTS `loan` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `loan`;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号(可以是手机号)',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `role` TINYINT NOT NULL DEFAULT 0 COMMENT '0-客户, 1-管理员',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基础用户表';

-- 2. 实名资料表
CREATE TABLE IF NOT EXISTS `user_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '系统用户ID',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `id_card` VARCHAR(20) NOT NULL COMMENT '身份证号',
  `id_card_front` VARCHAR(255) DEFAULT NULL COMMENT '身份证正面照',
  `id_card_back` VARCHAR(255) DEFAULT NULL COMMENT '身份证反面照',
  `bank_name` VARCHAR(100) DEFAULT NULL COMMENT '开户银行',
  `bank_card` VARCHAR(50) DEFAULT NULL COMMENT '银行卡号',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系手机',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '电子信箱',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核, 1-审核通过, 2-驳回',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户实名认证(KYC)表';

-- 3. 用户授信额度表
CREATE TABLE IF NOT EXISTS `user_credit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `total_credit` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '总授信额度',
  `used_credit` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '已使用未结清额度(含冻结)',
  `available_credit` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '当前可用额度',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-正常使用, 0-被管理员冻结',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='授信额度表';

-- 3.5 贷款产品表
CREATE TABLE IF NOT EXISTS `loan_product` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '产品ID',
  `name` varchar(128) NOT NULL COMMENT '产品名称',
  `type` tinyint(2) NOT NULL DEFAULT '0' COMMENT '类型：0消费贷 1经营贷 2房贷 3车贷',
  `annual_rate` decimal(7,4) NOT NULL COMMENT '年化利率(小数形式)',
  `min_amount` decimal(15,2) NOT NULL COMMENT '最低限额',
  `max_amount` decimal(15,2) NOT NULL COMMENT '最高限额',
  `min_term` int(11) NOT NULL COMMENT '最低期限(月)',
  `max_term` int(11) NOT NULL COMMENT '最高期限(月)',
  `description` varchar(512) DEFAULT NULL COMMENT '产品介绍',
  `status` tinyint(2) NOT NULL DEFAULT '0' COMMENT '状态：0下架 1上架中',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款产品库';

-- 4. 贷款申请单表
CREATE TABLE IF NOT EXISTS `loan_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT DEFAULT NULL COMMENT '关联产品ID',
  `amount` DECIMAL(15,2) NOT NULL COMMENT '申请贷款金额',
  `term_months` INT NOT NULL COMMENT '分期数(如3,6,12)',
  `annual_rate` DECIMAL(6,4) NOT NULL COMMENT '约定年化利率(如0.048表示4.8%)',
  `purpose` VARCHAR(200) DEFAULT NULL COMMENT '贷款用途',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审批, 1-审批通过(已放款), 2-审批驳回, 3-已结清',
  `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `audit_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户贷款申请表';

-- 5. 还款计划单表(等额本息生成的账单明细)
CREATE TABLE IF NOT EXISTS `repayment_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `loan_id` BIGINT NOT NULL COMMENT '贷款申请ID',
  `user_id` BIGINT NOT NULL COMMENT '贷款人',
  `term_index` INT NOT NULL COMMENT '第几期',
  `principal` DECIMAL(15,2) NOT NULL COMMENT '当期应还本金',
  `interest` DECIMAL(15,2) NOT NULL COMMENT '当期应还利息',
  `penalty` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '逾期产生且未缴清的日滚罚息总计',
  `total_amount` DECIMAL(15,2) NOT NULL COMMENT '本+息+罚息（应还总计）',
  `due_date` DATE NOT NULL COMMENT '最迟还款日(不含时分秒)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待还, 1-已还清, 2-逾期中',
  `settled_time` DATETIME DEFAULT NULL COMMENT '实际完全结清的时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_loan_id` (`loan_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='期供还款计划(账单)表';

-- 6. 还款流水记录表
CREATE TABLE IF NOT EXISTS `repayment_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_id` BIGINT DEFAULT NULL COMMENT '挂钩的计划ID(如果是针对某一期。如果有提前结清可能挂空或特殊标记)',
  `loan_id` BIGINT NOT NULL COMMENT '所属贷款单',
  `user_id` BIGINT NOT NULL,
  `pay_amount` DECIMAL(15,2) NOT NULL COMMENT '实际支付金额',
  `pay_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1-正常按期还款, 2-逾期后还款清欠, 3-提前全额结清',
  `pay_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_loan_id` (`loan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史还款流水记录表';

-- 7. 催收动作纪要表
CREATE TABLE IF NOT EXISTS `collection_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_id` BIGINT NOT NULL COMMENT '针对哪个逾期的计划发起',
  `loan_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL COMMENT '被催收人',
  `admin_id` BIGINT NOT NULL COMMENT '执行催收的管理员',
  `method` VARCHAR(50) NOT NULL COMMENT '催收手段(站内信/电话/法务)',
  `result` VARCHAR(255) NOT NULL COMMENT '反馈/结果描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员催收纪要表';

-- 8. 站内通知消息表
CREATE TABLE IF NOT EXISTS `sys_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `to_user_id` BIGINT NOT NULL COMMENT '收件人ID',
  `title` VARCHAR(100) NOT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '正文',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未读, 1-已读',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_to_user_id` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统广播或点对点站内信';

-- 8.5 额度申请审核单表
CREATE TABLE IF NOT EXISTS `credit_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '系统用户ID',
  `requested_amount` DECIMAL(15,2) NOT NULL COMMENT '用户期望申请的总额度（提额或初建）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核, 1-审核通过, 2-驳回',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='额度申请(提额/首建)工单表';

-- 8.6 解冻申请工单表
CREATE TABLE IF NOT EXISTS `unfreeze_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '系统用户ID',
  `reason` VARCHAR(255) NOT NULL COMMENT '解冻申请理由',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核, 1-解冻通过, 2-驳回',
  `admin_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解冻申请工单表';

-- -----------------------------------------------------
-- 9. 系统初始数据集
-- 提示: 默认账号初始密码为 123456 (系统以MD5鉴权)
-- MD5: e10adc3949ba59abbe56e057f20f883e
-- -----------------------------------------------------

-- 初始化默认系统账号与用户实体
REPLACE INTO `sys_user` (`id`, `username`, `password`, `role`) VALUES 
(1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1),
(2, 'user1', 'e10adc3949ba59abbe56e057f20f883e', 0),
(3, 'user2', 'e10adc3949ba59abbe56e057f20f883e', 0),
(4, 'user3', 'e10adc3949ba59abbe56e057f20f883e', 0);

-- 已认证用户预置资料
REPLACE INTO `user_profile` (`user_id`, `real_name`, `id_card`, `id_card_front`, `id_card_back`, `bank_name`, `bank_card`, `phone`, `email`, `status`) VALUES 
(2, '李四', '11010519990101888X', '', '', '中国建设银行', '6227000012345678', '13800138001', 'lisi@example.com', 1);

-- 待审批状态用户资料
REPLACE INTO `user_profile` (`user_id`, `real_name`, `id_card`, `id_card_front`, `id_card_back`, `bank_name`, `bank_card`, `phone`, `email`, `status`) VALUES 
(3, '王五', '11010520000101999X', '', '', '招商银行', '6222020088889999', '13900139001', 'wangwu@example.com', 0);

-- 已认证用户预置资料
REPLACE INTO `user_profile` (`user_id`, `real_name`, `id_card`, `id_card_front`, `id_card_back`, `bank_name`, `bank_card`, `phone`, `email`, `status`) VALUES 
(4, '赵六', '11010520010101777X', '', '', '中国农业银行', '6228480312123434', '13700137001', 'zhaoliu@example.com', 1);

-- 初始授信记录预置
REPLACE INTO `user_credit` (`user_id`, `total_credit`, `used_credit`, `available_credit`, `status`) VALUES 
(2, 100000.00, 10000.00, 90000.00, 1),
(4, 50000.00, 20000.00, 30000.00, 1);

-- 8.8 系统预置金融产品列表
REPLACE INTO `loan_product` (`id`, `name`, `type`, `annual_rate`, `min_amount`, `max_amount`, `min_term`, `max_term`, `description`, `status`) VALUES 
(1, '惠民消费贷', 0, 0.0480, 1000.00, 50000.00, 3, 12, '面向普通用户的日常消费贷款，下款快，利率低。', 1),
(2, '尊享经营贷', 1, 0.0380, 50000.00, 500000.00, 6, 36, '服务于小微企业主的专项经营贷款，额度高，期限长。', 1),
(3, '安居按揭贷', 2, 0.0315, 100000.00, 2000000.00, 12, 360, '支持首套及二套购房的长期限贷款，利率优惠。', 1),
(4, '车主专享贷', 3, 0.0420, 20000.00, 300000.00, 12, 60, '凭名下车辆办理，额度充足，随借随还。', 1);

-- 历史履约中贷款申请(包含产品关联关系)
REPLACE INTO `loan_application` (`id`, `user_id`, `product_id`, `amount`, `term_months`, `annual_rate`, `purpose`, `status`) VALUES 
(1, 2, 1, 10000.00, 3, 0.048, '房屋装修及家电采购', 1);

-- 待审批贷款申请档案
REPLACE INTO `loan_application` (`id`, `user_id`, `product_id`, `amount`, `term_months`, `annual_rate`, `purpose`, `status`) VALUES 
(2, 4, 1, 20000.00, 6, 0.048, '进货资金周转', 0);

-- 生成默认的等额本息还款计划明细
REPLACE INTO `repayment_plan` (`loan_id`, `user_id`, `term_index`, `principal`, `interest`, `penalty`, `total_amount`, `due_date`, `status`) VALUES 
(1, 2, 1, 3319.46, 40.00, 0, 3359.46, '2026-05-15', 0),
(1, 2, 2, 3333.58, 25.88, 0, 3359.46, '2026-06-15', 0),
(1, 2, 3, 3346.96, 12.50, 0, 3359.46, '2026-07-15', 0);
