package com.quickcart.catalog.dto.response;

import java.util.List;

public record InternalProductBatchResponse(List<InternalProductSnapshot> products) {
}