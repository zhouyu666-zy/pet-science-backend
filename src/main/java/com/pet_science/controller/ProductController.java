package com.pet_science.controller;

import com.pet_science.annotation.RequireAdmin;
import com.pet_science.annotation.RequireUser;
import com.pet_science.exception.BusinessException;
import com.pet_science.pojo.PageResult;
import com.pet_science.pojo.product.Product;
import com.pet_science.pojo.Result;
import com.pet_science.service.ProductService;
import com.pet_science.utils.JWTUtil;
import com.pet_science.utils.UserActivityTracker;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/product")
@Api(tags = "产品接口")
public class ProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private UserActivityTracker trackUserActivity; // 用户活跃信息记录

    @GetMapping("/list")
    @RequireUser
    @ApiOperation(value = "获取产品列表", notes = "支持按产品名称、类别、状态等筛选，支持分页查询")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "pageNum", value = "页码", dataType = "Integer"),
        @ApiImplicitParam(name = "pageSize", value = "每页记录数", dataType = "Integer"),
        @ApiImplicitParam(name = "name", value = "产品名称", dataType = "String"),
        @ApiImplicitParam(name = "category", value = "产品类别", dataType = "String"),
        @ApiImplicitParam(name = "status", value = "状态", dataType = "Integer")
    })
    public Result<PageResult<Product>> getProductList(@RequestParam(required = false) Map<String, Object> params,@RequestHeader("Authorization") String token) {
        int verify = JWTUtil.verifyToken(token);
        if (verify != 1) { // 如果不是管理员，则只显示上架的产品
            params.put("status", 1);
             // 统计用户活跃信息
            trackUserActivity.trackUserActivity(token);
        }
        // 获取分页参数
        Integer pageNum = params.get("pageNum") != null ? Integer.parseInt(params.get("pageNum").toString()) : 1;
        Integer pageSize = params.get("pageSize") != null ? Integer.parseInt(params.get("pageSize").toString()) : 10;
        
        // 调用服务层方法获取分页数据
        PageResult<Product> pageResult = productService.getProductListPage(pageNum, pageSize, params);
        return Result.successResultData(pageResult);
    }

    @GetMapping("/search")
    @RequireUser
    @ApiOperation(value = "搜索产品", notes = "根据产品名称、类别等进行模糊搜索")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "pageNum", value = "页码", dataType = "Integer"),
        @ApiImplicitParam(name = "pageSize", value = "每页记录数", dataType = "Integer"),
        @ApiImplicitParam(name = "keyword", value = "搜索关键字", dataType = "String")
    })
    public Result<PageResult<Product>> searchProducts(@RequestParam Map<String, Object> params,@RequestHeader("Authorization") String token) {
        Object keyword = params.get("keyword");
        if(keyword == null || keyword.toString().isEmpty()){
            throw new BusinessException("关键字不能为空");
        }
        return getProductList(params, token);
    }
    
    @GetMapping("/{id}")
    @ApiOperation(value = "获取产品详情", notes = "根据产品ID获取产品详细信息")
    @ApiImplicitParam(name = "id", value = "产品ID", required = true, dataType = "Integer", paramType = "path")
    public Result<Product> getProductDetail(@PathVariable("id") Integer id) {
        Product product = productService.getProductDetail(id);
        return Result.successResultData(product);
    }
    
    @PostMapping
    @RequireAdmin
    @ApiOperation(value = "创建产品", notes = "创建新产品")
    public Result<Product> createProduct(@RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return Result.successResultData(createdProduct);
    }
    
    @PutMapping("/{productId}")
    @RequireAdmin
    @ApiOperation(value = "更新产品", notes = "更新指定产品的信息")
    @ApiImplicitParam(name = "productId", value = "产品ID", required = true, dataType = "Integer", paramType = "path")
    public Result<Product> updateProduct(@PathVariable Integer productId, @RequestBody Product product) {
        product.setProductId(productId);
        Product updatedProduct = productService.updateProduct(product);
        return Result.successResultData(updatedProduct);
    }
    
    @DeleteMapping("/{id}")
    @RequireAdmin
    @ApiOperation(value = "删除产品", notes = "删除指定产品")
    @ApiImplicitParam(name = "id", value = "产品ID", required = true, dataType = "Integer", paramType = "path")
    public Result<String> deleteProduct(@PathVariable("id") Integer id) {
        boolean result = productService.deleteProduct(id);
        if (result) {
            return Result.successResultData("删除产品成功");
        }
        throw new BusinessException("删除产品失败");
    }
}