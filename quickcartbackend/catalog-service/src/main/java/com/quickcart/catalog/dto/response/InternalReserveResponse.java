package com.quickcart.catalog.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record InternalReserveResponse(List<ReservedItem> items) {

    public record ReservedItem(Long productId, String name, String unit, BigDecimal unitPrice, int quantity) { }
}