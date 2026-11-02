package com.quickcart.catalog.dto.response;

import java.math.BigDecimal;

public record ProductResponse(Long id, Long categoryId, String categoryName, String name, String description,
                              BigDecimal price, String unit, String imageUrl,
                              int availableQuantity, boolean inStock) {
}