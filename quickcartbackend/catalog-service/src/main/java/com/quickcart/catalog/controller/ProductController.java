package com.quickcart.catalog.controller;

import com.quickcart.catalog.dto.response.PageResponse;
import com.quickcart.catalog.dto.response.ProductResponse;
import com.quickcart.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @Operation(summary = "C2 Browse / search active products")
    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(defaultValue = "name") String sort,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.listPublic(q, categoryId, sort, page, size);
    }

    @Operation(summary = "C3 Product details")
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable long id) {
        return service.getPublic(id);
    }
}