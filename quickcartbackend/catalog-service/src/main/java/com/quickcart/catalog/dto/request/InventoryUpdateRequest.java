package com.quickcart.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryUpdateRequest(@NotNull @Min(0) @Max(100000) Integer quantity) {
}