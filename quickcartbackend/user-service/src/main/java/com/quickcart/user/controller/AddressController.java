package com.quickcart.user.controller;

import com.quickcart.user.dto.request.AddressRequest;
import com.quickcart.user.dto.response.AddressResponse;
import com.quickcart.user.security.AuthenticatedUser;
import com.quickcart.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/users/me/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @Operation(summary = "U5 List my addresses (default first)")
    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(user.id());
    }

    @Operation(summary = "U6 Add address")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody AddressRequest request) {
        return service.create(user.id(), request);
    }

    @Operation(summary = "U7 Update address")
    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id,
                                  @Valid @RequestBody AddressRequest request) {
        return service.update(user.id(), id, request);
    }

    @Operation(summary = "U8 Delete address")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long id) {
        service.delete(user.id(), id);
    }
}