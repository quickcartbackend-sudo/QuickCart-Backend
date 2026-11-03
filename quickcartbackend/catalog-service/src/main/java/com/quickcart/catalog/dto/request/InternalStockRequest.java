package com.quickcart.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InternalStockRequest(@NotEmpty @Size(max = 50) List<@Valid @NotNull Item> items) {

    public record Item(@NotNull Long productId, @NotNull @Min(1) @Max(100000) Integer quantity) { }
}