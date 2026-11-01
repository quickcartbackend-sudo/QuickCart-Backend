package com.quickcart.user.dto.response;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {
}