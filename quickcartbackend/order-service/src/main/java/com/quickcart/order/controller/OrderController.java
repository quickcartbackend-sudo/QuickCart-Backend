package com.quickcart.order.controller;

import com.quickcart.order.dto.request.PlaceOrderRequest;
import com.quickcart.order.dto.response.OrderResponse;
import com.quickcart.order.dto.response.OrderSummaryResponse;
import com.quickcart.order.dto.response.PageResponse;
import com.quickcart.order.security.AuthenticatedUser;
import com.quickcart.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @Operation(summary = "O6 Place order from cart")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody PlaceOrderRequest request) {
        return service.placeOrder(user.id(), request);
    }

    @Operation(summary = "O7 My orders (newest first)")
    @GetMapping
    public PageResponse<OrderSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return service.listMine(user.id(), page, size);
    }

    @Operation(summary = "O8 My order detail")
    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        return service.getMine(user.id(), id);
    }
}