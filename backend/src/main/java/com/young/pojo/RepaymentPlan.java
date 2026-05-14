package com.young.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RepaymentPlan {
    private Long id;
    private Long loanId;
    private Long userId;
    private Integer termIndex;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal penalty;
    private BigDecimal totalAmount;
    private Date dueDate;
    /**
     * 0-待还, 1-已还清, 2-逾期中
     */
    private Integer status;
    private Date settledTime;
    
    /** 用于关联查询时临时承载客户姓名 */
    private String remark;
    
    private Date createTime;
    private Date updateTime;
}
