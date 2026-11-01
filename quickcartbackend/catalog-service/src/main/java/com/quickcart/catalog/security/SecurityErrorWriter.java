package com.quickcart.catalog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcart.catalog.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest req, HttpServletResponse res,
                      HttpStatus status, String code, String message) throws IOException {
        ErrorResponse body = new ErrorResponse(Instant.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.getReasonPhrase(), code, message, req.getRequestURI(), List.of());
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), body);
    }
}