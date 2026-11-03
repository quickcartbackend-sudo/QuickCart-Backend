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
import com.quickcart.catalog.dto.request.InternalStockRequest;
import com.quickcart.catalog.dto.response.ErrorDetail;
import com.quickcart.catalog.dto.response.InternalReserveResponse.ReservedItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
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
        /**
     * I2. One transaction. Ids are processed in ascending order (deadlock prevention). Duplicate ids are merged.
     * Any problem throws, which rolls back every decrement made so far.
     */
    @Transactional
    public List<ReservedItem> reserve(List<InternalStockRequest.Item> items) {
        Map<Long, Integer> wanted = merge(items);
        Map<Long, Product> products = productRepository.findWithCategoryByIdIn(wanted.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<ErrorDetail> unavailable = new ArrayList<>();
        wanted.keySet().forEach(id -> {
            Product p = products.get(id);
            if (p == null || !p.isActive() || !p.getCategory().isActive()) {
                unavailable.add(new ErrorDetail("productId:" + id, "Product is not available"));
            }
        });
        if (!unavailable.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE",
                    "Some products are no longer available.", unavailable);
        }

        Instant now = Instant.now();
        List<ErrorDetail> shortages = new ArrayList<>();
        List<ReservedItem> reserved = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            long id = e.getKey();
            int qty = e.getValue();
            if (inventoryRepository.decrementIfAvailable(id, qty, now) == 1) {
                Product p = products.get(id);
                reserved.add(new ReservedItem(id, p.getName(), p.getUnit(), p.getPrice(), qty));
            } else {
                int available = inventoryRepository.findById(id).map(Inventory::getQuantity).orElse(0);
                shortages.add(new ErrorDetail("productId:" + id, "Requested " + qty + ", available " + available));
            }
        }
        if (!shortages.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "Some items are not available in the requested quantity.", shortages);
        }
        return reserved;
    }

    /** I3. Adds stock back. Unknown product ids are ignored and logged. */
    @Transactional
    public void release(List<InternalStockRequest.Item> items) {
        Instant now = Instant.now();
        merge(items).forEach((id, qty) -> {
            if (inventoryRepository.increment(id, qty, now) == 0) {
                log.warn("Release ignored unknown product id {}", id);
            }
        });
    }

    private Map<Long, Integer> merge(List<InternalStockRequest.Item> items) {
        Map<Long, Integer> merged = new TreeMap<>();
        items.forEach(i -> merged.merge(i.productId(), i.quantity(), Integer::sum));
        return merged;
    }
}