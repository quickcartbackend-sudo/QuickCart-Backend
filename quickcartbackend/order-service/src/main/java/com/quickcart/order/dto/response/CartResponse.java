package com.quickcart.order.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartLineResponse> items, int itemCount, BigDecimal subtotal,
                           BigDecimal deliveryFee, BigDecimal total, boolean hasUnavailableItems) {
}