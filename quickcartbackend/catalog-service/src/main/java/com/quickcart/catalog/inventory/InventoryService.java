package com.quickcart.catalog.inventory;

import com.quickcart.catalog.dto.response.InventoryItemResponse;
import com.quickcart.catalog.dto.response.PageResponse;
import com.quickcart.catalog.entity.Product;
import com.quickcart.catalog.exception.ApiException;
import com.quickcart.catalog.repository.ProductRepository;
import com.quickcart.catalog.service.QueryUtils;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    /** One query for many products; missing rows count as 0. */
    @Transactional(readOnly = true)
    public Map<Long, Integer> quantities(Collection<Long> productIds) {
        Map<Long, Integer> result = new HashMap<>();
        inventoryRepository.findAllById(productIds)
                .forEach(i -> result.put(i.getProductId(), i.getQuantity()));
        return result;
    }

    /** Called by ProductService inside its own transaction (row is created with the product). */
    @Transactional
    public void create(long productId, int quantity) {
        inventoryRepository.save(new Inventory(productId, quantity));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> list(String q, int page, int size) {          // A11
        Pageable pageable = QueryUtils.pageable(page, size, Sort.unsorted());
        return PageResponse.of(inventoryRepository.search(QueryUtils.likePattern(q), pageable));
    }

    @Transactional
    public InventoryItemResponse setQuantity(long productId, int quantity) {                 // A12
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Product not found."));
        Inventory inventory = inventoryRepository.findById(productId).orElse(null);
        if (inventory == null) {
            inventory = inventoryRepository.save(new Inventory(productId, quantity));
        } else {
            inventory.setQuantity(quantity);
        }
        return new InventoryItemResponse(productId, product.getName(), product.getUnit(),
                inventory.getQuantity(), inventory.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS));
    }
}