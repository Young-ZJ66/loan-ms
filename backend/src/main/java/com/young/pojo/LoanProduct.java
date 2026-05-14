package com.young.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 贷款产品实体
 * 管理员配置，客户在申请时选择
 */
@Data
public class LoanProduct {
    private Long id;
    /** 产品名称，如"极速周转贷"、"惠民消费贷" */
    private String name;
    /**
     * 产品类型：0-消费贷, 1-经营贷, 2-房贷, 3-车贷
     */
    private Integer type;
    /** 年化利率，如 0.0480 = 4.8% */
    private BigDecimal annualRate;
    /** 最低贷款金额（元） */
    private BigDecimal minAmount;
    /** 最高贷款金额（元） */
    private BigDecimal maxAmount;
    /** 最短贷款期限（月） */
    private Integer minTerm;
    /** 最长贷款期限（月） */
    private Integer maxTerm;
    /** 面向用户的产品说明文字 */
    private String description;
    /**
     * 上架状态：0-已下架, 1-上架中
     */
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
