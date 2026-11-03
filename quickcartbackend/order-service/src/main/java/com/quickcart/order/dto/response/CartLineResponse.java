package com.quickcart.order.dto.response;

import java.math.BigDecimal;

public record CartLineResponse(Long productId, String name, String unit, String imageUrl,
                               BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                               int availableQuantity, boolean available) {
}