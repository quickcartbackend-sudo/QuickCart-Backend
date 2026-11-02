package com.quickcart.catalog.controller;

import com.quickcart.catalog.dto.request.ActiveRequest;
import com.quickcart.catalog.dto.request.CategoryRequest;
import com.quickcart.catalog.dto.response.CategoryResponse;
import com.quickcart.catalog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService service;

    public AdminCategoryController(CategoryService service) {
        this.service = service;
    }

    @Operation(summary = "A1 All categories incl. inactive")
    @GetMapping
    public List<CategoryResponse> list() {
        return service.listAll();
    }

    @Operation(summary = "A2 Create category")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return service.create(request);
    }

    @Operation(summary = "A3 Update category")
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "A4 Activate / deactivate category")
    @PatchMapping("/{id}/status")
    public CategoryResponse status(@PathVariable long id, @Valid @RequestBody ActiveRequest request) {
        return service.setActive(id, request.active());
    }
}