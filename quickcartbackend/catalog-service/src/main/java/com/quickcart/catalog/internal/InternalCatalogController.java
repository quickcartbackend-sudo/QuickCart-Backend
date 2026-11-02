package com.quickcart.catalog.internal;

import com.quickcart.catalog.dto.request.InternalProductBatchRequest;
import com.quickcart.catalog.dto.response.InternalProductBatchResponse;
import com.quickcart.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Called only by order-service. Not routed by the gateway; protected by X-Internal-Api-Key. */
@Hidden
@RestController
@RequestMapping("/internal")
public class InternalCatalogController {

    private final ProductService productService;

    public InternalCatalogController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products/batch")
    public InternalProductBatchResponse batch(@Valid @RequestBody InternalProductBatchRequest request) {
        return new InternalProductBatchResponse(productService.snapshots(request.productIds()));
    }
}