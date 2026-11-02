package com.quickcart.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(min = 2, max = 150) String name,
        @Size(max = 1000) String description,
        @NotBlank @Size(min = 1, max = 30) String unit,
        @Size(max = 500) String imageUrl) {
}