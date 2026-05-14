package com.young.controller;

import com.young.common.Result;
import com.young.pojo.LoanProduct;
import com.young.service.LoanProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 贷款产品控制器
 * 提供产品的增删改查及上下架管理能力
 */
@Api(tags = "贷款产品管理")
@RestController
@RequestMapping("/api/product")
public class LoanProductController {

    @Autowired
    private LoanProductService productService;

    /**
     * 查询所有产品（管理端，含下架产品）
     */
    @ApiOperation("查询全部贷款产品")
    @GetMapping("/all")
    public Result<List<LoanProduct>> getAll() {
        return Result.success(productService.getAllProducts());
    }

    /**
     * 查询上架中的产品（客户端使用）
     */
    @ApiOperation("查询上架中的贷款产品")
    @GetMapping("/active")
    public Result<List<LoanProduct>> getActive() {
        return Result.success(productService.getActiveProducts());
    }

    /**
     * 新增贷款产品（管理端）
     */
    @ApiOperation("新增贷款产品")
    @PostMapping("/add")
    public Result<?> add(@RequestBody LoanProduct product) {
        productService.addProduct(product);
        return Result.success("产品创建成功");
    }

    /**
     * 编辑贷款产品（管理端）
     */
    @ApiOperation("编辑贷款产品")
    @PutMapping("/update")
    public Result<?> update(@RequestBody LoanProduct product) {
        productService.updateProduct(product);
        return Result.success("产品信息已更新");
    }

    /**
     * 切换产品上下架状态（管理端）
     */
    @ApiOperation("切换产品上下架状态")
    @PostMapping("/toggle/{id}")
    public Result<?> toggle(@PathVariable Long id) {
        productService.toggleStatus(id);
        return Result.success("产品状态已切换");
    }
}
