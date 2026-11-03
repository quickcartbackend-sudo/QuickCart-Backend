package com.quickcart.order.controller;

import com.quickcart.order.dto.request.UpdateOrderStatusRequest;
import com.quickcart.order.dto.response.AdminOrderResponse;
import com.quickcart.order.dto.response.AdminOrderSummaryResponse;
import com.quickcart.order.dto.response.OrderResponse;
import com.quickcart.order.dto.response.PageResponse;
import com.quickcart.order.entity.OrderStatus;
import com.quickcart.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService service;

    public AdminOrderController(OrderService service) {
        this.service = service;
    }

    @Operation(summary = "R1 All orders")
    @GetMapping
    public PageResponse<AdminOrderSummaryResponse> list(@RequestParam(required = false) OrderStatus status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return service.listAll(status, page, size);
    }

    @Operation(summary = "R2 Any order detail")
    @GetMapping("/{id}")
    public AdminOrderResponse get(@PathVariable long id) {
        return service.getAny(id);
    }

    @Operation(summary = "R3 Move order status")
    @PatchMapping("/{id}/status")
    public OrderResponse status(@PathVariable long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}