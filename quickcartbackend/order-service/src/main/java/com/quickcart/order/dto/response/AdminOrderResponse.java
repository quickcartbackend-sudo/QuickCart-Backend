package com.quickcart.order.dto.response;

import com.quickcart.order.entity.OrderStatus;
import com.quickcart.order.entity.PaymentMethod;
import com.quickcart.order.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminOrderResponse(Long id, Long userId, String orderNumber, OrderStatus status,
                                 PaymentMethod paymentMethod, PaymentStatus paymentStatus,
                                 BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal total,
                                 DeliveryAddressResponse deliveryAddress, List<OrderItemResponse> items,
                                 Instant createdAt, Instant updatedAt) {
}