package com.quickcart.catalog.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PriceRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("99999.99")
        @Digits(integer = 5, fraction = 2) BigDecimal price) {
}