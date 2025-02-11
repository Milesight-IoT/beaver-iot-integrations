package com.milesight.beaveriot.integration.aws.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.integration.aws.model.ProductResponseData;
import com.milesight.beaveriot.integration.aws.model.SearchProductRequest;
import com.milesight.beaveriot.integration.aws.model.parser.BatchDeleteProductRequest;
import com.milesight.beaveriot.integration.aws.model.parser.ProductRequest;
import com.milesight.beaveriot.integration.aws.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产品控制器
 */
@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseBody<Object> createProduct(@RequestBody ProductRequest productRequest) {
        Boolean result = productService.createProduct(productRequest);
        if (Boolean.FALSE.equals(result)) {
            return ResponseBuilder.fail("Failed to create product", "Failed to create product");
        }
        return ResponseBuilder.success();
    }

    @PostMapping("/search")
    public ResponseBody<Page<ProductResponseData>> search(@RequestBody SearchProductRequest searchProductRequest) {
        return ResponseBuilder.success(productService.searchProduct(searchProductRequest));
    }

    @PostMapping("/batch-delete")
    public  ResponseBody<Void>  batchDelete(@RequestBody BatchDeleteProductRequest batchDeleteProductRequest) {
        productService.batchDeleteProducts(batchDeleteProductRequest.getProductIdList());
        return ResponseBuilder.success();
    }

}
