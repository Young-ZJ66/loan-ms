package com.young.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RepaymentRecord {
    private Long id;
    private Long planId;
    private Long loanId;
    private Long userId;
    private BigDecimal payAmount;
    /**
     * 1-正常按期还款, 2-逾期后清欠, 3-提前全额结清
     */
    private Integer payType;
    private Date payTime;
    private String remark;
    private Date createTime;
}
