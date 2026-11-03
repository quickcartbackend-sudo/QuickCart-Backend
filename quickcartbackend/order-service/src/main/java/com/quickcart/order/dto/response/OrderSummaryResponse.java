package com.quickcart.order.dto.response;

import com.quickcart.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(Long id, String orderNumber, OrderStatus status, BigDecimal total,
                                   int itemCount, Instant createdAt) {
}