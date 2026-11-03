package com.quickcart.order.client;

import java.math.BigDecimal;

public record CatalogProduct(Long id, String name, String unit, String imageUrl,
                             BigDecimal price, boolean active, int availableQuantity) {
}