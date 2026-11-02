package com.quickcart.catalog.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminProductResponse(Long id, Long categoryId, String categoryName, String name, String description,
                                   BigDecimal price, String unit, String imageUrl,
                                   int availableQuantity, boolean inStock, boolean active,
                                   Instant createdAt, Instant updatedAt) {
}