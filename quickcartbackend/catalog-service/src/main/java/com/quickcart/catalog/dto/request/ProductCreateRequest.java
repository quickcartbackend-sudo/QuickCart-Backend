package com.quickcart.catalog.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(min = 2, max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("99999.99")
        @Digits(integer = 5, fraction = 2) BigDecimal price,
        @NotBlank @Size(min = 1, max = 30) String unit,
        @Size(max = 500) String imageUrl,
        @Min(0) @Max(100000) Integer initialQuantity) {
}