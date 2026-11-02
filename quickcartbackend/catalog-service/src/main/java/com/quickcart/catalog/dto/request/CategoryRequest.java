package com.quickcart.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(min = 2, max = 80) String name,
        @Size(max = 255) String description,
        @Size(max = 500) String imageUrl) {
}