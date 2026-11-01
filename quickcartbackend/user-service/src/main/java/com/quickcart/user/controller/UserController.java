package com.quickcart.user.controller;

import com.quickcart.user.dto.request.UpdateProfileRequest;
import com.quickcart.user.dto.response.UserProfile;
import com.quickcart.user.security.AuthenticatedUser;
import com.quickcart.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "U3 Current user profile")
    @GetMapping
    public UserProfile me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.getProfile(principal.id());
    }

    @Operation(summary = "U4 Update name and phone")
    @PutMapping
    public UserProfile update(@AuthenticationPrincipal AuthenticatedUser principal,
                              @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.id(), request);
    }
}