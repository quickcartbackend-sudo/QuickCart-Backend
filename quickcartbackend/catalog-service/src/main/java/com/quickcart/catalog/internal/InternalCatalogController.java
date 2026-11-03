package com.quickcart.catalog.internal;

import com.quickcart.catalog.dto.request.InternalProductBatchRequest;
import com.quickcart.catalog.dto.request.InternalStockRequest;
import com.quickcart.catalog.dto.response.InternalProductBatchResponse;
import com.quickcart.catalog.dto.response.InternalReserveResponse;
import com.quickcart.catalog.inventory.InventoryService;
import com.quickcart.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Called only by order-service. Not routed by the gateway; protected by X-Internal-Api-Key. */
@Hidden
@RestController
@RequestMapping("/internal")
public class InternalCatalogController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    public InternalCatalogController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @PostMapping("/products/batch")                                   // I1
    public InternalProductBatchResponse batch(@Valid @RequestBody InternalProductBatchRequest request) {
        return new InternalProductBatchResponse(productService.snapshots(request.productIds()));
    }

    @PostMapping("/inventory/reserve")                                // I2
    public InternalReserveResponse reserve(@Valid @RequestBody InternalStockRequest request) {
        return new InternalReserveResponse(inventoryService.reserve(request.items()));
    }

    @PostMapping("/inventory/release")                                // I3
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@Valid @RequestBody InternalStockRequest request) {
        inventoryService.release(request.items());
    }
}