package com.quickcart.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InternalProductBatchRequest(@NotNull @Size(max = 50) List<@NotNull Long> productIds) {
}