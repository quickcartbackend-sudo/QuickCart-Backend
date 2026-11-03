package com.quickcart.order.dto.response;

import com.quickcart.order.entity.OrderStatus;
import com.quickcart.order.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record AdminOrderSummaryResponse(Long id, String orderNumber, Long userId, String recipientName,
                                        OrderStatus status, PaymentStatus paymentStatus, BigDecimal total,
                                        int itemCount, Instant createdAt) {
}