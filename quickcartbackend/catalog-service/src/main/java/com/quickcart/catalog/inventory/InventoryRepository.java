package com.quickcart.catalog.inventory;

import com.quickcart.catalog.dto.response.InventoryItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query(value = "select new com.quickcart.catalog.dto.response.InventoryItemResponse("
            + "p.id, p.name, p.unit, i.quantity, i.updatedAt) "
            + "from Inventory i, Product p "
            + "where p.id = i.productId and lower(p.name) like :pattern escape '!' "
            + "order by lower(p.name), p.id",
            countQuery = "select count(i) from Inventory i, Product p "
                    + "where p.id = i.productId and lower(p.name) like :pattern escape '!'")
    Page<InventoryItemResponse> search(@Param("pattern") String pattern, Pageable pageable);
}