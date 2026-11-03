package com.quickcart.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetCartItemQuantityRequest(@NotNull @Min(1) Integer quantity) {
}