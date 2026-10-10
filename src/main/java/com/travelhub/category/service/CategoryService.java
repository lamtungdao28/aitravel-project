package com.travelhub.category.service;

import com.travelhub.category.dto.*;
import com.travelhub.category.entity.Category;
import com.travelhub.category.mapper.CategoryMapper;
import com.travelhub.category.repository.CategoryRepository;
import com.travelhub.common.exception.*;
import com.travelhub.common.response.PageResponse;
import com.travelhub.common.util.Pages;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    public CategoryResponse get(Long id) {
        return mapper.toResponse(require(id));
    }

    public PageResponse<CategoryResponse> list(Pageable pageable) {
        return PageResponse.from(
                repository
                        .findAll(Pages.checked(pageable, "id", "name", "slug"))
                        .map(mapper::toResponse));
    }

    @Transactional
    @CacheEvict(
            cacheNames = {"placeDetails"},
            allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse create(CategoryCreateRequest request) {
        Category category = new Category();
        apply(category, request.name(), request.slug(), request.description(), request.imageUrl());
        return mapper.toResponse(repository.saveAndFlush(category));
    }

    @Transactional
    @CacheEvict(
            cacheNames = {"placeDetails"},
            allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(Long id, CategoryUpdateRequest request) {
        Category category = require(id);
        apply(category, request.name(), request.slug(), request.description(), request.imageUrl());
        repository.flush();
        return mapper.toResponse(category);
    }

    @Transactional
    @CacheEvict(
            cacheNames = {"placeDetails"},
            allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Category category = require(id);
        if (repository.countAssignments(id) > 0)
            throw new BusinessException("Cannot delete a category assigned to places");
        repository.delete(category);
        repository.flush();
    }

    private Category require(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private void apply(
            Category category, String name, String slug, String description, String imageUrl) {
        String normalizedName = name.trim();
        String normalizedSlug = slug.toLowerCase(Locale.ROOT);
        Long excluded = category.getId() == null ? -1L : category.getId();
        if (repository.existsByNameIgnoreCaseAndIdNot(normalizedName, excluded))
            throw new DuplicateResourceException("Category name already exists");
        if (repository.existsBySlugAndIdNot(normalizedSlug, excluded))
            throw new DuplicateResourceException("Category slug already exists");
        category.setName(normalizedName);
        category.setSlug(normalizedSlug);
        category.setDescription(description);
        category.setImageUrl(imageUrl);
    }
}
