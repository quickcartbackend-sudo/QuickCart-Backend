package com.quickcart.order.dto.response;

public record DeliveryAddressResponse(String recipientName, String phone, String line1, String line2,
                                      String city, String state, String pincode) {
}