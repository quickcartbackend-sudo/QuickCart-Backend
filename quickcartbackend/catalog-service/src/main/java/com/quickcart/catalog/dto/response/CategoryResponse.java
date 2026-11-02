package com.quickcart.catalog.dto.response;

public record CategoryResponse(Long id, String name, String description, String imageUrl, boolean active) {
}