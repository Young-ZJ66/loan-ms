package com.young.pojo;

import lombok.Data;
import java.util.Date;

@Data
public class SysMessage {
    private Long id;
    private Long toUserId;
    private String title;
    private String content;
    /**
     * 0-未读, 1-已读
     */
    private Integer isRead;
    private Date createTime;
}
