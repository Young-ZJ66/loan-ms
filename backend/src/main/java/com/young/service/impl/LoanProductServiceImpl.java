package com.young.service.impl;

import com.young.mapper.LoanProductMapper;
import com.young.pojo.LoanProduct;
import com.young.service.LoanProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 贷款产品服务实现
 */
@Service
public class LoanProductServiceImpl implements LoanProductService {

    @Autowired
    private LoanProductMapper productMapper;

    @Override
    public List<LoanProduct> getAllProducts() {
        return productMapper.selectAll();
    }

    @Override
    public List<LoanProduct> getActiveProducts() {
        return productMapper.selectActive();
    }

    @Override
    public void addProduct(LoanProduct product) {
        // 新建时默认下架，由管理员手动上架
        product.setStatus(0);
        productMapper.insert(product);
    }

    @Override
    public void updateProduct(LoanProduct product) {
        // 若前端未传 status，则从数据库补回当前值，防止 NOT NULL 约束异常
        if (product.getStatus() == null) {
            LoanProduct existing = productMapper.selectById(product.getId());
            if (existing != null) {
                product.setStatus(existing.getStatus());
            }
        }
        productMapper.update(product);
    }

    @Override
    public void toggleStatus(Long id) {
        LoanProduct product = productMapper.selectById(id);
        if (product == null) return;
        // 0→1 上架, 1→0 下架
        product.setStatus(product.getStatus() == 1 ? 0 : 1);
        productMapper.update(product);
    }

    @Override
    public LoanProduct getById(Long id) {
        return productMapper.selectById(id);
    }
}
