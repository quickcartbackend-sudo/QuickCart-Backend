package com.quickcart.order.service;

import com.quickcart.order.client.CatalogClient;
import com.quickcart.order.client.CatalogClient.ReservedItem;
import com.quickcart.order.client.CatalogClient.StockItem;
import com.quickcart.order.config.OrderProperties;
import com.quickcart.order.dto.request.PlaceOrderRequest;
import com.quickcart.order.dto.response.AdminOrderResponse;
import com.quickcart.order.dto.response.AdminOrderSummaryResponse;
import com.quickcart.order.dto.response.CartResponse;
import com.quickcart.order.dto.response.ErrorDetail;
import com.quickcart.order.dto.response.OrderResponse;
import com.quickcart.order.dto.response.OrderSummaryResponse;
import com.quickcart.order.dto.response.PageResponse;
import com.quickcart.order.entity.Cart;
import com.quickcart.order.entity.CartItem;
import com.quickcart.order.entity.DeliveryAddress;
import com.quickcart.order.entity.Order;
import com.quickcart.order.entity.OrderItem;
import com.quickcart.order.entity.OrderStatus;
import com.quickcart.order.entity.PaymentMethod;
import com.quickcart.order.entity.PaymentStatus;
import com.quickcart.order.exception.ApiException;
import com.quickcart.order.mapper.OrderMapper;
import com.quickcart.order.repository.CartItemRepository;
import com.quickcart.order.repository.CartRepository;
import com.quickcart.order.repository.OrderItemCount;
import com.quickcart.order.repository.OrderItemRepository;
import com.quickcart.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final CatalogClient catalogClient;
    private final OrderProperties props;
    private final OrderMapper mapper;
    private final TransactionTemplate tx;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        CartRepository cartRepository, CartItemRepository cartItemRepository,
                        CartService cartService, CatalogClient catalogClient, OrderProperties props,
                        OrderMapper mapper, TransactionTemplate tx) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
        this.catalogClient = catalogClient;
        this.props = props;
        this.mapper = mapper;
        this.tx = tx;
    }

    // ------------------------------------------------------------------ O6 place order (Section 5.2 / 20.4)

    /** Deliberately not @Transactional: the catalog calls must not run inside a DB transaction. */
    public OrderResponse placeOrder(long userId, PlaceOrderRequest req) {
        CartResponse cart = cartService.getCart(userId);                     // 1. load cart (+ availability)
        if (cart.items().isEmpty()) {
            throw cartEmpty();
        }
        if (cart.hasUnavailableItems()) {
            throw unavailableItems(cart);
        }
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        cart.items().forEach(l -> wanted.merge(l.productId(), l.quantity(), Integer::sum));
        List<StockItem> stock = wanted.entrySet().stream()
                .map(e -> new StockItem(e.getKey(), e.getValue())).toList();

        List<ReservedItem> reserved = catalogClient.reserve(stock);          // 2. reserve (409 / 503 pass through)
        try {
            return tx.execute(status -> saveOrder(userId, req, wanted, reserved));   // 3. one local transaction
        } catch (RuntimeException e) {
            releaseQuietly(stock);                                           // 4. compensation, once, best effort
            throw e;                                                         // 500 INTERNAL_ERROR via the handler
        }
    }

    private OrderResponse saveOrder(long userId, PlaceOrderRequest req, Map<Long, Integer> wanted,
                                    List<ReservedItem> reserved) {
        Cart cart = cartRepository.findByUserIdForUpdate(userId).orElseThrow(this::cartEmpty);
        List<CartItem> current = cartItemRepository.findByCartUserIdOrderByIdAsc(userId);
        if (current.isEmpty()) {
            throw cartEmpty();                              // e.g. a double click already placed this order
        }
        Map<Long, Integer> now = current.stream()
                .collect(Collectors.toMap(CartItem::getProductId, CartItem::getQuantity));
        if (!now.equals(wanted)) {
            throw cartChanged();
        }

        Map<Long, ReservedItem> byId = reserved.stream()
                .collect(Collectors.toMap(ReservedItem::productId, Function.identity()));
        List<OrderLine> lines = new ArrayList<>();
        BigDecimal subtotal = ZERO;
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            ReservedItem r = byId.get(e.getKey());
            if (r == null) {
                throw new IllegalStateException("Reserve response is missing product " + e.getKey());
            }
            BigDecimal unitPrice = r.unitPrice().setScale(2, RoundingMode.HALF_UP);     // price from I2, not the client
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(e.getValue())).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);
            lines.add(new OrderLine(r.productId(), r.name(), unitPrice, e.getValue(), lineTotal));
        }
        BigDecimal deliveryFee = subtotal.compareTo(props.freeDeliveryThreshold()) >= 0
                ? ZERO : props.deliveryFee().setScale(2, RoundingMode.HALF_UP);

        var a = req.deliveryAddress();
        Order order = Order.builder()
                .orderNumber(nextOrderNumber())
                .userId(userId)
                .status(OrderStatus.PLACED)
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .total(subtotal.add(deliveryFee))
                .paymentMethod(req.paymentMethod())
                .paymentStatus(req.paymentMethod() == PaymentMethod.MOCK_PAYMENT
                        ? PaymentStatus.PAID : PaymentStatus.PENDING)
                .deliveryAddress(new DeliveryAddress(a.recipientName().trim(), a.phone(), a.line1().trim(),
                        blankToNull(a.line2()), a.city().trim(), a.state().trim(), a.pincode()))
                .build();
        lines.forEach(l -> order.addItem(new OrderItem(order, l.productId(), l.name(), l.unitPrice(),
                l.quantity(), l.lineTotal())));
        orderRepository.saveAndFlush(order);

        cartItemRepository.deleteAllByCartId(cart.getId());                 // cleared in the same transaction
        cart.touch();
        return mapper.toOrder(order);
    }

    private String nextOrderNumber() {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("QC%s-%06d", date, orderRepository.nextOrderSequence());
    }

    private void releaseQuietly(List<StockItem> stock) {
        try {
            catalogClient.release(stock);
        } catch (RuntimeException e) {
            log.error("Compensation release failed; stock may be under-counted for {}", stock, e);
        }
    }

    // ------------------------------------------------------------------ O7, O8

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listMine(long userId, int page, int size) {
        Page<Order> result = orderRepository.findByUserId(userId, QueryUtils.pageable(page, size, NEWEST_FIRST));
        Map<Long, Long> counts = counts(result);
        return PageResponse.of(result.map(o -> mapper.toSummary(o, counts.getOrDefault(o.getId(), 0L).intValue())));
    }

    @Transactional(readOnly = true)
    public OrderResponse getMine(long userId, long id) {
        return orderRepository.findWithItemsByIdAndUserId(id, userId)
                .map(mapper::toOrder)
                .orElseThrow(this::notFound);                     // another user's order also gives 404
    }

    // ------------------------------------------------------------------ R1, R2, R3

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryResponse> listAll(OrderStatus status, int page, int size) {
        Pageable pageable = QueryUtils.pageable(page, size, NEWEST_FIRST);
        Page<Order> result = status == null ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        Map<Long, Long> counts = counts(result);
        return PageResponse.of(result.map(o ->
                mapper.toAdminSummary(o, counts.getOrDefault(o.getId(), 0L).intValue())));
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse getAny(long id) {
        return orderRepository.findWithItemsById(id).map(mapper::toAdminOrder).orElseThrow(this::notFound);
    }

    /** Section 5.4: on CANCELLED the stock is released inside the same transaction; failure rolls back (503). */
    public OrderResponse updateStatus(long id, OrderStatus next) {
        return tx.execute(status -> {
            Order order = orderRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
            OrderStatus current = order.getStatus();
            if (!current.canMoveTo(next)) {
                throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                        "Cannot change order status from " + current + " to " + next + ".",
                        List.of(new ErrorDetail("status", "Allowed: " + current.allowedNext())));
            }
            order.changeStatus(next);
            if (next == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD) {
                order.markPaid();
            }
            orderRepository.saveAndFlush(order);
            if (next == OrderStatus.CANCELLED) {
                List<StockItem> items = order.getItems().stream()
                        .map(i -> new StockItem(i.getProductId(), i.getQuantity())).toList();
                catalogClient.release(items);                      // throws 503 -> whole transaction rolls back
            }
            return mapper.toOrder(order);
        });
    }

    // ------------------------------------------------------------------ helpers

    private Map<Long, Long> counts(Page<Order> page) {
        List<Long> ids = page.getContent().stream().map(Order::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return orderItemRepository.countByOrderIds(ids).stream()
                .collect(Collectors.toMap(OrderItemCount::orderId, OrderItemCount::quantity));
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ApiException cartEmpty() {
        return new ApiException(HttpStatus.CONFLICT, "CART_EMPTY", "Your cart is empty.");
    }

    private ApiException cartChanged() {
        return new ApiException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE",
                "Your cart changed while placing the order. Please review it and try again.");
    }

    private ApiException unavailableItems(CartResponse cart) {
        List<ErrorDetail> details = cart.items().stream()
                .filter(l -> !l.available())
                .map(l -> new ErrorDetail("productId:" + l.productId(),
                        "No longer available in the requested quantity"))
                .toList();
        return new ApiException(HttpStatus.CONFLICT, "PRODUCT_UNAVAILABLE",
                "Some items in your cart are unavailable. Remove them to continue.", details);
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Order not found.");
    }

    private record OrderLine(Long productId, String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) { }
}