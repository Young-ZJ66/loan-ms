package com.young.pojo;

import lombok.Data;
import java.util.Date;

@Data
public class UserProfile {
    private Long id;
    private Long userId;
    private String realName;
    private String idCard;
    private String idCardFront;
    private String idCardBack;
    private String bankName;
    private String bankCard;
    /** 联系手机号 */
    private String phone;
    /** 电子信箱 */
    private String email;
    /**
     * 0-待审核, 1-审核通过, 2-驳回
     */
    private Integer status;
    private Date auditTime;
    private Date createTime;
    private Date updateTime;
    
    // 冗余展示字段
    private String username;
}
