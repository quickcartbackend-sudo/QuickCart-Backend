package com.quickcart.user.security;

import com.quickcart.user.entity.Role;

public record AuthenticatedUser(long id, String email, Role role) {
}