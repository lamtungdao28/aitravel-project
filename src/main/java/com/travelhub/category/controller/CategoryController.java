package com.travelhub.category.controller;

import com.travelhub.category.dto.*;
import com.travelhub.category.service.CategoryService;
import com.travelhub.common.response.PageResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "Categories")
public class CategoryController {
    private final CategoryService service;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryCreateRequest request) {
        var response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + response.id()))
                .body(response);
    }

    @GetMapping
    public PageResponse<CategoryResponse> list(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(
            @PathVariable @Positive Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
