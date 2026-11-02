package com.quickcart.catalog.inventory;

import com.quickcart.catalog.dto.request.InventoryUpdateRequest;
import com.quickcart.catalog.dto.response.InventoryItemResponse;
import com.quickcart.catalog.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @Operation(summary = "A11 Stock list")
    @GetMapping
    public PageResponse<InventoryItemResponse> list(@RequestParam(required = false) String q,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.list(q, page, size);
    }

    @Operation(summary = "A12 Set stock quantity (absolute value)")
    @PutMapping("/{productId}")
    public InventoryItemResponse set(@PathVariable long productId,
                                     @Valid @RequestBody InventoryUpdateRequest request) {
        return service.setQuantity(productId, request.quantity());
    }
}