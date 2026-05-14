package com.young.pojo;

import lombok.Data;
import java.util.Date;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    /**
     * 0-客户, 1-管理员
     */
    private Integer role;
    private Date createTime;
    private Date updateTime;
}
