package com.quickcart.catalog.dto.response;

import java.math.BigDecimal;

public record InternalProductSnapshot(Long id, String name, String unit, String imageUrl,
                                      BigDecimal price, boolean active, int availableQuantity) {
}