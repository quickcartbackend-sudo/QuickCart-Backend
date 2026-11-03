package com.quickcart.order.client;

import com.quickcart.order.dto.response.ErrorDetail;
import com.quickcart.order.exception.ApiException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
/** Section 5.5 rules. I1 is read-only, so it gets one retry. reserve/release (step 7) get none. */
@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);
    private static final int MAX_IDS_PER_CALL = 50;      // Section 8.5 I1 limit
    private static final int ATTEMPTS = 2;               // 1 call + 1 retry

    private final RestClient restClient;

    public CatalogClient(@Qualifier("catalogRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** Unknown product ids are simply absent from the result map. */
    public Map<Long, CatalogProduct> getProducts(Collection<Long> productIds) {
        List<Long> distinct = productIds.stream().distinct().toList();
        Map<Long, CatalogProduct> result = new HashMap<>();
        for (int from = 0; from < distinct.size(); from += MAX_IDS_PER_CALL) {
            List<Long> chunk = new ArrayList<>(
                    distinct.subList(from, Math.min(from + MAX_IDS_PER_CALL, distinct.size())));
            for (CatalogProduct p : fetchBatch(chunk)) {
                result.put(p.id(), p);
            }
        }
        return result;
    }

    public Optional<CatalogProduct> findProduct(long productId) {
        return Optional.ofNullable(getProducts(List.of(productId)).get(productId));
    }

    private List<CatalogProduct> fetchBatch(List<Long> ids) {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                BatchResponse response = restClient.post()
                        .uri("/internal/products/batch")
                        .body(new BatchRequest(ids))
                        .retrieve()
                        .body(BatchResponse.class);
                return response == null || response.products() == null ? List.of() : response.products();
            } catch (HttpClientErrorException e) {
                throw mapClientError(e);                       // 4xx: never retried
            } catch (ResourceAccessException | HttpServerErrorException | IllegalStateException e) {
                // timeout / connection refused / 5xx / "No instances available"
                log.warn("catalog-service I1 attempt {}/{} failed: {}", attempt, ATTEMPTS, e.getMessage());
            } catch (RestClientException e) {
                log.error("catalog-service I1 returned an unusable response", e);
                throw unavailable();
            }
        }
        throw unavailable();
    }

    private ApiException mapClientError(HttpClientErrorException e) {
        int status = e.getStatusCode().value();
        HttpStatus resolved = HttpStatus.resolve(status);
        ErrorBody body = null;
        try {
            body = e.getResponseBodyAs(ErrorBody.class);
        } catch (Exception ignored) {
            // fall through to 503
        }
        // 401/403 from an internal call is OUR misconfiguration (bad API key), not the customer's problem
        if (status == 401 || status == 403 || resolved == null || body == null || body.code() == null) {
            log.error("catalog-service rejected an internal call with HTTP {}", status);
            return unavailable();
        }
        List<ErrorDetail> details = body.details() == null ? List.of()
                : body.details().stream().map(d -> new ErrorDetail(d.field(), d.message())).toList();
        return new ApiException(resolved, body.code(),
                body.message() == null ? "The request could not be completed." : body.message(), details);
    }

    private ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                "A dependent service is temporarily unavailable. Please try again.");
    }
        /** I2. 409 INSUFFICIENT_STOCK / PRODUCT_UNAVAILABLE pass through. No retry (not idempotent). */
    public List<ReservedItem> reserve(List<StockItem> items) {
        try {
            ReserveResponse response = restClient.post()
                    .uri("/internal/inventory/reserve")
                    .body(new StockRequest(items))
                    .retrieve()
                    .body(ReserveResponse.class);
            if (response == null || response.items() == null) {
                log.error("catalog-service reserve returned an empty body");
                throw unavailable();
            }
            return response.items();
        } catch (HttpClientErrorException e) {
            throw mapClientError(e);
        } catch (ResourceAccessException | HttpServerErrorException | IllegalStateException e) {
            log.warn("catalog-service reserve failed (not retried): {}", e.getMessage());
            throw unavailable();
        } catch (RestClientException e) {
            log.error("catalog-service reserve returned an unusable response", e);
            throw unavailable();
        }
    }

    public void release(List<StockItem> items) {
        try {
            restClient.post()
                    .uri("/internal/inventory/release")
                    .body(new StockRequest(items))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw mapClientError(e);
        } catch (ResourceAccessException | HttpServerErrorException | IllegalStateException e) {
            log.warn("catalog-service release failed (not retried): {}", e.getMessage());
            throw unavailable();
        } catch (RestClientException e) {
            log.error("catalog-service release returned an unusable response", e);
            throw unavailable();
        }
    }

    public record BatchRequest(List<Long> productIds) { }

    public record BatchResponse(List<CatalogProduct> products) { }

    public record ErrorBody(String code, String message, List<ErrorBodyDetail> details) { }

    public record ErrorBodyDetail(String field, String message) { }

    public record StockItem(Long productId, int quantity) { }

    public record StockRequest(List<StockItem> items) { }

    public record ReservedItem(Long productId, String name, String unit, BigDecimal unitPrice, int quantity) { }

    public record ReserveResponse(List<ReservedItem> items) { }
}