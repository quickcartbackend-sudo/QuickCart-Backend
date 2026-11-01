package com.quickcart.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"eureka.client.enabled=false"})
@AutoConfigureWebTestClient
class GatewaySkeletonTest {

    @Autowired
    WebTestClient client;

    @Test
    void internalPath_isNotRouted_returns404StandardBody() {
        client.post().uri("/internal/inventory/reserve")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND")
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.path").isEqualTo("/internal/inventory/reserve")
                .jsonPath("$.details").isArray();
    }

    @Test
    void unknownPath_returns404StandardBody() {
        client.get().uri("/api/v1/does-not-exist")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND");
    }

    @Test
    void routedPathWithNoInstances_returns503StandardBody() {
        client.get().uri("/api/v1/products")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.message").isNotEmpty();
    }

    @Test
    void requestId_isGeneratedWhenAbsent() {
        client.get().uri("/api/v1/does-not-exist")
                .exchange()
                .expectHeader().exists("X-Request-Id");
    }

    @Test
    void requestId_isEchoedWhenProvided() {
        client.get().uri("/api/v1/does-not-exist")
                .header("X-Request-Id", "abc-123")
                .exchange()
                .expectHeader().valueEquals("X-Request-Id", "abc-123");
    }

    @Test
    void corsPreflight_allowsConfiguredOrigin() {
        client.options().uri("/api/v1/products")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
    }

    @Test
    void corsPreflight_rejectsUnknownOrigin() {
        client.options().uri("/api/v1/products")
                .header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isForbidden();
    }
}