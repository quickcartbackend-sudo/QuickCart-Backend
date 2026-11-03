package com.quickcart.order.dto.request;

import com.quickcart.order.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotNull @Valid DeliveryAddressRequest deliveryAddress) {
}