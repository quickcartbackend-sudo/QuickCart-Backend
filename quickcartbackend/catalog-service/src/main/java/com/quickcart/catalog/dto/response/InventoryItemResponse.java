package com.quickcart.catalog.dto.response;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record InventoryItemResponse(Long productId, String productName, String unit,
                                    Integer quantity, Instant updatedAt) {
    public InventoryItemResponse {
        updatedAt = updatedAt == null ? null : updatedAt.truncatedTo(ChronoUnit.SECONDS);
    }
}