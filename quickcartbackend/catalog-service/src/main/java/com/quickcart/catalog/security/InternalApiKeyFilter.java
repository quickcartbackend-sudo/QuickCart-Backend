package com.quickcart.catalog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Not a @Component (it would be registered twice); SecurityConfig creates it. */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";

    private final byte[] expected;
    private final SecurityErrorWriter writer;

    public InternalApiKeyFilter(String apiKey, SecurityErrorWriter writer) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INTERNAL_API_KEY must be set");
        }
        this.expected = apiKey.getBytes(StandardCharsets.UTF_8);
        this.writer = writer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        // constant-time comparison
        if (provided != null && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expected)) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "internal-caller", null, List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))));
            chain.doFilter(request, response);
            return;
        }
        writer.write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required.");
    }
}