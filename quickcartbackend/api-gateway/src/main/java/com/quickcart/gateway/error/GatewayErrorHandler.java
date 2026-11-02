package com.quickcart.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcart.gateway.dto.ErrorResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Replaces Spring's default error JSON. Order -2 runs before the default handler (-1).
 * 404 (no route), 503 (no instance / upstream down) and anything else become the Section 12.1 body.
 */
@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        String code;
        String message;
        switch (status) {
            case NOT_FOUND -> {
                code = "NOT_FOUND";
                message = "Resource not found.";
            }
            case BAD_REQUEST -> {
                code = "VALIDATION_ERROR";
                message = "Bad request.";
            }
            case UNAUTHORIZED -> {
                code = "UNAUTHORIZED";
                message = "Authentication is required.";
            }
            case FORBIDDEN -> {
                code = "FORBIDDEN";
                message = "You do not have permission to perform this action.";
            }
            case BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                code = "SERVICE_UNAVAILABLE";
                message = "A dependent service is temporarily unavailable. Please try again.";
                log.warn("Upstream problem on {}: {}", exchange.getRequest().getPath(), ex.getMessage());
            }
            default -> {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                code = "INTERNAL_ERROR";
                message = "An unexpected error occurred. Please try again later.";
                log.error("Unexpected gateway error on {}", exchange.getRequest().getPath(), ex);
            }
        }

        ErrorResponse body = new ErrorResponse(
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                exchange.getRequest().getPath().value(),
                List.of());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception writeFailure) {
            log.error("Could not serialise error body", writeFailure);
            return exchange.getResponse().setComplete();
        }
    }

        private HttpStatus resolveStatus(Throwable ex) {
        Throwable t = ex;
        for (int depth = 0; t != null && depth < 10; depth++, t = t.getCause()) {
            if (t instanceof ResponseStatusException rse) {
                HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
                if (resolved != null) {
                    return resolved;
                }
            }
            if (t instanceof java.net.ConnectException
                    || t instanceof java.util.concurrent.TimeoutException
                    || t instanceof reactor.netty.http.client.PrematureCloseException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}