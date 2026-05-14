package com.young.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UnfreezeApplication {
    private Long id;
    private Long userId;
    private String reason;
    private Integer status; // 0-待审核 1-通过 2-驳回
    private Long adminId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 连表查询使用，显示用户实名信息和账号
    private String realName;
    private String username;
}
