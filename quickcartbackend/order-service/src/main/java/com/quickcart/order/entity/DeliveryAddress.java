package com.quickcart.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAddress {

    @Column(name = "delivery_recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "delivery_phone", nullable = false, length = 10)
    private String phone;

    @Column(name = "delivery_line1", nullable = false, length = 150)
    private String line1;

    @Column(name = "delivery_line2", length = 150)
    private String line2;

    @Column(name = "delivery_city", nullable = false, length = 60)
    private String city;

    @Column(name = "delivery_state", nullable = false, length = 60)
    private String state;

    @Column(name = "delivery_pincode", nullable = false, length = 6)
    private String pincode;
}