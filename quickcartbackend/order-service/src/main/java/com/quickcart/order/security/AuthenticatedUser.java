package com.quickcart.order.security;

public record AuthenticatedUser(long id, String email, String role) {
}