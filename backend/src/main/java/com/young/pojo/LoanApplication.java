package com.young.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class LoanApplication {
    private Long id;
    private Long userId;
    private BigDecimal amount;
    private Integer termMonths;
    private BigDecimal annualRate;
    private String purpose;
    /**
     * 0-待审批, 1-已放款, 2-驳回, 3-已结清
     */
    private Integer status;
    private Date applyTime;
    private Date auditTime;
    private Date createTime;
    private Date updateTime;
    
    /** 关联的贷款产品ID */
    private Long productId;

    // 关联知展示字段
    private String username;
    private String realName;
    /** 该申请关联的产品名称 */
    private String productName;
}
