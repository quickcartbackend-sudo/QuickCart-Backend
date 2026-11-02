package com.quickcart.catalog.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() { }

    public Inventory(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }

    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public Instant getUpdatedAt() { return updatedAt; }
}