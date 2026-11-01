package com.quickcart.user.service;

import com.quickcart.user.dto.request.LoginRequest;
import com.quickcart.user.dto.request.RegisterRequest;
import com.quickcart.user.dto.response.AuthResponse;
import com.quickcart.user.entity.Role;
import com.quickcart.user.entity.User;
import com.quickcart.user.exception.ApiException;
import com.quickcart.user.mapper.UserMapper;
import com.quickcart.user.repository.UserRepository;
import com.quickcart.user.security.JwtService;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (userRepository.existsByEmail(email)) {
            throw emailExists();
        }
        // role is always CUSTOMER; the request has no role field (Section 8.3)
        User user = new User(req.fullName().trim(), email,
                passwordEncoder.encode(req.password()), req.phone(), Role.CUSTOMER);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException race) {
            throw emailExists();     // two simultaneous registrations: the DB unique index decides
        }
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalize(req.email()))
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Invalid email or password."));   // same message for every failure cause
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(jwtService.issue(user), "Bearer",
                jwtService.getExpirySeconds(), userMapper.toSummary(user));
    }

    private ApiException emailExists() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                "An account with this email already exists.");
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}