package com.quickcart.catalog.repository;

import com.quickcart.catalog.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() { }

    /** Customer view: product.active AND category.active (Section 8.4 C2). */
    public static Specification<Product> visibleToCustomers() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.isTrue(root.get("category").get("active")));
    }

    /** pattern is already lower-case, wrapped in % and escaped with '!' (see QueryUtils). */
    public static Specification<Product> nameLike(String pattern) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '!');
    }

    public static Specification<Product> inCategory(Long categoryId) {
        return categoryId == null ? null
                : (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasActive(Boolean active) {
        return active == null ? null : (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}