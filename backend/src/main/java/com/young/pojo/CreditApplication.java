package com.young.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditApplication {
    private Long id;
    private Long userId;
    private BigDecimal requestedAmount;
    private Integer status; // 0-待审核, 1-审核通过, 2-驳回
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String username;
    private String realName;
    
    private String idCard;
}
