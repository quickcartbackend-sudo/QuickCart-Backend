package com.quickcart.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quickcart.catalog-client")
public record CatalogClientProperties(int connectTimeoutMs, int readTimeoutMs) {
}