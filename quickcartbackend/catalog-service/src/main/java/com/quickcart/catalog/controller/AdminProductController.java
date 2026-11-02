package com.quickcart.catalog.controller;

import com.quickcart.catalog.dto.request.ActiveRequest;
import com.quickcart.catalog.dto.request.PriceRequest;
import com.quickcart.catalog.dto.request.ProductCreateRequest;
import com.quickcart.catalog.dto.request.ProductUpdateRequest;
import com.quickcart.catalog.dto.response.AdminProductResponse;
import com.quickcart.catalog.dto.response.PageResponse;
import com.quickcart.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService service;

    public AdminProductController(ProductService service) {
        this.service = service;
    }

    @Operation(summary = "A5 List all products")
    @GetMapping
    public PageResponse<AdminProductResponse> list(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) Boolean active,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return service.listAdmin(q, categoryId, active, page, size);
    }

    @Operation(summary = "A6 Product (any status)")
    @GetMapping("/{id}")
    public AdminProductResponse get(@PathVariable long id) {
        return service.getAdmin(id);
    }

    @Operation(summary = "A7 Create product + inventory row")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductResponse create(@Valid @RequestBody ProductCreateRequest request) {
        return service.create(request);
    }

    @Operation(summary = "A8 Update product details (not price/stock)")
    @PutMapping("/{id}")
    public AdminProductResponse update(@PathVariable long id, @Valid @RequestBody ProductUpdateRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "A9 Update price")
    @PatchMapping("/{id}/price")
    public AdminProductResponse price(@PathVariable long id, @Valid @RequestBody PriceRequest request) {
        return service.updatePrice(id, request);
    }

    @Operation(summary = "A10 Activate / deactivate (soft delete)")
    @PatchMapping("/{id}/status")
    public AdminProductResponse status(@PathVariable long id, @Valid @RequestBody ActiveRequest request) {
        return service.setActive(id, request.active());
    }
}