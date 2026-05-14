package com.young.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class UserCredit {
    private Long id;
    private Long userId;
    private BigDecimal totalCredit;
    private BigDecimal usedCredit;
    private BigDecimal availableCredit;
    /**
     * 1-正常使用, 0-被管理员冻结
     */
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
