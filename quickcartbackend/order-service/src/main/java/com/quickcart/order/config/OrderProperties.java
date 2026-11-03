package com.quickcart.order.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quickcart.order")
public record OrderProperties(BigDecimal deliveryFee, BigDecimal freeDeliveryThreshold, int maxCartLineQuantity) {
}