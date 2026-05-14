package com.young.pojo;

import lombok.Data;
import java.util.Date;

@Data
public class CollectionRecord {
    private Long id;
    private Long planId;
    private Long loanId;
    private Long userId;
    private Long adminId;
    private String method;
    private String result;
    private Date createTime;
}
