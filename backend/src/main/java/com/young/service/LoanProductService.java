package com.young.service;

import com.young.pojo.LoanProduct;
import java.util.List;

/**
 * 贷款产品服务接口
 */
public interface LoanProductService {
    /** 查询所有产品（管理端） */
    List<LoanProduct> getAllProducts();
    /** 查询上架中的产品（客户端） */
    List<LoanProduct> getActiveProducts();
    /** 新增产品 */
    void addProduct(LoanProduct product);
    /** 编辑产品 */
    void updateProduct(LoanProduct product);
    /** 切换上下架状态 */
    void toggleStatus(Long id);
    /** 根据 id 查询单个产品 */
    LoanProduct getById(Long id);
}
