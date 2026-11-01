package com.quickcart.catalog.security;

public record AuthenticatedUser(long id, String email, String role) {
}