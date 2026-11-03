package com.quickcart.order.service;

import com.quickcart.order.client.CatalogClient;
import com.quickcart.order.client.CatalogProduct;
import com.quickcart.order.config.OrderProperties;
import com.quickcart.order.dto.request.AddCartItemRequest;
import com.quickcart.order.dto.request.SetCartItemQuantityRequest;
import com.quickcart.order.dto.response.CartLineResponse;
import com.quickcart.order.dto.response.CartResponse;
import com.quickcart.order.dto.response.ErrorDetail;
import com.quickcart.order.entity.Cart;
import com.quickcart.order.entity.CartItem;
import com.quickcart.order.exception.ApiException;
import com.quickcart.order.repository.CartItemRepository;
import com.quickcart.order.repository.CartRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CartService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CatalogClient catalogClient;
    private final OrderProperties props;
    private final TransactionTemplate tx;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       CatalogClient catalogClient, OrderProperties props, TransactionTemplate tx) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.catalogClient = catalogClient;
        this.props = props;
        this.tx = tx;
    }

    // ---------------------------------------------------------------- O1

    public CartResponse getCart(long userId) {
        List<CartItem> items = cartItemRepository.findByCartUserIdOrderByIdAsc(userId);
        Map<Long, CatalogProduct> products = items.isEmpty() ? Map.of()
                : catalogClient.getProducts(items.stream().map(CartItem::getProductId).toList());
        return build(items, products);
    }

    // ---------------------------------------------------------------- O2

    public CartResponse addItem(long userId, AddCartItemRequest req) {
        int max = props.maxCartLineQuantity();
        long productId = req.productId();
        if (req.quantity() > max) {
            throw tooMany(max);
        }
        CatalogProduct product = catalogClient.findProduct(productId)
                .filter(CatalogProduct::active)
                .orElseThrow(this::productNotFound);

        tx.executeWithoutResult(status -> {
            Cart cart = lockOrCreateCart(userId);
            CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElse(null);
            int resulting = (item == null ? 0 : item.getQuantity()) + req.quantity();
            if (resulting > max) {
                throw tooMany(max);
            }
            if (resulting > product.availableQuantity()) {
                throw insufficient(productId, resulting, product.availableQuantity());
            }
            if (item == null) {
                cartItemRepository.save(new CartItem(cart, productId, resulting));
            } else {
                item.setQuantity(resulting);
            }
            cart.touch();
        });
        return getCart(userId);
    }

    // ---------------------------------------------------------------- O3

    public CartResponse setQuantity(long userId, long productId, SetCartItemQuantityRequest req) {
        int quantity = req.quantity();
        if (quantity > props.maxCartLineQuantity()) {
            throw tooMany(props.maxCartLineQuantity());
        }
        if (!cartItemRepository.existsByCartUserIdAndProductId(userId, productId)) {
            throw itemNotFound();
        }
        CatalogProduct product = catalogClient.findProduct(productId)
                .filter(CatalogProduct::active)
                .orElseThrow(this::productNotFound);
        if (quantity > product.availableQuantity()) {
            throw insufficient(productId, quantity, product.availableQuantity());
        }

        tx.executeWithoutResult(status -> {
            Cart cart = cartRepository.findByUserIdForUpdate(userId).orElseThrow(this::itemNotFound);
            CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                    .orElseThrow(this::itemNotFound);
            item.setQuantity(quantity);
            cart.touch();
        });
        return getCart(userId);
    }

    // ---------------------------------------------------------------- O4

    public CartResponse removeItem(long userId, long productId) {
        tx.executeWithoutResult(status -> {
            Cart cart = cartRepository.findByUserIdForUpdate(userId).orElseThrow(this::itemNotFound);
            CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                    .orElseThrow(this::itemNotFound);
            cartItemRepository.delete(item);
            cart.touch();
        });
        return getCart(userId);
    }

    // ---------------------------------------------------------------- O5

    public void clear(long userId) {
        tx.executeWithoutResult(status ->
                cartRepository.findByUserIdForUpdate(userId).ifPresent(cart -> {
                    cartItemRepository.deleteAllByCartId(cart.getId());
                    cart.touch();
                }));
    }

    // ---------------------------------------------------------------- calculation (Section 8.6 "Final rule")

    private CartResponse build(List<CartItem> items, Map<Long, CatalogProduct> products) {
        List<CartLineResponse> lines = new ArrayList<>();
        BigDecimal subtotal = ZERO;
        int itemCount = 0;
        boolean anyUnavailable = false;

        for (CartItem item : items) {
            CatalogProduct p = products.get(item.getProductId());
            int qty = item.getQuantity();

            if (p == null) {                                   // product no longer exists
                lines.add(new CartLineResponse(item.getProductId(), "Unavailable item", null, null,
                        ZERO, qty, ZERO, 0, false));
                anyUnavailable = true;
                continue;
            }
            boolean available = p.active() && p.availableQuantity() >= qty;
            if (available) {
                BigDecimal unitPrice = p.price().setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(lineTotal);
                itemCount += qty;
                lines.add(new CartLineResponse(p.id(), p.name(), p.unit(), p.imageUrl(),
                        unitPrice, qty, lineTotal, p.availableQuantity(), true));
            } else {                                           // excluded from every total
                anyUnavailable = true;
                lines.add(new CartLineResponse(p.id(), p.name(), p.unit(), p.imageUrl(),
                        ZERO, qty, ZERO, p.availableQuantity(), false));
            }
        }

        BigDecimal deliveryFee;
        if (itemCount == 0 || subtotal.compareTo(props.freeDeliveryThreshold()) >= 0) {
            deliveryFee = ZERO;
        } else {
            deliveryFee = props.deliveryFee().setScale(2, RoundingMode.HALF_UP);
        }
        return new CartResponse(lines, itemCount, subtotal, deliveryFee,
                subtotal.add(deliveryFee), anyUnavailable);
    }

    // ---------------------------------------------------------------- helpers

    private Cart lockOrCreateCart(long userId) {
        cartRepository.insertIfAbsent(userId);                 // no-op when the cart exists
        return cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Cart row missing after insert"));
    }

    private ApiException productNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found.");
    }

    private ApiException itemNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "Item is not in your cart.");
    }

    private ApiException insufficient(long productId, int requested, int available) {
        return new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                "Some items are not available in the requested quantity.",
                List.of(new ErrorDetail("productId:" + productId,
                        "Requested " + requested + ", available " + available)));
    }

    private ApiException tooMany(int max) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed.",
                List.of(new ErrorDetail("quantity", "Cannot exceed " + max + " per item")));
    }
    // Catalog calls never happen inside a DB transaction, so no connection or row lock is held during a network call. Writes use TransactionTemplate, which also avoids self-invocation problems.
}