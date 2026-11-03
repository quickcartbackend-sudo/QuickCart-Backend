package com.quickcart.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(min = 1, max = 30) String label,
        @NotBlank @Size(min = 2, max = 100) String recipientName,
        @NotBlank @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Invalid phone number") String phone,
        @NotBlank @Size(min = 3, max = 150) String line1,
        @Size(max = 150) String line2,
        @NotBlank @Size(min = 2, max = 60) String city,
        @NotBlank @Size(min = 2, max = 60) String state,
        @NotBlank @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid pincode") String pincode,
        Boolean isDefault) {
}