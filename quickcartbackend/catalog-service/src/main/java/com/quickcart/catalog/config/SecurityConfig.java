package com.quickcart.catalog.config;

import com.quickcart.catalog.security.InternalApiKeyFilter;
import com.quickcart.catalog.security.JwtAuthenticationFilter;
import com.quickcart.catalog.security.JwtVerifier;
import com.quickcart.catalog.security.RestAccessDeniedHandler;
import com.quickcart.catalog.security.RestAuthenticationEntryPoint;
import com.quickcart.catalog.security.SecurityErrorWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtVerifier verifier,
                                            SecurityErrorWriter errorWriter,
                                            RestAuthenticationEntryPoint entryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler,
                                            @Value("${quickcart.internal.api-key}") String internalApiKey)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**", "/api/v1/products/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/internal/**").hasRole("INTERNAL")   // granted only by InternalApiKeyFilter
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(verifier), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new InternalApiKeyFilter(internalApiKey, errorWriter),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}