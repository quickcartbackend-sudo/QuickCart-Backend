package com.quickcart.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({OrderProperties.class, CatalogClientProperties.class})
public class CatalogClientConfig {

    /** The load-balancer interceptor resolves http://catalog-service through Eureka. */
    @Bean
    @LoadBalanced
    RestClient.Builder catalogRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient catalogRestClient(@LoadBalanced RestClient.Builder builder,
                                 CatalogClientProperties props,
                                 @Value("${quickcart.internal.api-key}") String internalApiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalStateException("INTERNAL_API_KEY must be set");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeoutMs());   // Section 5.5: 2 s
        factory.setReadTimeout(props.readTimeoutMs());         // Section 5.5: 5 s
        return builder
                .baseUrl("http://catalog-service")
                .requestFactory(factory)
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();
    }
}