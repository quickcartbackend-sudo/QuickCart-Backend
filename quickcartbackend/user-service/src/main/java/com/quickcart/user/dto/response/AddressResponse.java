package com.quickcart.user.dto.response;

public record AddressResponse(Long id, String label, String recipientName, String phone, String line1,
                              String line2, String city, String state, String pincode, boolean isDefault) {
}