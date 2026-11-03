package com.quickcart.order.mapper;

import com.quickcart.order.dto.response.AdminOrderResponse;
import com.quickcart.order.dto.response.AdminOrderSummaryResponse;
import com.quickcart.order.dto.response.DeliveryAddressResponse;
import com.quickcart.order.dto.response.OrderItemResponse;
import com.quickcart.order.dto.response.OrderResponse;
import com.quickcart.order.dto.response.OrderSummaryResponse;
import com.quickcart.order.entity.DeliveryAddress;
import com.quickcart.order.entity.Order;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toOrder(Order o) {
        return new OrderResponse(o.getId(), o.getOrderNumber(), o.getStatus(), o.getPaymentMethod(),
                o.getPaymentStatus(), o.getSubtotal(), o.getDeliveryFee(), o.getTotal(),
                address(o.getDeliveryAddress()), items(o), secs(o.getCreatedAt()), secs(o.getUpdatedAt()));
    }

    public AdminOrderResponse toAdminOrder(Order o) {
        return new AdminOrderResponse(o.getId(), o.getUserId(), o.getOrderNumber(), o.getStatus(),
                o.getPaymentMethod(), o.getPaymentStatus(), o.getSubtotal(), o.getDeliveryFee(), o.getTotal(),
                address(o.getDeliveryAddress()), items(o), secs(o.getCreatedAt()), secs(o.getUpdatedAt()));
    }

    public OrderSummaryResponse toSummary(Order o, int itemCount) {
        return new OrderSummaryResponse(o.getId(), o.getOrderNumber(), o.getStatus(), o.getTotal(),
                itemCount, secs(o.getCreatedAt()));
    }

    public AdminOrderSummaryResponse toAdminSummary(Order o, int itemCount) {
        return new AdminOrderSummaryResponse(o.getId(), o.getOrderNumber(), o.getUserId(),
                o.getDeliveryAddress().getRecipientName(), o.getStatus(), o.getPaymentStatus(),
                o.getTotal(), itemCount, secs(o.getCreatedAt()));
    }

    private List<OrderItemResponse> items(Order o) {
        return o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProductId(), i.getProductName(), i.getUnitPrice(),
                        i.getQuantity(), i.getLineTotal()))
                .toList();
    }

    private DeliveryAddressResponse address(DeliveryAddress a) {
        return new DeliveryAddressResponse(a.getRecipientName(), a.getPhone(), a.getLine1(), a.getLine2(),
                a.getCity(), a.getState(), a.getPincode());
    }

    private Instant secs(Instant t) {
        return t == null ? null : t.truncatedTo(ChronoUnit.SECONDS);
    }
}