package com.quickcart.gateway.filter;

import com.quickcart.gateway.security.JwtVerifier;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Section 9.4 gateway rules: public routes, JWT everywhere else, ADMIN role for /api/v1/admin/**. */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String PREFIX = "Bearer ";

    private final JwtVerifier verifier;

    public JwtAuthGlobalFilter(JwtVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getURI().getRawPath();

        // reject path-confusion tricks before any prefix matching
        String lower = path.toLowerCase();
        if (path.contains("..") || path.contains("//") || lower.contains("%2e") || lower.contains("%2f")) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
        }

        if (HttpMethod.OPTIONS.equals(method) || isPublic(method, path)) {
            return chain.filter(exchange);
        }

        Optional<String> role = extractRole(exchange);
        if (role.isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        }
        if (under("/api/v1/admin", path) && !"ADMIN".equals(role.get())) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
        }
        // CUSTOMER-only checks (cart, orders) are enforced by order-service (Section 9.4)
        return chain.filter(exchange);
    }

    private boolean isPublic(HttpMethod method, String path) {
        if (HttpMethod.POST.equals(method) && under("/api/v1/auth", path)) {
            return true;
        }
        return HttpMethod.GET.equals(method)
                && (under("/api/v1/categories", path) || under("/api/v1/products", path));
    }

    private Optional<String> extractRole(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith(PREFIX)) {
            return Optional.empty();
        }
        return verifier.verifyAndGetRole(header.substring(PREFIX.length()).trim());
    }

    private boolean under(String base, String path) {
        return path.equals(base) || path.startsWith(base + "/");
    }

    @Override
    public int getOrder() {
        return -100;     // after RequestIdFilter (HIGHEST_PRECEDENCE), before routing filters
    }
    // It runs only for routed requests, so /internal/** and unknown paths still get the 404 from GatewayErrorHandler and never reach this filter. Failures are raised as ResponseStatusException
}