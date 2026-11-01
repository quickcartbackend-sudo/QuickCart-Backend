package com.quickcart.user.dto.response;

import java.time.Instant;

public record UserProfile(Long id, String fullName, String email, String phone, String role, Instant createdAt) {
}