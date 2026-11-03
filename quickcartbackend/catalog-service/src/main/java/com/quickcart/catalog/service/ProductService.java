package com.quickcart.catalog.service;

import com.quickcart.catalog.dto.request.PriceRequest;
import com.quickcart.catalog.dto.request.ProductCreateRequest;
import com.quickcart.catalog.dto.request.ProductUpdateRequest;
import com.quickcart.catalog.dto.response.AdminProductResponse;
import com.quickcart.catalog.dto.response.InternalProductSnapshot;
import com.quickcart.catalog.dto.response.PageResponse;
import com.quickcart.catalog.dto.response.ProductResponse;
import com.quickcart.catalog.entity.Category;
import com.quickcart.catalog.entity.Product;
import com.quickcart.catalog.exception.ApiException;
import com.quickcart.catalog.inventory.InventoryService;
import com.quickcart.catalog.mapper.CatalogMapper;
import com.quickcart.catalog.repository.CategoryRepository;
import com.quickcart.catalog.repository.ProductRepository;
import com.quickcart.catalog.repository.ProductSpecifications;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private static final Sort.Order BY_NAME = Sort.Order.asc("name").ignoreCase();
    private static final Sort.Order BY_ID = Sort.Order.asc("id");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryService inventoryService;
    private final CatalogMapper mapper;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          InventoryService inventoryService, CatalogMapper mapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryService = inventoryService;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ customer (C2, C3)

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listPublic(String q, Long categoryId, String sort, int page, int size) {
        Specification<Product> spec = Specification.where(ProductSpecifications.visibleToCustomers())
                .and(QueryUtils.hasText(q) ? ProductSpecifications.nameLike(QueryUtils.likePattern(q)) : null)
                .and(ProductSpecifications.inCategory(categoryId));
        Page<Product> result = productRepository.findAll(spec,
                QueryUtils.pageable(page, size, parseSort(sort)));
        Map<Long, Integer> stock = inventoryService.quantities(result.map(Product::getId).getContent());
        return PageResponse.of(result.map(p -> mapper.toProduct(p, stock.getOrDefault(p.getId(), 0))));
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublic(long id) {
        Product p = productRepository.findWithCategoryById(id)
                .filter(x -> x.isActive() && x.getCategory().isActive())
                .orElseThrow(this::productNotFound);
        return mapper.toProduct(p, inventoryService.quantities(List.of(id)).getOrDefault(id, 0));
    }

    private Sort parseSort(String sort) {
        String s = sort == null || sort.isBlank() ? "name" : sort;
        return switch (s) {
            case "name" -> Sort.by(BY_NAME, BY_ID);
            case "price_asc" -> Sort.by(Sort.Order.asc("price"), BY_ID);
            case "price_desc" -> Sort.by(Sort.Order.desc("price"), BY_ID);
            default -> throw QueryUtils.invalid("sort", "Must be one of: name, price_asc, price_desc");
        };
    }

    // ------------------------------------------------------------------ admin (A5-A10)

    @Transactional(readOnly = true)
    public PageResponse<AdminProductResponse> listAdmin(String q, Long categoryId, Boolean active,
                                                        int page, int size) {
        Specification<Product> spec = Specification
                .where(QueryUtils.hasText(q) ? ProductSpecifications.nameLike(QueryUtils.likePattern(q)) : null)
                .and(ProductSpecifications.inCategory(categoryId))
                .and(ProductSpecifications.hasActive(active));
        Page<Product> result = productRepository.findAll(spec,
                QueryUtils.pageable(page, size, Sort.by(BY_NAME, BY_ID)));
        Map<Long, Integer> stock = inventoryService.quantities(result.map(Product::getId).getContent());
        return PageResponse.of(result.map(p -> mapper.toAdminProduct(p, stock.getOrDefault(p.getId(), 0))));
    }

    @Transactional(readOnly = true)
    public AdminProductResponse getAdmin(long id) {
        return toAdmin(find(id));
    }

    @Transactional
    public AdminProductResponse create(ProductCreateRequest req) {
        Category category = findCategory(req.categoryId());
        Product product = productRepository.save(new Product(category, req.name().trim(),
                QueryUtils.blankToNull(req.description()), req.price(), req.unit().trim(),
                QueryUtils.blankToNull(req.imageUrl())));
        int quantity = req.initialQuantity() == null ? 0 : req.initialQuantity();
        inventoryService.create(product.getId(), quantity);        // same transaction as the product
        return mapper.toAdminProduct(product, quantity);
    }

        @Transactional
    public AdminProductResponse update(long id, ProductUpdateRequest req) {
        Product product = find(id);
        product.updateDetails(findCategory(req.categoryId()), req.name().trim(),
                QueryUtils.blankToNull(req.description()), req.unit().trim(),
                QueryUtils.blankToNull(req.imageUrl()));
        return toAdmin(productRepository.saveAndFlush(product));
    }

    @Transactional
    public AdminProductResponse updatePrice(long id, PriceRequest req) {
        Product product = find(id);
        product.changePrice(req.price());
        return toAdmin(productRepository.saveAndFlush(product));
    }

    @Transactional
    public AdminProductResponse setActive(long id, boolean active) {
        Product product = find(id);
        product.setActive(active);
        return toAdmin(productRepository.saveAndFlush(product));
    }

    // ------------------------------------------------------------------ internal (I1)

    @Transactional(readOnly = true)
    public List<InternalProductSnapshot> snapshots(List<Long> ids) {
        List<Long> distinct = ids.stream().distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> stock = inventoryService.quantities(distinct);
        return productRepository.findWithCategoryByIdIn(distinct).stream()      // unknown ids are omitted
                .map(p -> new InternalProductSnapshot(p.getId(), p.getName(), p.getUnit(), p.getImageUrl(),
                        p.getPrice(), p.isActive() && p.getCategory().isActive(),
                        stock.getOrDefault(p.getId(), 0)))
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    private AdminProductResponse toAdmin(Product p) {
        return mapper.toAdminProduct(p, inventoryService.quantities(List.of(p.getId())).getOrDefault(p.getId(), 0));
    }

    private Product find(long id) {
        return productRepository.findWithCategoryById(id).orElseThrow(this::productNotFound);
    }

    private Category findCategory(long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Category not found."));
    }

    private ApiException productNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Product not found.");
    }
}