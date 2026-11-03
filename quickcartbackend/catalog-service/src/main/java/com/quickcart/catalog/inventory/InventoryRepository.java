package com.quickcart.catalog.inventory;

import com.quickcart.catalog.dto.response.InventoryItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query(value = "select new com.quickcart.catalog.dto.response.InventoryItemResponse("
            + "p.id, p.name, p.unit, i.quantity, i.updatedAt) "
            + "from Inventory i, Product p "
            + "where p.id = i.productId and lower(p.name) like :pattern escape '!' "
            + "order by lower(p.name), p.id",
            countQuery = "select count(i) from Inventory i, Product p "
                    + "where p.id = i.productId and lower(p.name) like :pattern escape '!'")
    Page<InventoryItemResponse> search(@Param("pattern") String pattern, Pageable pageable);

        /** Atomic conditional decrement (Section 20.4). Returns 1 on success, 0 if stock is too low. */
    @Modifying
    @Query("update Inventory i set i.quantity = i.quantity - :qty, i.updatedAt = :now "
            + "where i.productId = :id and i.quantity >= :qty")
    int decrementIfAvailable(@Param("id") Long id, @Param("qty") int qty, @Param("now") Instant now);

    @Modifying
    @Query("update Inventory i set i.quantity = i.quantity + :qty, i.updatedAt = :now where i.productId = :id")
    int increment(@Param("id") Long id, @Param("qty") int qty, @Param("now") Instant now);
}