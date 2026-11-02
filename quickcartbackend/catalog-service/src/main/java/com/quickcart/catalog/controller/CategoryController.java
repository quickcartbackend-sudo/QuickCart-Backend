package com.quickcart.catalog.controller;

import com.quickcart.catalog.dto.response.CategoryResponse;
import com.quickcart.catalog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @Operation(summary = "C1 Active categories")
    @GetMapping
    public List<CategoryResponse> list() {
        return service.listActive();
    }
}