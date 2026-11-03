package com.quickcart.catalog.service;

import com.quickcart.catalog.dto.request.CategoryRequest;
import com.quickcart.catalog.dto.response.CategoryResponse;
import com.quickcart.catalog.entity.Category;
import com.quickcart.catalog.exception.ApiException;
import com.quickcart.catalog.mapper.CatalogMapper;
import com.quickcart.catalog.repository.CategoryRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private static final Sort BY_NAME = Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));

    private final CategoryRepository repository;
    private final CatalogMapper mapper;

    public CategoryService(CategoryRepository repository, CatalogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {                               // C1
        return repository.findByActiveTrue(BY_NAME).stream().map(mapper::toCategory).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {                                  // A1
        return repository.findAll(BY_NAME).stream().map(mapper::toCategory).toList();
    }

    // A2/A3 are deliberately not @Transactional: a unique-index violation must surface at
    // saveAndFlush so it can be mapped to 409, not at a later commit.
    public CategoryResponse create(CategoryRequest req) {                      // A2
        String name = req.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw exists();
        }
        Category category = new Category(name, QueryUtils.blankToNull(req.description()),
                QueryUtils.blankToNull(req.imageUrl()));
        try {
            return mapper.toCategory(repository.saveAndFlush(category));
        } catch (DataIntegrityViolationException race) {
            throw exists();
        }
    }

    public CategoryResponse update(long id, CategoryRequest req) {             // A3
        Category category = find(id);
        String name = req.name().trim();
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw exists();
        }
        category.update(name, QueryUtils.blankToNull(req.description()), QueryUtils.blankToNull(req.imageUrl()));
        try {
            return mapper.toCategory(repository.saveAndFlush(category));
        } catch (DataIntegrityViolationException race) {
            throw exists();
        }
    }

        @Transactional
    public CategoryResponse setActive(long id, boolean active) {               // A4
        Category category = find(id);
        category.setActive(active);
        return mapper.toCategory(repository.saveAndFlush(category));
    }

    private Category find(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Category not found."));
    }

    private ApiException exists() {
        return new ApiException(HttpStatus.CONFLICT, "CATEGORY_ALREADY_EXISTS",
                "A category with this name already exists.");
    }
}