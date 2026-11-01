package com.quickcart.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotBlank @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Invalid phone number") String phone) {
}