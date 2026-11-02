package com.quickcart.catalog.mapper;

import com.quickcart.catalog.dto.response.AdminProductResponse;
import com.quickcart.catalog.dto.response.CategoryResponse;
import com.quickcart.catalog.dto.response.ProductResponse;
import com.quickcart.catalog.entity.Category;
import com.quickcart.catalog.entity.Product;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class CatalogMapper {

    public CategoryResponse toCategory(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getImageUrl(), c.isActive());
    }

    public ProductResponse toProduct(Product p, int quantity) {
        return new ProductResponse(p.getId(), p.getCategory().getId(), p.getCategory().getName(),
                p.getName(), p.getDescription(), p.getPrice(), p.getUnit(), p.getImageUrl(),
                quantity, quantity > 0);
    }

    public AdminProductResponse toAdminProduct(Product p, int quantity) {
        return new AdminProductResponse(p.getId(), p.getCategory().getId(), p.getCategory().getName(),
                p.getName(), p.getDescription(), p.getPrice(), p.getUnit(), p.getImageUrl(),
                quantity, quantity > 0, p.isActive(),
                p.getCreatedAt().truncatedTo(ChronoUnit.SECONDS),
                p.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS));
    }
}