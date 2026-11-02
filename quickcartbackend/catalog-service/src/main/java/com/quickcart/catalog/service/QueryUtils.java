package com.quickcart.catalog.service;

import com.quickcart.catalog.dto.response.ErrorDetail;
import com.quickcart.catalog.exception.ApiException;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

public final class QueryUtils {

    public static final int MAX_PAGE_SIZE = 50;

    private QueryUtils() { }

    public static Pageable pageable(int page, int size, Sort sort) {
        if (page < 0) {
            throw invalid("page", "Must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw invalid("size", "Must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }

    /** "%text%", lower-case, with %, _ and ! escaped using '!'. Blank input matches everything. */
    public static String likePattern(String q) {
        if (q == null || q.isBlank()) {
            return "%";
        }
        String escaped = q.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    public static boolean hasText(String q) {
        return q != null && !q.isBlank();
    }

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public static ApiException invalid(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed.",
                List.of(new ErrorDetail(field, message)));
    }
}