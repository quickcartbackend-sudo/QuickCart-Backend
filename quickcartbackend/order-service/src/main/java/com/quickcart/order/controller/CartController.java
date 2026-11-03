package com.quickcart.order.controller;

import com.quickcart.order.dto.request.AddCartItemRequest;
import com.quickcart.order.dto.request.SetCartItemQuantityRequest;
import com.quickcart.order.dto.response.CartResponse;
import com.quickcart.order.security.AuthenticatedUser;
import com.quickcart.order.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @Operation(summary = "O1 View cart (enriched)")
    @GetMapping
    public CartResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.getCart(user.id());
    }

    @Operation(summary = "O2 Add item (adds to existing quantity)")
    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal AuthenticatedUser user,
                            @Valid @RequestBody AddCartItemRequest request) {
        return service.addItem(user.id(), request);
    }

    @Operation(summary = "O3 Set item quantity (absolute)")
    @PutMapping("/items/{productId}")
    public CartResponse set(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long productId,
                            @Valid @RequestBody SetCartItemQuantityRequest request) {
        return service.setQuantity(user.id(), productId, request);
    }

    @Operation(summary = "O4 Remove item")
    @DeleteMapping("/items/{productId}")
    public CartResponse remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long productId) {
        return service.removeItem(user.id(), productId);
    }

    @Operation(summary = "O5 Clear cart")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal AuthenticatedUser user) {
        service.clear(user.id());
    }
}