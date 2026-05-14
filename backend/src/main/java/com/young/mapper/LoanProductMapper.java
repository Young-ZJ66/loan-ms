package com.young.mapper;

import com.young.pojo.LoanProduct;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 贷款产品数据访问层
 */
@Mapper
public interface LoanProductMapper {
    /** 新增贷款产品 */
    void insert(LoanProduct product);
    /** 更新贷款产品（全字段覆盖） */
    void update(LoanProduct product);
    /** 根据主键查询 */
    LoanProduct selectById(Long id);
    /** 查询所有产品 */
    List<LoanProduct> selectAll();
    /** 查询所有上架中的产品（客户端使用） */
    List<LoanProduct> selectActive();
}
